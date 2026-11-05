package cl.duoc.mssmartrentrentals.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("null")
public class RabbitMQConfig {

    public static final String EXCHANGE_CMD_DIRECT = "cmd.direct";
    public static final String EXCHANGE_SMARTRENT = "smartrent.exchange";
    public static final String QUEUE_ORDER_CREATED = "q.rentals.order.created";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_CMD_DIRECT, true, false);
    }

    @Bean
    public DirectExchange smartrentExchange() {
        return new DirectExchange(EXCHANGE_SMARTRENT, true, false);
    }

    @Bean
    public Queue rentalsOrderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED).build();
    }

    @Bean
    public Binding rentalsOrderCreatedBinding(Queue rentalsOrderCreatedQueue, DirectExchange smartrentExchange) {
        return BindingBuilder.bind(rentalsOrderCreatedQueue).to(smartrentExchange).with(ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    @SuppressWarnings("all")
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        if (connectionFactory instanceof CachingConnectionFactory cachingFactory) {
            cachingFactory.setConnectionTimeout(10000);
        }
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
