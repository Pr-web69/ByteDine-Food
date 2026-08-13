package com.lexiang.server.service.ai;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * AI 底层通用服务接口
 * 封装 Spring AI 调用，统一异常处理、降级兜底
 * 所有上层 AI 业务方法都通过此接口调用大模型
 */
public interface AiBaseService {

    /** 通用对话（默认参数） */
    String chat(String systemPrompt, String userPrompt);

    /** 通用对话（自定义参数：temperature、topP 等） */
    String chat(String systemPrompt, String userPrompt, ChatOptions options);

    /** 无 System Prompt 的简单对话 */
    String chat(String prompt);
}