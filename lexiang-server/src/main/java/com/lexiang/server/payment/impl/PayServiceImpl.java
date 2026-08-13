package com.lexiang.server.payment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.entity.OrderDetail;
import com.lexiang.server.entity.Orders;
import com.lexiang.server.entity.ShoppingCart;
import com.lexiang.server.mapper.OrderDetailMapper;
import com.lexiang.server.mapper.OrdersMapper;
import com.lexiang.server.mapper.ShoppingCartMapper;
import com.lexiang.server.payment.PayService;
import com.lexiang.server.util.AlipayUtil;
import com.lexiang.server.vo.PayResultVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 支付宝沙箱支付服务实现（v3.0）
 * <p>
 * 完整的支付闭环：
 * ① 创建支付：校验订单 → 组装支付宝参数 → 调用 SDK → 返回支付 HTML
 * ② 异步回调：验签 → 校验交易状态 → 幂等更新订单 → 累加销量 → 清空购物车
 * ③ 主动查询：DB 状态判断 → 支付宝侧兜底查询 → 补救确认
 * <p>
 * 幂等性保障：
 * - confirmPayment 中 status!=0 直接跳过（防止支付宝重复推送通知）
 * - 支付宝多次 POST 同一笔订单，只有第一次会触发状态变更
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final OrdersMapper ordersMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final AlipayUtil alipayUtil;
    private final RedissonClient redissonClient;

    /**
     * 创建支付宝电脑网站支付
     * <p>
     * 流程：
     * 1. 校验订单是否存在、状态是否为待支付(0)
     * 2. 组装商品标题 + 金额格式化
     * 3. 调用 AlipayUtil 生成支付 HTML
     * 4. 返回 PayResultVO（含支付宝页面 HTML）
     */
    @Override
    public PayResultVO createPayment(Long orderId) {
        // 1. 校验订单
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            log.warn("[支付-校验] 订单不存在: orderId={}", orderId);
            throw new BusinessException(400, "订单不存在");
        }
        if (order.getStatus() != 0) {
            log.warn("[支付-校验] 订单状态不正确: orderId={}, status={}, orderNo={}",
                    orderId, order.getStatus(), order.getOrderNo());
            throw new BusinessException(400, "订单状态不正确，无法支付");
        }

        log.info("=== 创建支付宝支付 ===");
        log.info("[订单信息] orderId={}, orderNo={}, totalAmount={}, userId={}, merchantId={}",
                orderId, order.getOrderNo(), order.getTotalAmount(), order.getUserId(), order.getMerchantId());

        // 2. 组装商品标题（取第一个菜品名 + "等N件"）
        List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaUpdateWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId)
        );
        String subject;
        if (details.isEmpty()) {
            subject = "乐享美食订单";
        } else {
            subject = details.get(0).getDishName() +
                    (details.size() > 1 ? " 等" + details.size() + "件" : "");
        }
        log.info("[支付标题] subject={}, 菜品数={}", subject, details.size());

        // 3. 金额：元 → 保留两位小数（支付宝金额格式要求）
        String totalAmount = order.getTotalAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
        log.info("[支付金额] {} 元", totalAmount);

        // 4. 调用支付宝 SDK 创建支付（AlipayUtil 内部有详细日志）
        String payHtml = alipayUtil.createPagePay(order.getOrderNo(), totalAmount, subject);

        log.info("[支付-结果] 返回HTML长度={}", payHtml != null ? payHtml.length() : 0);
        return PayResultVO.builder()
                .orderNo(order.getOrderNo())
                .amount(order.getTotalAmount())
                .qrCode(payHtml)
                .status("WAIT_PAY")
                .message("请在新窗口完成支付")
                .build();
    }

    /**
     * 处理支付宝异步通知（回调）
     * <p>
     * 支付宝服务器 POST 通知到 notifyBase + /api/pay/notify/alipay
     * 处理流程：验签 → 交易状态判断 → 幂等更新 → 返回 success/failure
     * <p>
     * 幂等性：无论支付宝推送多少次，confirmPayment 内部 status!=0 检查确保只处理一次
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(HttpServletRequest request) {
        log.info("=== 收到支付宝异步回调 ===");
        log.info("[请求来源] remoteAddr={}, contentType={}",
                request.getRemoteAddr(), request.getContentType());

        // 1. RSA2 签名验证（防止伪造回调）
        if (!alipayUtil.verifyNotifySignature(request)) {
            log.warn("[回调-拒绝] 验签失败，可能为伪造请求");
            return "failure";
        }

        // 2. 检查交易状态（仅处理 TRADE_SUCCESS / TRADE_FINISHED）
        if (!alipayUtil.isTradeSuccess(request)) {
            String tradeStatus = request.getParameter("trade_status");
            log.info("[回调-跳过] 非支付成功状态: trade_status={}, out_trade_no={}",
                    tradeStatus, request.getParameter("out_trade_no"));
            // 返回 success 避免支付宝重复推送（非终态通知也需要确认接收）
            return "success";
        }

        // 3. 提取关键参数
        String outTradeNo = alipayUtil.getOutTradeNo(request);
        String tradeNo = alipayUtil.getTradeNo(request);
        String buyerId = alipayUtil.getBuyerId(request);
        String totalAmount = request.getParameter("total_amount");

        log.info("[回调-确认] outTradeNo={}, tradeNo={}, buyerId={}, totalAmount={}",
                outTradeNo, tradeNo, buyerId, totalAmount);

        // 4. 幂等确认支付
        confirmPayment(outTradeNo, tradeNo);

        log.info("[回调-完成] 返回 success → 支付宝停止重复推送");
        return "success";
    }

    /**
     * 确认支付核心逻辑（幂等 + 分布式锁）
     * <p>
     * 调用路径：
     * ① 支付宝异步回调 → handleAlipayNotify → confirmPayment
     * ② 前端轮询查询    → queryPayment        → confirmPayment（兜底）
     * ③ 手动查询        → queryPaymentByOrderNo → confirmPayment（兜底）
     * <p>
     * 幂等设计（双层防护）：
     * - Redisson 分布式锁：pay:confirm:{orderNo}，防止回调+轮询并发同时处理同一笔订单
     * - status != 0 检查：已处理的订单直接跳过，防止重复累加销量/清空购物车
     *
     * @param orderNo 商户订单号
     * @param tradeNo 支付宝交易号（轮询兜底时可能为空字符串）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(String orderNo, String tradeNo) {
        // ★ Redisson 分布式锁 — 防并发（回调 + 前端轮询同时触发）
        String lockKey = "pay:confirm:" + orderNo;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("[支付确认-并发] 订单 {} 正在处理中，跳过本次调用", orderNo);
                return;
            }

            // 1. 根据订单号查询订单
            Orders order = ordersMapper.selectOne(
                    new LambdaUpdateWrapper<Orders>().eq(Orders::getOrderNo, orderNo)
            );
            if (order == null) {
                log.error("[支付确认-异常] 订单不存在: orderNo={}", orderNo);
                return;
            }

            // 2. ★ 幂等检查：已支付(1)、已取消(4)、已接单(2)等非待支付状态，直接跳过
            if (order.getStatus() != 0) {
                log.info("[支付确认-幂等跳过] orderNo={}, 当前状态={}, 已处理过，跳过重复推送",
                        orderNo, order.getStatus());
                return;
            }

            // 3. ★ 更新订单状态：0（待支付）→ 1（已支付/待接单）
            order.setStatus(1);
            order.setPayMethod("ALIPAY");
            order.setPayTime(LocalDateTime.now());
            ordersMapper.updateById(order);
            log.info("[支付确认-状态] orderNo={} status: 0→1, payTime={}, tradeNo={}",
                    orderNo, order.getPayTime(), tradeNo);

            // 4. 累加销量（遍历订单明细，每个菜品销量 +quantity）
            List<OrderDetail> details = orderDetailMapper.selectList(
                    new LambdaUpdateWrapper<OrderDetail>().eq(OrderDetail::getOrderId, order.getId())
            );
            for (OrderDetail detail : details) {
                if (detail.getQuantity() != null && detail.getQuantity() > 0) {
                    ordersMapper.increaseSales(detail.getDishId(), detail.getQuantity());
                    log.debug("[支付确认-销量] dishId={}, dishName={}, +{}",
                            detail.getDishId(), detail.getDishName(), detail.getQuantity());
                }
            }

            // 5. 清空用户购物车
            int deleted = shoppingCartMapper.delete(
                    new LambdaQueryWrapper<ShoppingCart>()
                            .eq(ShoppingCart::getUserId, order.getUserId())
            );
            log.info("[支付确认-购物车] userId={}, 清空{}条", order.getUserId(), deleted);

            log.info("=== 支付确认完成 === orderNo={}, amount={}, 销量+{}种, 购物车已清空",
                    orderNo, order.getTotalAmount(), details.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[支付确认-中断] orderNo={} 获取锁被中断", orderNo);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * 主动查询订单支付状态（前端轮询用）
     * <p>
     * 策略优先级：
     * ① 本地 DB 已支付(1) → 直接返回 SUCCESS
     * ② 本地 DB 已取消(4) → 直接返回 FAIL
     * ③ 支付宝侧查询 → TRADE_SUCCESS → 补救确认 → SUCCESS
     * ④ 其他 → WAIT_PAY（继续等待）
     */
    @Override
    public PayResultVO queryPayment(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }

        // 订单已支付 → 直接返回
        if (order.getStatus() == 1) {
            log.debug("[轮询-已支付] orderId={}, orderNo={}", orderId, order.getOrderNo());
            return PayResultVO.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getTotalAmount())
                    .status("SUCCESS")
                    .message("支付成功")
                    .build();
        }

        // 订单已取消
        if (order.getStatus() == 4) {
            return PayResultVO.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getTotalAmount())
                    .status("FAIL")
                    .message("订单已取消")
                    .build();
        }

        // 查询支付宝侧状态（兜底：防止回调丢失）
        String tradeStatus = alipayUtil.queryOrder(order.getOrderNo());

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 支付宝侧已支付，但回调未到达 → 补救确认
            log.warn("[轮询-补救] orderNo={}, 支付宝侧已支付但回调未到达，执行补救确认", order.getOrderNo());
            confirmPayment(order.getOrderNo(), "");
            return PayResultVO.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getTotalAmount())
                    .status("SUCCESS")
                    .message("支付成功")
                    .build();
        }

        // 仍在等待支付
        return PayResultVO.builder()
                .orderNo(order.getOrderNo())
                .amount(order.getTotalAmount())
                .status("WAIT_PAY")
                .message("等待支付")
                .build();
    }

    /**
     * 主动查询订单支付状态（通过订单号）
     */
    public PayResultVO queryPaymentByOrderNo(String orderNo) {
        Orders order = ordersMapper.selectOne(
                new LambdaUpdateWrapper<Orders>().eq(Orders::getOrderNo, orderNo)
        );
        if (order == null) {
            return PayResultVO.builder().status("FAIL").message("订单不存在").build();
        }
        return queryPayment(order.getId());
    }
}
