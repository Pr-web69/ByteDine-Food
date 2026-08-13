package com.lexiang.server.config;

import com.lexiang.server.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 * 1. 注册JWT拦截器，放行登录/注册接口
 * 2. 配置跨域，允许前端本地开发时跨域访问
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册拦截器
     * addPathPatterns("/**") 拦截所有请求
     * excludePathPatterns(...) 放行登录、注册、Knife4j文档页面
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/ai/suggest",
                        "/api/user/ai/nutrition",
                        "/api/user/ai/health",
                        "/api/user/banner/**",// 轮播图公开访问
                        "/api/user/dish/hot", // 热门菜品公开
                        "/api/user/dish/today", // 今日推荐公开
                        "/api/user/dish/list", // 全部分类菜品公开
                        "/api/category/list",   // 用户端分类公开
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/send-code",
                        "/api/user/forgot-password",
                        "/api/merchant/login",
                        "/api/merchant/send-code",
                        "/api/merchant/forgot-password",
                        "/api/user/merchant/business-status", // 商家营业状态公开
                        "/api/pay/notify/**",   // 支付异步回调公开
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-ui/**"


                );

    }

    /**
     * 配置跨域
     * 允许Vue3开发服务器（localhost:5173）跨域请求
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}