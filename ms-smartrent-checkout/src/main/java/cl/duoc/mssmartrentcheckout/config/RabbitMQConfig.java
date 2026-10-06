package cl.duoc.mssmartrentcheckout.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "smartrent.exchange";

    @Bean
    public DirectExchange directExchange() {
        // durable=true, autoDelete=false — se declarará cuando RabbitMQ esté disponible
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    @SuppressWarnings("all")
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        // Configurar retry a nivel de conexión para tolerar que RabbitMQ
        // no esté listo al momento del arranque
        if (connectionFactory instanceof CachingConnectionFactory cachingFactory) {
            cachingFactory.setConnectionTimeout(10000);
        }
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
