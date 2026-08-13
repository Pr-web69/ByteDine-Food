package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 * 前端提交的用户/商家登录表单数据
 *
 * @NotBlank 校验字符串不能为null且不能为空串，校验失败时由GlobalExceptionHandler统一处理
 */
@Data
public class LoginDTO {

    /** 手机号或用户名，不能为空 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
