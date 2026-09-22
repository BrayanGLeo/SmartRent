package com.smartrent.notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_EMAIL = "q.cmd.email";
    public static final String QUEUE_PREP = "q.cmd.prep";
    public static final String QUEUE_EMAIL_DLQ = "q.cmd.email.dlq";
    
    public static final String EXCHANGE_NOTIFY = "x.notify";

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_EMAIL_DLQ)
                .build();
    }

    @Bean
    public Queue prepQueue() {
        return QueueBuilder.durable(QUEUE_PREP).build();
    }

    @Bean
    public Queue emailDlqQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL_DLQ).build();
    }

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(EXCHANGE_NOTIFY);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange notifyExchange) {
        return BindingBuilder.bind(emailQueue).to(notifyExchange).with("email");
    }

    @Bean
    public Binding prepBinding(Queue prepQueue, DirectExchange notifyExchange) {
        return BindingBuilder.bind(prepQueue).to(notifyExchange).with("prep");
    }
}
