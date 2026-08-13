package com.lexiang.server.task;

import com.lexiang.server.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时关单定时任务
 * 1. @Scheduled(fixedRate = 60000) 每分钟扫描一次过期订单
 * 2. 事务保护：单笔订单取消失败不影响其他订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    /**
     * 每 60 秒扫描一次，取消超过 30 分钟未支付的订单
     * fixedRate：从上次任务开始算间隔，不管上次是否执行完
     * 改成 fixedDelay 更安全（等上次执行完再算间隔）
     */
    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredOrders() {
        try {
            orderService.cancelTimeoutOrders();
        } catch (Exception e) {
            // 定时任务不能抛异常，否则调度器会停止
            log.error("超时关单任务执行失败", e);
        }
    }
}