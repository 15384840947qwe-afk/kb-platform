package com.lyq.kb.common;

import lombok.Data;

/**
 * 统一响应包装：所有Controller都返回这个结构。
 * 前端约定：code=200成功，其他都是失败，失败时读message提示用户
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    /** 无返回数据的成功，比如"删除成功" */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    /** 业务错误的快捷写法，默认500 */
    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}