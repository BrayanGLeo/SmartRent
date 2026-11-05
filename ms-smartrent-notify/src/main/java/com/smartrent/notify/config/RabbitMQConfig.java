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
    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_SMARTRENT = "smartrent.exchange";

    public static final String ROUTING_KEY_EMAIL = "email";
    public static final String ROUTING_KEY_PREP = "prep";
    public static final String ROUTING_KEY_CMD_EMAIL = "email.send";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";
    public static final String ROUTING_KEY_CHECKOUT = "smartrent.routing.checkout";

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
        return new DirectExchange(EXCHANGE_NOTIFY, true, false);
    }

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public DirectExchange smartrentExchange() {
        return new DirectExchange(EXCHANGE_SMARTRENT, true, false);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange notifyExchange) {
        return BindingBuilder.bind(emailQueue).to(notifyExchange).with(ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding prepBinding(Queue prepQueue, DirectExchange notifyExchange) {
        return BindingBuilder.bind(prepQueue).to(notifyExchange).with(ROUTING_KEY_PREP);
    }

    @Bean
    public Binding cmdEmailBinding(Queue emailQueue, DirectExchange cmdDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(cmdDirectExchange).with(ROUTING_KEY_CMD_EMAIL);
    }

    @Bean
    public Binding orderCreatedBinding(Queue emailQueue, DirectExchange smartrentExchange) {
        return BindingBuilder.bind(emailQueue).to(smartrentExchange).with(ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    public Binding checkoutRoutingBinding(Queue emailQueue, DirectExchange smartrentExchange) {
        return BindingBuilder.bind(emailQueue).to(smartrentExchange).with(ROUTING_KEY_CHECKOUT);
    }
}
