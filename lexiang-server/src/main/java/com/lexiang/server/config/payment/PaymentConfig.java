package com.lexiang.server.config.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱支付配置（v4.0）
 * <p>
 * 配置层级：
 * payment.gateway      — 沙箱网关（全站唯一）
 * payment.notify-base  — 回调公网地址（natapp 内网穿透）
 * payment.return-url   — 支付完成前端跳转地址
 * payment.alipay.*     — 支付宝业务参数（APPID/密钥等）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    /** 沙箱网关地址（全站唯一，与正式环境隔离） */
    private String gateway;

    /** 回调通知公网地址（natapp 内网穿透） */
    private String notifyBase;

    /** 支付完成后前端跳转地址 */
    private String returnUrl;

    /** 支付宝业务参数 */
    private AlipayConfig alipay = new AlipayConfig();

    /**
     * 获取有效的网关地址（优先使用 payment.gateway，回退到 payment.alipay.gateway 兼容旧配置）
     */
    public String getEffectiveGateway() {
        if (gateway != null && !gateway.isBlank()) {
            return gateway;
        }
        return alipay.getGateway();
    }

    @Data
    public static class AlipayConfig {
        /** 沙箱 APPID */
        private String appId;
        /** 商户 PID（seller_id） */
        private String sellerId;
        /** 开发者应用私钥（PKCS#8 PEM 格式，RSA2 签名用） */
        private String privateKey;
        /** 支付宝公钥（PEM 格式，回调验签用） */
        private String alipayPublicKey;
        /** 沙箱网关地址（兼容旧配置，建议使用 payment.gateway） */
        private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
        /** 签名算法 */
        private String signType = "RSA2";
    }
}
