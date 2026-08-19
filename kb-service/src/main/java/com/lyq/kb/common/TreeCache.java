package com.lyq.kb.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 目录树缓存：读多写少的数据放Redis，避免每次打开页面都查库组树。
 * 失效策略"写时删除"：任何改动树的操作后调evict；
 * 再叠30分钟过期兜底——就算哪处漏了evict，缓存也会自己愈合
 */
@Component
@RequiredArgsConstructor
public class TreeCache {

    private static final String KEY = "kb:tree:";

    private final StringRedisTemplate redisTemplate;

    public String get(Long kbId) {
        return redisTemplate.opsForValue().get(KEY + kbId);
    }

    public void put(Long kbId, String json) {
        redisTemplate.opsForValue().set(KEY + kbId, json, Duration.ofMinutes(30));
    }

    public void evict(Long kbId) {
        redisTemplate.delete(KEY + kbId);
    }
}
