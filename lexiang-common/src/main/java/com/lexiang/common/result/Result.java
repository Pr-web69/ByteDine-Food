package com.lexiang.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果类
 * 所有Controller接口必须返回 Result<T>，前端统一解析格式为：
 * { "code": 200, "message": "操作成功", "data": {...} }
 *
 * @param <T> 泛型数据，可以是单个对象、列表、分页对象等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码：200成功、400参数错误、401未登录、500服务端异常 */
    private Integer code;

    /** 提示信息：成功/失败的具体描述 */
    private String message;

    /** 返回数据：泛型，无数据时为null */
    private T data;

    /**
     * 快速创建成功响应（带数据）
     * 例：return Result.success(userList);
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 快速创建成功响应（无数据）
     * 例：return Result.success();
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 快速创建失败响应（自定义状态码和消息）
     * 例：return Result.error(401, "请先登录");
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 快速创建失败响应（默认400参数错误）
     * 例：return Result.error("用户名已存在");
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(400, message, null);
    }
}
