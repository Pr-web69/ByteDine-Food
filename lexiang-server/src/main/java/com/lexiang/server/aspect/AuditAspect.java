package com.lexiang.server.aspect;

import com.lexiang.server.entity.AuditLog;
import com.lexiang.server.interceptor.JwtInterceptor;
import com.lexiang.server.mapper.AuditLogMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作审计 AOP 切面
 * 1. @Around 环绕通知：既能记录入参，又不影响原方法执行
 * 2. Spring AOP + 自定义注解 实现无侵入式审计
 * 3. try-catch 保护：审计失败不影响业务主流程
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(auditAnno)")
    public Object audit(ProceedingJoinPoint jp, AuditAnno auditAnno) throws Throwable {
        Object result = jp.proceed(); // 先执行业务 → 审计不影响主流程

        try {
            // 获取当前用户信息
            Claims claims = JwtInterceptor.USER_HOLDER.get();
            Long operatorId = null;
            String operatorType = "ANONYMOUS";
            if (claims != null) {
                operatorId = claims.get("userId", Long.class);
                Object type = claims.get("userType");
                operatorType = (type != null && !"MERCHANT".equals(type.toString())) ? "USER" : "ADMIN";
            }

            // 获取请求 IP
            String ip = "127.0.0.1";
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
                }
            } catch (Exception ignored) {}

            // 写入审计日志
            AuditLog logEntry = new AuditLog();
            logEntry.setOperatorId(operatorId);
            logEntry.setOperatorType(operatorType);
            logEntry.setModule(auditAnno.module());
            logEntry.setAction(auditAnno.action());
            logEntry.setDetail(getDetail(jp));
            logEntry.setIp(ip);
            auditLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("审计日志写入失败（不影响业务）: {}", e.getMessage());
        }

        return result;
    }

    /** 提取关键入参作为操作详情 */
    private String getDetail(ProceedingJoinPoint jp) {
        try {
            Object[] args = jp.getArgs();
            if (args.length > 0 && args[0] != null) {
                String s = args[0].toString();
                return s.length() > 200 ? s.substring(0, 200) + "..." : s;
            }
        } catch (Exception ignored) {}
        return "";
    }
}