package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.OrderSubmitDTO;
import com.lexiang.server.entity.*;
import com.lexiang.server.mapper.*;
import com.lexiang.server.service.OrderDetailService;
import com.lexiang.server.service.OrderSubmitService;
import com.lexiang.server.service.redisService.StockService;
import com.lexiang.server.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 下单核心业务实现
 * <p>
 * 从 OrderServiceImpl 抽离到独立 Service，确保 {@code @Transactional} 正常生效：
 * - OrderServiceImpl.submit() 负责 Redisson 分布式锁 + 获取 userId
 * - OrderSubmitServiceImpl.doSubmit() 负责事务保护下的下单全流程
 * <p>
 * 下单流程（全程事务保护，任一环节失败全部回滚）：
 * ① 幂等 Token 校验（Lua 原子）② 未支付订单拦截
 * ③ 查询购物车 → ④ 校验地址 → ⑤ 校验菜品+库存 → ⑥ 构建订单明细（价格快照）
 * ⑦ 插入订单主表 → ⑧ 批量插入明细 → ⑨ Redis Lua 原子扣库存 → ⑩ 返回订单 VO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSubmitServiceImpl implements OrderSubmitService {

    private final OrdersMapper ordersMapper;
    private final ShoppingCartMapper cartMapper;
    private final DishMapper dishMapper;
    private final AddressMapper addressMapper;
    private final MerchantMapper merchantMapper;
    private final OrderDetailService orderDetailService;
    private final StockService stockService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成订单号
     * 格式：LX 20260716 15230 07
     *      ↑   日期    毫秒后四位  两位随机数
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        String random = String.format("%02d", ThreadLocalRandom.current().nextInt(100));
        return "LX" + date + suffix + random;
    }

    private String getStatusText(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "待接单";
            case 2: return "待配送";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private OrderVO buildOrderVO(Orders order, List<OrderDetail> details) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setPayMethod(order.getPayMethod());
        vo.setRemark(order.getRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setConsignee(order.getConsignee());
        vo.setPhone(order.getPhone());
        vo.setAddress(order.getAddress());
        vo.setDetails(details);
        return vo;
    }

    /**
     * 下单核心逻辑（事务保护）
     * <p>
     * {@code @Transactional(rollbackFor = Exception.class)} 现在由 Spring AOP 拦截，
     * 因为本方法在独立 Service 中，调用方通过注入代理调用。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO doSubmit(OrderSubmitDTO dto, Long userId, String orderToken) {
        // ====== ① 幂等 Token 校验（Lua 原子：查询+删除一步完成） ======
        if (orderToken != null && !orderToken.isEmpty()) {
            String luaScript = "if redis.call('GET', KEYS[1]) then return redis.call('DEL', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
            Long result = stringRedisTemplate.execute(
                    script,
                    java.util.Collections.singletonList("order:token:" + orderToken)
            );
            if (result == null || result == 0) {
                throw new BusinessException(400, "请勿重复提交订单");
            }
        }

        // ====== ② 未支付订单拦截 ======
        int unpaidCount = ordersMapper.countUnpaidByUserId(userId);
        if (unpaidCount > 0) {
            log.warn("[下单拦截] userId={} 存在未支付订单({}条)，拒绝新建订单", userId, unpaidCount);
            throw new BusinessException(400, "你尚有未支付订单，请先处理后再下单");
        }

        // ====== ③ 查询当前用户的购物车 ======
        LambdaQueryWrapper<ShoppingCart> cartWrapper = new LambdaQueryWrapper<>();
        cartWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> cartList = cartMapper.selectList(cartWrapper);
        if (cartList == null || cartList.isEmpty()) {
            throw new BusinessException(400, "购物车为空，请先添加菜品");
        }

        // ====== ④ 校验收货地址 ======
        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException(400, "收货地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限使用该地址");
        }

        // ====== ⑤ 遍历购物车：校验菜品 + 快照信息 + 计算总价 ======
        Long merchantId = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();

        for (ShoppingCart cart : cartList) {
            Dish dish = dishMapper.selectById(cart.getDishId());
            if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) {
                String name = dish != null ? dish.getName() : String.valueOf(cart.getDishId());
                throw new BusinessException(400, "菜品【" + name + "】已下架，请从购物车移除后再下单");
            }

            // 库存预校验
            if (dish.getStock() == null || dish.getStock() < cart.getQuantity()) {
                throw new BusinessException(400,
                        "菜品【" + dish.getName() + "】库存不足，当前库存：" + dish.getStock());
            }

            // 跨商家检查
            if (merchantId == null) {
                merchantId = dish.getMerchantId();
            } else if (!merchantId.equals(dish.getMerchantId())) {
                throw new BusinessException(400, "购物车中含有不同商家的菜品，请分别下单");
            }

            // 价格快照
            BigDecimal price = cart.getPrice() != null ? cart.getPrice() : dish.getPrice();
            if (price == null) {
                throw new BusinessException(400, "菜品【" + dish.getName() + "】价格异常，请联系商家");
            }
            BigDecimal itemAmount = price.multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);

            OrderDetail detail = new OrderDetail();
            detail.setDishId(dish.getId());
            detail.setDishName(dish.getName());
            detail.setDishImage(dish.getImage());
            detail.setPrice(price);
            detail.setQuantity(cart.getQuantity());
            detail.setAmount(itemAmount);
            detail.setSpecInfo(cart.getSpecInfo());
            details.add(detail);
        }

        // ====== ⑥ 校验商家营业状态 ======
        if (merchantId != null) {
            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant != null && merchant.getBusinessStatus() != null && merchant.getBusinessStatus() == 0) {
                throw new BusinessException(400, "商家已打烊，明日再来吧~");
            }
        }

        // ====== ⑦ 插入订单主表 ======
        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setAddressId(dto.getAddressId());
        order.setTotalAmount(totalAmount);
        order.setStatus(0);
        order.setPayMethod("SIMULATE");
        order.setRemark(dto.getRemark());
        order.setConsignee(address.getContactName());
        order.setPhone(address.getContactPhone());
        order.setAddress(address.getAddressDetail());
        ordersMapper.insert(order);

        // ====== ⑧ 批量插入订单明细 ======
        for (OrderDetail detail : details) {
            detail.setOrderId(order.getId());
        }
        orderDetailService.saveBatch(details);

        // ====== ⑨ Redis Lua 原子扣库存 ======
        for (OrderDetail detail : details) {
            boolean success = stockService.deduct(detail.getDishId(), detail.getQuantity());
            if (!success) {
                throw new BusinessException(400,
                        "菜品【" + detail.getDishName() + "】库存不足，下单失败");
            }
        }

        // ====== ⑩ 清空购物车 + 返回 ======
        cartMapper.delete(cartWrapper);
        log.info("用户 {} 下单成功，订单号：{}，金额：{}，待支付（已预扣库存）", userId, order.getOrderNo(), totalAmount);
        return buildOrderVO(order, details);
    }
}
