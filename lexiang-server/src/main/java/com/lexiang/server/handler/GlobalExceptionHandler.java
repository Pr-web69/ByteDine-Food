package com.lexiang.server.handler;

import com.lexiang.common.exception.BusinessException;
import com.lexiang.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 使用 @RestControllerAdvice 拦截所有Controller抛出的异常，
 * 统一包装成 Result.error 返回给前端，Controller层无需写try-catch
 *
 * 异常处理优先级：BusinessException > 参数校验异常 > 兜底Exception
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常
     * Service层 throw new BusinessException(400, "库存不足") 时触发
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {} - {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获参数校验失败异常（@Valid 校验不通过时抛出）
     * 取第一条校验失败的提示信息返回给前端
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    /**
     * 捕获 JSON 格式错误（前端传了非法 JSON 时触发）
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<?> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("请求体JSON格式错误: {}", e.getMessage());
        return Result.error(400, "请求参数格式错误，请检查JSON格式");
    }

    /**
     * 捕获 HTTP 请求方法错误（GET请求访问了POST接口等）
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.error(405, "请求方式错误，请检查接口调用方式");
    }

    /**
     * 兜底异常处理
     * 捕获所有未预料到的异常（空指针、SQL异常等），打印堆栈便于排查
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "服务器内部异常，请联系管理员");
    }
}
