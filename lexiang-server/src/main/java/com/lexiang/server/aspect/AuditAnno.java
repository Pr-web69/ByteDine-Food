package com.lexiang.server.aspect;

import java.lang.annotation.*;

/**
 * 操作审计注解
 * 用法：@AuditAnno(module = "订单管理", action = "创建订单")
 * 贴在 Controller 方法上，AOP 切面自动记录操作日志
 */
// 仅能标注在方法上
@Target(ElementType.METHOD)
// 运行时保留，AOP切面运行阶段可以读取注解参数
@Retention(RetentionPolicy.RUNTIME)
// 生成javadoc文档
@Documented
public @interface AuditAnno {
    /** 操作模块 */
    String module() default "";
    /** 操作行为 */
    String action() default "";
}
