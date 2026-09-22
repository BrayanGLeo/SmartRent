package cl.duoc.mssmartrentrentals.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RentalEventPublisher {

    public static final String RENTAL_ID_KEY = "rentalId";

    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RentalEventPublisher(RabbitTemplate rabbitTemplate, KafkaTemplate<String, Object> kafkaTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEmailEvent(Long rentalId, String userEmail, String status) {
        Map<String, Object> message = Map.of(
            RENTAL_ID_KEY, rentalId,
            "userEmail", userEmail,
            "status", status,
            "action", "email.send"
        );
        // Enviando al exchange "cmd.direct" con routing key "email.send"
        rabbitTemplate.convertAndSend("cmd.direct", "email.send", message);
    }

    public void publishPrepEvent(Long rentalId, Long machineId) {
        Map<String, Object> message = Map.of(
            RENTAL_ID_KEY, rentalId,
            "machineId", machineId,
            "action", "print.ticket"
        );
        // Enviando directo a la cola "q.cmd.prep"
        rabbitTemplate.convertAndSend("q.cmd.prep", message);
    }

    public void publishAuditAndReportEvent(Long rentalId, String userId, String status) {
        Map<String, Object> payload = Map.of(
            RENTAL_ID_KEY, rentalId,
            "userId", userId,
            "status", status,
            "timestamp", System.currentTimeMillis()
        );
        // Enviando al tópico de Kafka "rentals.events"
        kafkaTemplate.send("rentals.events", payload);
    }
}
