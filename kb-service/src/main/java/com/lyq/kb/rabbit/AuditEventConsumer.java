package com.lyq.kb.rabbit;

import com.lyq.kb.common.TreeCache;
import com.lyq.kb.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 审核通过事件消费者：主流程返回后这里异步干三件事——
 * 通知日志（将来接站内信/邮件的坑位）、Redis审批计数+1、
 * 再清一次树缓存做双保险（主流程已同步清过，这里防漏）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private static final Pattern KB_ID = Pattern.compile("\"kbId\":(\\d+)");

    private final StringRedisTemplate redisTemplate;
    private final TreeCache treeCache;

    @RabbitListener(queues = RabbitConfig.AUDIT_QUEUE)
    public void onAuditApproved(String msg) {
        log.info("[异步]收到审核通过通知：{}", msg);
        // 审批计数：将来管理端做"已审批N篇"统计直接读它
        redisTemplate.opsForValue().increment("kb:stats:approvedCount");
        Matcher m = KB_ID.matcher(msg);
        if (m.find()) {
            treeCache.evict(Long.parseLong(m.group(1)));
        }
    }
}
