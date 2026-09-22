package com.smartrent.notify.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void testQueues() {
        Queue emailQueue = config.emailQueue();
        assertNotNull(emailQueue);
        assertEquals(RabbitMQConfig.QUEUE_EMAIL, emailQueue.getName());

        Queue prepQueue = config.prepQueue();
        assertNotNull(prepQueue);
        assertEquals(RabbitMQConfig.QUEUE_PREP, prepQueue.getName());

        Queue emailDlqQueue = config.emailDlqQueue();
        assertNotNull(emailDlqQueue);
        assertEquals(RabbitMQConfig.QUEUE_EMAIL_DLQ, emailDlqQueue.getName());
    }

    @Test
    void testExchangeAndBindings() {
        DirectExchange exchange = config.notifyExchange();
        assertNotNull(exchange);
        assertEquals(RabbitMQConfig.EXCHANGE_NOTIFY, exchange.getName());

        Binding emailBinding = config.emailBinding(config.emailQueue(), exchange);
        assertNotNull(emailBinding);
        assertEquals("email", emailBinding.getRoutingKey());

        Binding prepBinding = config.prepBinding(config.prepQueue(), exchange);
        assertNotNull(prepBinding);
        assertEquals("prep", prepBinding.getRoutingKey());
    }
}
