package com.lyq.kb.config;

import com.lyq.kb.common.RateLimit;
import com.lyq.kb.common.RateLimitException;
import com.lyq.kb.common.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * AI接口限流切面：拦截所有标了@RateLimit的Controller方法。
 *
 * 方案同limiter-demo：Redis + Lua脚本原子计数（incr+expire一条命令完成，
 * 不会出现"计数了但没设过期时间导致永久封锁"的竞态）。
 * 与demo的两点不同：
 * 1) 维度从IP换成用户id——请求都带JWT，UserContext里就有身份，
 *    按用户限不会误伤同出口IP的室友，也不会被伪造X-Forwarded-For绕过；
 * 2) Redis异常时放行（fail-open）：限流是保护措施不是核心功能，
 *    Redis挂了宁可放行也不能把整个AI功能打挂。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    /** 原子计数：第一次incr后设置窗口过期时间，返回当前计数 */
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('incr', KEYS[1])\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call('expire', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return current;", Long.class);

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String methodKey = signature.getMethod().getDeclaringClass().getSimpleName()
                + "." + signature.getMethod().getName();

        // 按用户维度：未登录兜底用anon（正常走不到，拦截器已拦未登录请求）
        UserContext.CurrentUser user = UserContext.get();
        String userKey = user != null ? "u" + user.getId() : "anon";
        String redisKey = "kb:rl:" + userKey + ":" + methodKey;

        Long count;
        try {
            count = redisTemplate.execute(SCRIPT, Collections.singletonList(redisKey),
                    String.valueOf(rateLimit.timeWindow()));
        } catch (Exception e) {
            // Redis不可用：放行并告警，限流失效不影响业务可用
            log.warn("限流检查失败(Redis不可用)，本次放行: {}", redisKey, e);
            return pjp.proceed();
        }

        if (count != null && count > rateLimit.maxCount()) {
            log.info("用户[{}]触发限流: {} ({}/{}s)", userKey, methodKey, count, rateLimit.timeWindow());
            throw new RateLimitException(rateLimit.message());
        }
        return pjp.proceed();
    }
}
