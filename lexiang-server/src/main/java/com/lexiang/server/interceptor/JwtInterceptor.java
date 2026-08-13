package com.lexiang.server.interceptor;

import com.lexiang.server.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 * 除了登录/注册接口，其他所有请求必须携带有效Token
 * 校验通过后将用户信息存入 ThreadLocal（当前线程可见的变量）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /** ThreadLocal：存储当前请求的用户信息，请求结束后必须清除，防止内存泄漏 */
    public static final ThreadLocal<Claims> USER_HOLDER = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取 Token（格式：Bearer xxx）
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;  // 拦截，不继续执行Controller
        }

        // 2. 去掉 "Bearer " 前缀，只保留Token本体
        token = token.substring(7);

        // 3. 解析Token
        try {
            Claims claims = jwtUtil.parseToken(token);
            USER_HOLDER.set(claims);  // 存入ThreadLocal，后续Controller可取用
            return true;
        } catch (Exception e) {
            log.warn("Token校验失败: {}", e.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束，清除ThreadLocal，防止内存泄漏
        USER_HOLDER.remove();
    }
}