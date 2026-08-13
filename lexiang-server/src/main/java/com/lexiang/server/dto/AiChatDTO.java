package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 聊天请求 DTO
 */
@Data
public class AiChatDTO {

    @NotBlank(message = "问题不能为空哦！")
    private String question;
}