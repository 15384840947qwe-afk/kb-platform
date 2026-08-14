package com.lyq.kb.common;

/**
 * 限流触发时抛出：由GlobalExceptionHandler统一转成HTTP 429。
 * 用独立异常类型（而非复用IllegalArgument的400），
 * 前端好区分"业务错误"和"请求太频繁"两种场景给不同提示。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
