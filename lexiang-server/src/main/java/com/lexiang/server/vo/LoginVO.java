package com.lexiang.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回的VO
 * 前端拿到 token 后存入 localStorage，后续请求在请求头带上
 * type 字段区分用户端(1)和商家端(2)，前端根据类型跳转不同页面
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** JWT Token，前端请求时放在请求头 Authorization: Bearer xxx */
    private String token;

    /** 用户/商家ID */
    private Long userId;

    /** 用户/商家名称 */
    private String userName;

    /** 类型：1普通用户 2商家 */
    private Integer type;
}
