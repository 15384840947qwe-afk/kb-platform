package com.lyq.kb.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI接口限流注解：标在Controller方法上，由RateLimitAspect统一拦截。
 * 按"当前登录用户"维度计数（UserContext取用户id），防止单用户刷接口烧token。
 * 参考limiter-demo的注解+切面方案，但维度从IP换成了用户——
 * 本项目所有请求都带JWT，按用户限比按IP准（同宿舍同出口IP不会误伤）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口，单位秒，如60=一分钟 */
    int timeWindow() default 60;

    /** 窗口内最多允许的调用次数 */
    int maxCount() default 5;

    /** 超限时的提示文案 */
    String message() default "请求太频繁，请稍后再试";
}
