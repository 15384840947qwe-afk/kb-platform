package com.lyq.kb.rabbit;

import com.lyq.kb.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** 文件事件生产者：上传成功后发事件，主流程不等后续处理 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendFileUploaded(Long fileId, String originalName) {
        String msg = String.format("{\"fileId\":%d,\"originalName\":\"%s\"}", fileId, originalName);
        rabbitTemplate.convertAndSend(RabbitConfig.FILE_EXCHANGE, RabbitConfig.FILE_KEY, msg);
        log.info("已发送文件上传事件：{}", msg);
    }
}