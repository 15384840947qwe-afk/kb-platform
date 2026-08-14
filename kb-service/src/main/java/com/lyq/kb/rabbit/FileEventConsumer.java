package com.lyq.kb.rabbit;

import com.lyq.kb.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 文件事件消费者：现在只打日志演示异步链路，
 * 将来挂缩略图生成、病毒扫描、通知等都加在这里，上传接口不用改
 */
@Slf4j
@Component
public class FileEventConsumer {

    @RabbitListener(queues = RabbitConfig.FILE_QUEUE)
    public void onFileUploaded(String msg) {
        log.info("[异步]收到文件上传事件，开始后续处理：{}", msg);
    }
}