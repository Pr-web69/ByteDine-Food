package com.lexiang.server.controller.user;

import com.lexiang.common.result.Result;
import com.lexiang.server.dto.AiChatDTO;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.service.ai.AiBaseService;
import com.lexiang.server.service.ai.UserAiService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户端 - AI 智能助手
 * /api/user/ai
 */
@RestController("userAiController")
@RequestMapping("/api/user/ai")
@RequiredArgsConstructor
public class AiController {

    private final UserAiService userAiService;
    private final AiBaseService aiBaseService;

    @Value("${spring.ai.openai.base-url:未配置}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:未配置}")
    private String model;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * AI 服务健康检查
     * GET /api/user/ai/health
     * 返回 AI 配置状态，无需登录
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("baseUrl", baseUrl);
        info.put("model", model);
        info.put("apiKeyConfigured", apiKey != null && !apiKey.isBlank());
        info.put("apiKeyPrefix", apiKey != null && apiKey.length() >= 8
                ? apiKey.substring(0, 8) + "..." : "未配置");

        // 尝试一次简单调用检测连通性
        try {
            String testReply = aiBaseService.chat("请回复'OK'");
            info.put("connected", testReply != null && !testReply.contains("不可用")
                    && !testReply.contains("认证失败") && !testReply.contains("网络异常"));
            info.put("testReply", testReply);
        } catch (Exception e) {
            info.put("connected", false);
            info.put("error", e.getMessage());
        }

        return Result.success(info);
    }

    /**
     * 智能点餐助手
     */
    @PostMapping("/suggest")
    public Result<String> suggest(@Valid @RequestBody AiChatDTO dto) {
        Long userId = getUserId();
        return Result.success(userAiService.suggestDish(userId, dto.getQuestion()));
    }

    /**
     * 订单智能客服
     */
    @PostMapping("/order-consult")
    public Result<String> orderConsult(@RequestBody Map<String, String> body) {
        Long userId = getUserId();
        Long orderId = Long.valueOf(body.get("orderId"));
        return Result.success(userAiService.orderConsult(userId, orderId, body.get("question")));
    }

    private Long getUserId() {
        Claims claims = JwtInterceptor.USER_HOLDER.get();
        if (claims == null) return null;
        return claims.get("userId", Long.class);
    }
}