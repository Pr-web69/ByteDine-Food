package com.lexiang.server.service.ai.impl;

import com.lexiang.server.service.ai.AiBaseService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 底层通用服务实现
 *
 * 核心职责：
 * 1. 封装 ChatClient 调用（切换模型只改这层）
 * 2. 统一异常处理（AI 不可用时返回降级文案）
 * 3. 避免 Controller / 上层 Service 直接依赖 Spring AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBaseServiceImpl implements AiBaseService {

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String model;

    /** 启动时校验 AI 配置，帮助快速定位问题 */
    @PostConstruct
    public void checkConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("====== AI 配置异常 ======");
            log.warn("");
            log.warn("");
        } else {
            log.info("AI 配置检查通过 → base-url: {}, model: {}, apiKey: {}...{}",
                    baseUrl, model,
                    apiKey.substring(0, Math.min(8, apiKey.length())),
                    apiKey.length() > 8 ? apiKey.substring(apiKey.length() - 4) : "");
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            String result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            if (result == null || result.isBlank()) {
                log.warn("AI 返回内容为空，systemPrompt长度={}, userPrompt长度={}",
                        systemPrompt.length(), userPrompt.length());
                return "AI 暂时无法生成回复，请换个问题试试。";
            }
            return result;
        } catch (Exception e) {
            log.error("AI 调用失败 | 异常类型: {} | 原因: {}", e.getClass().getSimpleName(), e.getMessage());
            log.debug("AI 调用详细异常: ", e);
            return buildFallbackMessage(e);
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, ChatOptions options) {
        try {
            String result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(options)
                    .call()
                    .content();
            if (result == null || result.isBlank()) {
                log.warn("AI 返回内容为空（自定义参数模式）");
                return "AI 暂时无法生成回复，请换个问题试试。";
            }
            return result;
        } catch (Exception e) {
            log.error("AI 调用失败（自定义参数） | 异常类型: {} | 原因: {}", e.getClass().getSimpleName(), e.getMessage());
            log.debug("AI 调用详细异常: ", e);
            return buildFallbackMessage(e);
        }
    }

    @Override
    public String chat(String prompt) {
        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (result == null || result.isBlank()) {
                log.warn("AI 返回内容为空（简单对话模式）");
                return "AI 暂时无法生成回复，请换个问题试试。";
            }
            return result;
        } catch (Exception e) {
            log.error("AI 调用失败（简单对话） | 异常类型: {} | 原因: {}", e.getClass().getSimpleName(), e.getMessage());
            log.debug("AI 调用详细异常: ", e);
            return buildFallbackMessage(e);
        }
    }

    /**
     * 根据异常类型构建更精准的兜底提示
     */
    private String buildFallbackMessage(Exception e) {
        String msg = e.getMessage();
        if (msg != null) {
            // API Key 无效
            if (msg.contains("401") || msg.contains("Unauthorized") || msg.contains("invalid api-key")) {
                log.error(">>> AI API Key 无效，请检查 spring.ai.openai.api-key 配置");
                return "AI 服务认证失败，请检查 API Key 配置。";
            }
            // 模型不存在
            if (msg.contains("404") || msg.contains("model") && msg.contains("not found")) {
                log.error(">>> AI 模型不可用，请检查 spring.ai.openai.chat.options.model 配置");
                return "AI 模型暂不可用，请联系管理员。";
            }
            // 限流
            if (msg.contains("429") || msg.contains("rate limit") || msg.contains("Too Many")) {
                return "AI 服务繁忙，请稍后再试。";
            }
            // 超时
            if (e instanceof java.util.concurrent.TimeoutException
                    || msg.contains("timeout") || msg.contains("Timeout")) {
                return "AI 响应超时，请稍后重试。";
            }
            // 连接失败
            if (msg.contains("Connection refused") || msg.contains("connect timed out")
                    || msg.contains("Connection reset")) {
                log.error(">>> AI 服务网络连接失败，请检查 base-url 配置和网络连通性");
                return "AI 服务网络异常，请检查网络连接。";
            }
        }
        return "AI 服务暂时不可用，请稍后重试。";
    }
}
