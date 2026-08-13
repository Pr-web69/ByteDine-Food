package com.lexiang.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * 在Service层遇到业务错误（如库存不足、用户不存在）时手动抛出，
 * 由GlobalExceptionHandler统一捕获并转为Result.error返回给前端
 *
 * 使用例：throw new BusinessException(400, "库存不足");
 *         throw new BusinessException(401, "请先登录");
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 异常状态码，对应HTTP或业务错误码 */
    private final Integer code;

    /**
     * @param code    状态码
     * @param message 错误描述
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 默认400参数错误的构造方法
     * @param message 错误描述
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
