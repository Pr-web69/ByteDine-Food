package com.lexiang.server.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付结果 VO（v3.0 简化，移除 payMethod）
 */
@Data
@Builder
public class PayResultVO {
    /** 商户订单号 */
    private String orderNo;
    /** 订单金额 */
    private BigDecimal amount;
    /** 支付页面 HTML（支付宝返回，前端直接渲染） */
    private String qrCode;
    /** 支付状态：WAIT_PAY / SUCCESS / FAIL */
    private String status;
    /** 提示信息 */
    private String message;
}