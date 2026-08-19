package com.lyq.kb.common;

/**
 * 权限不足异常。至此错误语义凑齐三件套：
 * 400=参数/业务错（IllegalArgumentException）、401=没登录（拦截器）、403=登录了但权限不够（本异常）
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}