package com.smartrent.notify.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.smartrent.notify.config.RabbitMQConfig;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL)
    public void processEmail(String message) {
        log.info("Email enviado al usuario: Tu arriendo ha sido Aprobado. Detalles: {}", message);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PREP)
    public void processPrep(String message) {
        log.info("Imprimiendo ticket de picking para la máquina. Detalles: {}", message);
    }
}
