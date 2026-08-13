package com.lexiang.server.util;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.config.payment.PaymentConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝沙箱支付工具类
 * 提供三个核心能力：
 * ① 创建电脑网站支付 → 返回支付页面 HTML（手动 RSA2 签名，绕过 SDK 的 JAR 冲突）
 * ② 异步回调验签       → 使用 SDK 的 AlipaySignature.rsaCheckV1
 * ③ 主动查询订单状态   → 手动 RSA2 签名 + HTTP POST → 解析 JSON 响应
 * 
 * 设计说明：Alipay SDK *-ALL.jar 内置的加密库与 JDK 内置 JCE 存在冲突，
 * 导致 SDK 的 DefaultSigner 始终报 "Unable to decode key"。
 * 但 SDK 的验签工具 (AlipaySignature) 正常工作，因此：
 * - 签名（下单、查询）用 Java 原生 Signature API
 * - 验签（回调通知）仍使用 SDK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayUtil {

    private final PaymentConfig paymentConfig;

    // ==================== ① 创建支付订单（手动 RSA2 签名） ====================

    public String createPagePay(String orderNo, String totalAmount, String subject) {
        PaymentConfig.AlipayConfig cfg = paymentConfig.getAlipay();

        String pk = cfg.getPrivateKey() != null ? cfg.getPrivateKey().trim() : null;
        String effectiveGateway = paymentConfig.getEffectiveGateway();

        // 配置校验
        if (cfg.getAppId() == null || cfg.getAppId().isEmpty())
            throw new BusinessException(500, "支付宝 APPID 未配置");
        if (pk == null || pk.isEmpty())
            throw new BusinessException(500, "支付宝应用私钥未配置");
        if (paymentConfig.getNotifyBase() == null || paymentConfig.getNotifyBase().contains("your-domain"))
            throw new BusinessException(500, "支付回调地址未配置");

        // 密钥预解码验证
        PrivateKey privateKey = loadPrivateKey(pk);

        log.info("=== 支付宝预下单开始 ===");
        log.info("[参数] orderNo={}, amount={}, subject={}", orderNo, totalAmount, subject);
        log.info("[配置] gateway={}, appId={}, signType={}", effectiveGateway, cfg.getAppId(), cfg.getSignType());
        log.info("[配置] notifyUrl={}, returnUrl={}", buildNotifyUrl(), paymentConfig.getReturnUrl());

        try {
            long t0 = System.currentTimeMillis();

            // 1. 有序参数（TreeMap 字典序）
            Map<String, String> params = new TreeMap<>();
            params.put("app_id", cfg.getAppId());
            params.put("method", "alipay.trade.page.pay");
            params.put("format", "json");
            params.put("charset", "utf-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            params.put("notify_url", buildNotifyUrl());
            params.put("return_url", paymentConfig.getReturnUrl());
            // 注意：不要往 biz_content 塞 seller_id，page.pay 无此字段，传了会被支付宝拒绝（报“系统有点儿忙”）
            params.put("biz_content",
                    "{\"out_trade_no\":\"" + orderNo + "\"" +
                    ",\"total_amount\":\"" + totalAmount + "\"" +
                    ",\"subject\":\"" + subject + "\"" +
                    ",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                    ",\"timeout_express\":\"30m\"}");

            // 2. 拼接待签名字符串 — value 不编码（与 AlipaySignature.getSignCheckContentV1 一致）
            StringBuilder signContent = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (signContent.length() > 0) signContent.append("&");
                signContent.append(e.getKey()).append("=").append(e.getValue());
            }
            log.info("[手动签名] 待签名字符串前200字符: {}",
                    signContent.length() > 200 ? signContent.substring(0, 200) + "..." : signContent.toString());

            // 3. SHA256withRSA 签名 + Base64
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(signContent.toString().getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(sig.sign());
            log.info("[手动签名] RSA2签名完成, 签名Base64长度={}", sign.length());

            // 4. 构建完整 HTML 文档（显式声明 UTF-8，确保浏览器提交中文不丢编码）
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<meta charset=\"UTF-8\">\n")
                .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n")
                .append("<title>跳转到支付宝支付</title>\n</head>\n<body>\n");
            html.append("<form id='alipaysubmit' name='alipaysubmit' action='").append(effectiveGateway)
                .append("?charset=utf-8' method='POST' accept-charset='UTF-8'>\n");
            for (Map.Entry<String, String> e : params.entrySet()) {
                html.append("<input type='hidden' name='").append(e.getKey())
                    .append("' value='").append(htmlEscape(e.getValue())).append("'/>\n");
            }
            html.append("<input type='hidden' name='sign' value='").append(htmlEscape(sign)).append("'/>\n");
            html.append("</form>\n<script>document.getElementById('alipaysubmit').submit();</script>\n");
            html.append("</body>\n</html>");

            long elapsed = System.currentTimeMillis() - t0;
            log.info("[结果] 支付跳转HTML构建成功, 耗时={}ms, HTML长度={}", elapsed, html.length());
            return html.toString();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[异常] 手动签名失败: orderNo={}, msg={}", orderNo, e.getMessage(), e);
            throw new BusinessException(500, "支付宝支付异常: " + e.getMessage());
        }
    }

    // ==================== ② 异步回调验签（使用 SDK） ====================

    public boolean verifyNotifySignature(HttpServletRequest request) {
        PaymentConfig.AlipayConfig cfg = paymentConfig.getAlipay();

        Map<String, String> params = new TreeMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String n = names.nextElement();
            params.put(n, request.getParameter(n));
        }

        log.info("=== 支付宝异步回调验签 ===");
        log.info("[回调] notify_id={}, trade_no={}, out_trade_no={}, trade_status={}",
                params.get("notify_id"), params.get("trade_no"),
                params.get("out_trade_no"), params.get("trade_status"));

        try {
            boolean valid = AlipaySignature.rsaCheckV1(params, cfg.getAlipayPublicKey(), "UTF-8", cfg.getSignType());
            log.info("[验签] {}", valid ? "✅ 通过" : "❌ 失败");
            return valid;
        } catch (AlipayApiException e) {
            log.error("[验签异常] {}", e.getErrMsg(), e);
            return false;
        }
    }

    public boolean isTradeSuccess(HttpServletRequest request) {
        String s = request.getParameter("trade_status");
        return "TRADE_SUCCESS".equals(s) || "TRADE_FINISHED".equals(s);
    }

    public String getOutTradeNo(HttpServletRequest request) { return request.getParameter("out_trade_no"); }
    public String getTradeNo(HttpServletRequest request) { return request.getParameter("trade_no"); }
    public String getBuyerId(HttpServletRequest request) { return request.getParameter("buyer_id"); }

    // ==================== ③ 订单查询（手动 RSA2 签名 + HTTP POST） ====================

    public String queryOrder(String orderNo) {
        PaymentConfig.AlipayConfig cfg = paymentConfig.getAlipay();

        log.info("[主动查询] orderNo={}", orderNo);

        String pk = cfg.getPrivateKey() != null ? cfg.getPrivateKey().trim() : null;
        PrivateKey privateKey = loadPrivateKey(pk);
        String gateway = paymentConfig.getEffectiveGateway();

        try {
            // 1. 有序参数
            Map<String, String> params = new TreeMap<>();
            params.put("app_id", cfg.getAppId());
            params.put("method", "alipay.trade.query");
            params.put("format", "json");
            params.put("charset", "utf-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            params.put("biz_content", "{\"out_trade_no\":\"" + orderNo + "\"}");

            // 2. 签名字符串 — value 不编码
            StringBuilder signContent = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (signContent.length() > 0) signContent.append("&");
                signContent.append(e.getKey()).append("=").append(e.getValue());
            }

            // 3. RSA2 签名
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(signContent.toString().getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(sig.sign());

            // 4. 拼接 POST body（value URL 编码用于 HTTP 传输）
            StringBuilder postBody = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (postBody.length() > 0) postBody.append("&");
                postBody.append(e.getKey()).append("=").append(urlEncode(e.getValue()));
            }
            postBody.append("&sign=").append(urlEncode(sign));

            // 5. HTTP POST（绕过 JVM 代理，避免本地代理干扰支付宝沙箱连接）
            byte[] bodyBytes = postBody.toString().getBytes(StandardCharsets.UTF_8);
            java.net.URI uri = java.net.URI.create(gateway);
            java.net.URL url = uri.toURL();
            // 直连支付宝沙箱，绕过系统代理（代理可能导致连接超时）
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.getOutputStream().write(bodyBytes);
            conn.getOutputStream().close();

            // 6. 读取响应
            int status = conn.getResponseCode();
            String respBody = new String(
                    (status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream()).readAllBytes(),
                    StandardCharsets.UTF_8);

            // 7. 解析 JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(respBody);
            com.fasterxml.jackson.databind.JsonNode resp = root.path("alipay_trade_query_response");
            String code = resp.path("code").asText();
            String tradeStatus = resp.path("trade_status").asText();
            String tradeNo = resp.path("trade_no").asText();
            String buyerId = resp.path("buyer_logon_id").asText();
            String amount = resp.path("total_amount").asText();

            log.info("[查询结果] orderNo={}, code={}, tradeStatus={}, tradeNo={}, buyerId={}, amount={}",
                    orderNo, code, tradeStatus, tradeNo, buyerId, amount);

            if ("10000".equals(code)) return tradeStatus;

            String subCode = resp.path("sub_code").asText();
            String subMsg = resp.path("sub_msg").asText();
            // ACQ.TRADE_NOT_EXIST: page pay 交易在用户到达支付宝页面之前不存在 → 正常，INFO 即可
            if ("ACQ.TRADE_NOT_EXIST".equals(subCode)) {
                log.info("[查询结果] orderNo={}, 交易尚未创建（用户未到达收银台），继续轮询", orderNo);
            } else {
                log.warn("[查询失败] orderNo={}, subCode={}, subMsg={}", orderNo, subCode, subMsg);
            }
            return null;

        } catch (Exception e) {
            log.error("[查询异常] orderNo={}, msg={}", orderNo, e.getMessage(), e);
            return null;
        }
    }

    // ==================== ④ 工具方法 ====================

    private String buildNotifyUrl() {
        String base = paymentConfig.getNotifyBase();
        if (base == null) return null;
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/api/pay/notify/alipay";
    }

    private PrivateKey loadPrivateKey(String pemKey) {
        try {
            String b64 = pemKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            if (b64.isEmpty()) throw new BusinessException(500, "私钥内容为空");

            byte[] bytes = Base64.getDecoder().decode(b64);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(bytes));
            log.info("[密钥验证] ✅ 私钥解码成功: algorithm={}, format={}", key.getAlgorithm(), key.getFormat());
            return key;
        } catch (BusinessException e) {
            throw e;
        } catch (java.security.spec.InvalidKeySpecException e) {
            log.error("[密钥验证] ❌ PKCS#8解析失败: {}", e.getMessage());
            throw new BusinessException(500, "私钥格式不兼容，请用 openssl pkcs8 -topk8 转换");
        } catch (IllegalArgumentException e) {
            log.error("[密钥验证] ❌ base64解码失败: {}", e.getMessage());
            throw new BusinessException(500, "私钥含非法字符，请从支付宝沙箱重新复制");
        } catch (Exception e) {
            log.error("[密钥验证] ❌ 异常: {}", e.getMessage(), e);
            throw new BusinessException(500, "私钥格式异常: " + e.getMessage());
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }

    private String htmlEscape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                    .replace("<", "&lt;").replace(">", "&gt;");
    }
}
