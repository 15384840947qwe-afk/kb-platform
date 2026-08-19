package com.lyq.kb.common;

import lombok.Data;

/**
 * 当前登录用户上下文：拦截器解析token后把身份存进来，
 * 之后Controller/Service任何地方用UserContext.get()就能拿到"我是谁"。
 * 底层是ThreadLocal——Tomcat里一个请求固定由一个线程处理，
 * ThreadLocal天然做到"每个请求一份独立的身份"，互不串
 */
public class UserContext {

    @Data
    public static class CurrentUser {
        private Long id;
        private String username;
        private String role;
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    /**
     * 请求结束必须调用！Tomcat线程池会复用线程，不清的话，
     * 同一线程处理的下一个请求会"继承"上一个人的身份——A用户看到B的数据，事故级漏洞。
     * 你在short-url里ShardingContext用完finally清理，就是同一个道理
     */
    public static void clear() {
        HOLDER.remove();
    }
}