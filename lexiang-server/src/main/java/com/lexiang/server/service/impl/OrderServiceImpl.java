package com.lexiang.server.service.impl;
import org.redisson.api.RLock;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.OrderSubmitDTO;
import com.lexiang.server.entity.*;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.*;
import com.lexiang.server.service.OrderDetailService;
import com.lexiang.server.service.OrderService;
import com.lexiang.server.service.OrderSubmitService;
import com.lexiang.server.service.redisService.StockService;
import com.lexiang.server.vo.OrderVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务层实现
 *
 * 核心设计：
 * 1. @Transactional 保证下单多表操作原子性，任意一步失败全部回滚
 * 2. 下单前库存预校验，库存不足直接拒绝，不生成无效订单
 * 3. 订单详情快照菜品价格和名称，商家后续改价不影响历史订单
 * 4. 雪花算法 + 日期 + 随机数 生成全局唯一订单号
 * 5. 取消订单恢复库存，保证库存数据一致性
 * 6. 用户只能操作自己的订单，商家只能操作自己店铺的订单（数据权限隔离）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // 构造器注入
    // Mapper 数据层
    private final OrdersMapper ordersMapper;
    private final ShoppingCartMapper cartMapper;
    private final DishMapper dishMapper;
    private final AddressMapper addressMapper;
    private final MerchantMapper merchantMapper;
    // 业务层Service
    private final OrderDetailService orderDetailService;
    private final OrderSubmitService orderSubmitService;
    private final StockService stockService;
    // Redisson 注入
    private final org.redisson.api.RedissonClient redissonClient;
    //幂等校验token
    private final StringRedisTemplate stringRedisTemplate;

    /* =========================================================== */

    /**
     * 从ThreadLocal取出当前登录用户ID
     * ThreadLocal是线程级别的变量，每个请求一个线程，互不干扰
     */
    private Long getUserId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) {
            throw new BusinessException(401, "请先登录");
        }
        return claims.get("userId", Long.class);
    }

    /**
     * 从ThreadLocal取出当前登录商家ID
     * 和 getUserId 本质一样，语义区分，代码更可读
     */
    private Long getMerchantId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) {
            throw new BusinessException(401, "请先登录");
        }
        return claims.get("userId", Long.class);
    }

    /**
     * 将数字状态转为中文
     * 0待支付 1待接单 2待配送 3已完成 4已取消
     */
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

    /**
     * 构建 OrderVO
     * @param order   订单主记录
     * @param details 订单详情列表（列表查询时可传null，详情查询时传完整列表）
     */
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
        // 收货信息快照
        vo.setConsignee(order.getConsignee());
        vo.setPhone(order.getPhone());
        vo.setAddress(order.getAddress());
        vo.setDetails(details);
        return vo;
    }

    /**
     * 回滚库存和销量：取消已支付订单时，把支付时加的销量减回来
     * 库存在下单时已预扣，无论取消还是超时都需要恢复
     *
     * 【业务规则】
     * - 库存恢复：使用 DishMapper.restoreStock 原子操作
     * - 销量回滚：仅已支付订单取消时才减销量（待支付订单销量未增加）
     * - 营业额通过统计查询动态计算（status=4不计入营收），无需单独回滚
     *
     * @param orderId     订单ID
     * @param wasPaid     订单是否已支付（true=减销量+恢复库存，false=仅恢复库存）
     */
    private void rollbackStockAndSales(Long orderId, boolean wasPaid) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);

        for (OrderDetail detail : details) {
            // Redis 原子恢复库存
            stockService.restore(detail.getDishId(), detail.getQuantity());

            // 同步 Redis 库存到 MySQL（最终一致性）
            stockService.syncToMySQL(detail.getDishId());

            // 仅已支付订单才需要回滚销量
            if (wasPaid) {
                Dish dish = dishMapper.selectById(detail.getDishId());
                if (dish != null) {
                    int currentSales = dish.getSales() == null ? 0 : dish.getSales();
                    dish.setSales(Math.max(0, currentSales - detail.getQuantity()));
                    dishMapper.updateById(dish);
                }
            }
        }
    }

    /* ======================业务方法实现 ===============*/

    /**
     * 用户下单
     *
     * 流程：
     * 购物车 → 库存校验 → 生成订单号 → 插入订单+明细 → 扣库存 → 删购物车 → 返回
     * 全程 @Transactional，任一环节失败全部回滚
     */
    @Override
    public OrderVO submit(OrderSubmitDTO dto, String orderToken) {
            // ① 获取当前用户ID
            Long userId = getUserId();

            // ①.① Redisson 分布式锁 — 防同一用户并发下单
            // 锁粒度：order:lock:{userId}，同一用户同一时刻只能有一个下单请求
            String lockKey = "order:lock:" + userId;
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = false;
            try {
                // tryLock：非阻塞，等待3秒获取锁，锁自动过期10秒
                // 看门狗机制：Redisson 自动续期，业务没执行完锁不会过期
                locked = lock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS);
                if (!locked) {
                    throw new BusinessException(429, "下单太频繁，请稍后再试");
                }

                return orderSubmitService.doSubmit(dto, userId, orderToken);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(500, "下单被中断");
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        }

    /**
     * 生成下单幂等 Token
     * 1. UUID 生成全局唯一 Token
     * 2. 存入 Redis，5 分钟有效，key = order:token:{token}
     * 3. 提交订单时用 Lua 脚本原子性校验+删除，防止重复提交
     */
    @Override
    public String generateOrderToken() {
        Long userId = getUserId();
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                "order:token:" + token,
                String.valueOf(userId),
                java.time.Duration.ofMinutes(5)
        );
        log.debug("生成下单Token userId={} token={}", userId, token);
        return token;
    }

    /**
     * 用户查看自己的订单列表（分页 + 可选状态筛选）
     * 不加载订单详情（列表不需要，节省数据库IO）
     */
    @Override
    public Page<OrderVO> userPage(Integer page, Integer pageSize, Integer status) {
        Long userId = getUserId();

        // MyBatis-Plus 分页查询
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);
        wrapper.eq(status != null, Orders::getStatus, status);
        wrapper.orderByDesc(Orders::getCreateTime);

        ordersMapper.selectPage(pageInfo, wrapper);

        // 转换为 Page<OrderVO>，保留分页元数据
        Page<OrderVO> voPage = new Page<>(page, pageSize, pageInfo.getTotal());
        List<OrderVO> voList = new ArrayList<>();
        for (Orders order : pageInfo.getRecords()) {
            voList.add(buildOrderVO(order, null)); // 列表不加载详情
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 用户查看单个订单完整详情
     * 包含订单信息和所有订单明细（菜品快照）
     */
    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Long userId = getUserId();

        // 查订单主记录
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限查看该订单");
        }

        // 查订单明细
        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(detailWrapper);

        return buildOrderVO(order, details);
    }

    /**
     * 用户取消订单（仅限待支付状态）
     *
     * 【业务规则】
     * - 仅允许取消 status=0（待支付）的订单，支付后用户不能自行取消
     * - 恢复预扣库存，不减销量（支付时才加销量）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        Long userId = getUserId();

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限取消该订单");
        }
        // 【业务规则】用户只能取消待支付订单，已支付需联系商家拒单
        if (order.getStatus() != 0) {
            throw new BusinessException(400, "当前订单状态不允许取消，已支付订单请联系商家拒单");
        }

        order.setStatus(4); // 已取消
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        ordersMapper.updateById(order);

        // 恢复预扣库存（下单时已扣），不减销量（未支付）
        rollbackStockAndSales(orderId, false);
        log.info("用户 {} 取消订单 {}（待支付），已恢复库存，原因：{}", userId, order.getOrderNo(), reason);
    }

    /**
     * 用户确认收货
     * 条件：订单状态为 2（配送中）
     * 操作：改状态为3（已完成）→记录完成时间
     */
    @Override
    public void confirm(Long orderId) {
        Long userId = getUserId();

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作该订单");
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(400, "当前订单状态不允许确认收货，请等待商家配送");
        }

        order.setStatus(3); // 已完成
        order.setFinishTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        log.info("用户 {} 确认收货，订单 {}", userId, order.getOrderNo());
    }

    /**
     * 商家查看自己店铺的订单列表（分页 + 可选状态筛选 + 订单号搜索）
     */
    @Override
    public Page<OrderVO> merchantPage(Integer page, Integer pageSize,
                                      Integer status, String keyword) {
        Long merchantId = getMerchantId();

        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getMerchantId, merchantId);
        wrapper.eq(status != null, Orders::getStatus, status);
        // 按订单号模糊搜索
        wrapper.like(keyword != null && !keyword.isEmpty(),
                Orders::getOrderNo, keyword);
        wrapper.orderByDesc(Orders::getCreateTime);

        ordersMapper.selectPage(pageInfo, wrapper);

        Page<OrderVO> voPage = new Page<>(page, pageSize, pageInfo.getTotal());
        List<OrderVO> voList = new ArrayList<>();
        for (Orders order : pageInfo.getRecords()) {
            voList.add(buildOrderVO(order, null));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 商家查看单个订单详情（含订单明细）
     */
    @Override
    public OrderVO getMerchantOrderDetail(Long orderId) {
        Long merchantId = getMerchantId();

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权限查看该订单");
        }

        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(detailWrapper);

        return buildOrderVO(order, details);
    }

    /**
     * 商家拒单（可拒待支付和待接单）
     *
     * 【业务规则】
     * - 待支付(0)拒单：恢复预扣库存，不减销量
     * - 待接单(1)拒单（已支付）：完整回滚 — 恢复库存 + 减销量 + 营业额自动排除（status=4不计入统计）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantCancel(Long orderId, String reason) {
        Long merchantId = getMerchantId();

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权限操作该订单");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new BusinessException(400, "当前订单状态不允许拒单");
        }

        int oldStatus = order.getStatus();
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        ordersMapper.updateById(order);

        // 【业务规则】
        // - 待支付(0)拒单：恢复库存（下单时已预扣），不减销量（支付时才加）
        // - 待接单(1)拒单：恢复库存 + 减销量（支付时已加），营业额自动排除
        boolean wasPaid = (oldStatus == 1);
        rollbackStockAndSales(orderId, wasPaid);
        log.info("商家 {} 拒单 {}（原状态:{}），原因：{}，回滚库存{}销量",
                merchantId, order.getOrderNo(), oldStatus, reason, wasPaid ? "+" : "");
    }

    /**
     * 商家接单（status 1→2 待配送）
     *
     * 【业务规则】接单不修改库存、销量、营业额（这些都在支付时已完成）
     * 仅修改订单状态为待配送
     */
    @Override
    public void accept(Long orderId) {
        Long merchantId = getMerchantId();

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权限操作该订单");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(400, "当前订单状态不允许接单，仅待接单状态可接单");
        }

        order.setStatus(2); // 待配送
        ordersMapper.updateById(order);
        log.info("商家 {} 接单，订单 {} → 待配送", merchantId, order.getOrderNo());
    }

    /**
     * 商家完成订单（status 2→3 已完成）
     * 毕设简化：骑手配送完成后商家点击完成
     */
    @Override
    public void complete(Long orderId) {
        Long merchantId = getMerchantId();

        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        if (!order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(403, "无权限操作该订单");
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(400, "当前订单状态不允许完成，仅待配送状态可完成");
        }

        order.setStatus(3); // 已完成
        order.setFinishTime(LocalDateTime.now());
        ordersMapper.updateById(order);
        log.info("商家 {} 完成订单 {}", merchantId, order.getOrderNo());
    }

    /**
     * 系统定时关单 — 30分钟未支付自动取消
     * 【业务规则】
     * - 查询 create_time < 30分钟前 + status=0 的订单
     * - 恢复 Redis 预扣库存（已支付订单不会被取消）
     * - 批量改状态为 4 + 记录取消原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrders() {
        // 查出所有超过30分钟仍待支付的订单
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, 0)
                .lt(Orders::getCreateTime, deadline);
        List<Orders> expiredOrders = ordersMapper.selectList(wrapper);

        if (expiredOrders.isEmpty()) return;

        for (Orders order : expiredOrders) {
            // 恢复 Redis 预扣库存
            rollbackStockAndSales(order.getId(), false);

            // 更新订单状态
            order.setStatus(4);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("超时未支付，系统自动取消");
            ordersMapper.updateById(order);

            log.info("超时关单 orderNo={} userId={}", order.getOrderNo(), order.getUserId());
        }
        log.info("定时关单完成，共取消 {} 笔订单", expiredOrders.size());
    }
}