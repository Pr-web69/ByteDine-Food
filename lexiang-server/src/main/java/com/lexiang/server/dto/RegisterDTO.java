package com.lexiang.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册请求DTO
 * 前端提交的注册表单数据
 */
@Data
public class RegisterDTO {

    /** 手机号，登录凭据，不能为空 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 昵称，选填，不填则自动生成 */
    private String nickname;
}
