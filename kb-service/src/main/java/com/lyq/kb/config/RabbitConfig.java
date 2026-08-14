package com.lyq.kb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

@Configuration
public class RabbitConfig {

    public static final String FILE_EXCHANGE = "kb.file.events";
    public static final String FILE_QUEUE = "file-uploaded-queue";
    public static final String FILE_KEY = "file.uploaded";

    public static final String AUDIT_EXCHANGE = "kb.audit.events";
    public static final String AUDIT_QUEUE = "audit-approved-queue";
    public static final String AUDIT_KEY = "audit.approved";

    @Bean
    public TopicExchange fileExchange() {
        return new TopicExchange(FILE_EXCHANGE, true, false);
    }

    @Bean
    public Queue fileQueue() {
        return new Queue(FILE_QUEUE, true);
    }

    @Bean
    public Binding fileBinding(Queue fileQueue, TopicExchange fileExchange) {
        return BindingBuilder.bind(fileQueue).to(fileExchange).with(FILE_KEY);
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return new Queue(AUDIT_QUEUE, true);
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditQueue).to(auditExchange).with(AUDIT_KEY);
    }
}