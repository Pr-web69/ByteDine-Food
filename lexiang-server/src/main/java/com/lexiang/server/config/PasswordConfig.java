package com.lexiang.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置
 * <p>
 * 仅提供 BCrypt 密码加密能力（用户注册/登录），不引入 Spring Security Filter Chain。
 * 鉴权由 WebConfig 中注册的 JwtInterceptor 全权接管，
 * 因此用 spring-security-crypto 替代 spring-boot-starter-security，减少攻击面。
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 加密器，cost=10（Spring Security 默认值）
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
