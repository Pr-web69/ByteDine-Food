package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.dto.PayOrderDTO;
import com.lexiang.server.payment.PayService;
import com.lexiang.server.vo.PayResultVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器（v3.0 仅支付宝沙箱）
 * <p>
 * 三个对外接口：
 * ① POST /api/user/pay/create → 创建支付宝支付
 * ② GET  /api/user/pay/query  → 轮询支付状态
 * ③ POST /api/pay/notify/alipay → 支付宝异步回调（公开，无需认证）
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 创建支付（生成支付宝收银台页面 HTML）
     * POST /api/user/pay/create
     */
    @PostMapping("/user/pay/create")
    public Result<PayResultVO> create(@RequestBody PayOrderDTO dto) {
        log.info("[接口] POST /api/user/pay/create, orderId={}", dto.getOrderId());
        PayResultVO result = payService.createPayment(dto.getOrderId());
        log.info("[接口] 创建支付完成: orderNo={}, status={}", result.getOrderNo(), result.getStatus());
        return Result.success(result);
    }

    /**
     * 查询订单支付状态（兜底轮询）
     * GET /api/user/pay/query?orderId=xxx
     */
    @GetMapping("/user/pay/query")
    public Result<PayResultVO> query(@RequestParam Long orderId) {
        log.debug("[接口] GET /api/user/pay/query, orderId={}", orderId);
        PayResultVO result = payService.queryPayment(orderId);
        return Result.success(result);
    }

    /**
     * 查询订单支付状态（通过订单号，用户支付完成页跳转后使用）
     * GET /api/user/pay/queryNo?orderNo=xxx
     */
    @GetMapping("/user/pay/queryNo")
    public Result<PayResultVO> queryByOrderNo(@RequestParam String orderNo) {
        log.debug("[接口] GET /api/user/pay/queryNo, orderNo={}", orderNo);

        // 通过 orderNo 找到 orderId，再复用 queryPayment（含补救确认逻辑）
        PayResultVO result = payService.queryPaymentByOrderNo(orderNo);
        return Result.success(result);
    }

    /**
     * 支付宝异步通知回调（无需认证，WebConfig 已放行）
     * POST /api/pay/notify/alipay
     * <p>
     * 支付宝服务器 POST 到此地址通知支付结果。
     * 返回 "success" 表示商户已收到通知，支付宝停止25小时内重复推送。
     * 返回 "failure" 表示处理失败，支付宝会在 4m/10m/30m/1h/2h/6h/15h 递增间隔重试。
     */
    @PostMapping("/pay/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        log.info("=== [接口] POST /api/pay/notify/alipay ===");
        String result = payService.handleAlipayNotify(request);
        log.info("[接口] 回调处理结果: {}", result);
        return result;
    }
}
