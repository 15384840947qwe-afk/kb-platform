package com.lyq.kb.rabbit;

import com.lyq.kb.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** 审核事件生产者：通过时发一条消息，主流程不等后续处理 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendDocApproved(Long kbId, String title) {
        String msg = String.format("{\"kbId\":%d,\"title\":\"%s\"}", kbId, title);
        rabbitTemplate.convertAndSend(RabbitConfig.AUDIT_EXCHANGE, RabbitConfig.AUDIT_KEY, msg);
        log.info("已发送审核通过事件：{}", msg);
    }
}
