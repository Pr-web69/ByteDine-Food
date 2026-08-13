package com.lexiang.server.payment;

import com.lexiang.server.vo.PayResultVO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付服务接口v3.0
 */
public interface PayService {

    /**
     * 创建支付宝电脑网站支付，返回支付页面 HTML
     */
    PayResultVO createPayment(Long orderId);

    /**
     * 处理支付宝异步回调
     */
    String handleAlipayNotify(HttpServletRequest request);

    /**
     * 主动查询订单支付状态（兜底查询）
     */
    PayResultVO queryPayment(Long orderId);

    /**
     * 通过订单号主动查询支付状态（支付成功页跳转用）
     */
    PayResultVO queryPaymentByOrderNo(String orderNo);
}