package cl.duoc.mssmartrentrentals.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RentalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RentalEventPublisher.class);
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
        try {
            // Enviando al exchange "cmd.direct" con routing key "email.send"
            rabbitTemplate.convertAndSend("cmd.direct", "email.send", message);
        } catch (Exception ex) {
            log.error("Error publicando evento de email en RabbitMQ para rentalId={}: {}", rentalId, ex.getMessage());
        }
    }

    public void publishPrepEvent(Long rentalId, Long machineId) {
        Map<String, Object> message = Map.of(
            RENTAL_ID_KEY, rentalId,
            "machineId", machineId,
            "action", "print.ticket"
        );
        try {
            // Enviando directo a la cola "q.cmd.prep"
            rabbitTemplate.convertAndSend("q.cmd.prep", message);
        } catch (Exception ex) {
            log.error("Error publicando comando de picking en RabbitMQ para rentalId={}: {}", rentalId, ex.getMessage());
        }
    }

    public void publishAuditAndReportEvent(Long rentalId, String userId, String status) {
        publishAuditAndReportEvent(rentalId, rentalId, userId, status);
    }

    public void publishAuditAndReportEvent(Long rentalId, Long machineId, String userId, String status) {
        Long resolvedMachineId = machineId != null ? machineId : 1L;
        Map<String, Object> payload = Map.of(
            RENTAL_ID_KEY, rentalId,
            "machineId", resolvedMachineId,
            "machineName", resolveMachineName(resolvedMachineId),
            "userId", userId,
            "status", status,
            "timestamp", System.currentTimeMillis()
        );
        try {
            // Enviando al tópico de Kafka "rentals.events" con rentalId como partition key
            String key = rentalId != null ? String.valueOf(rentalId) : "0";
            var future = kafkaTemplate.send("rentals.events", key, payload);
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error asíncrono enviando evento a Kafka (rentals.events) para rentalId={}: {}",
                                rentalId, ex.getMessage());
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Error enviando evento a Kafka (rentals.events) para rentalId={}: {}", rentalId, ex.getMessage());
        }
    }

    static String resolveMachineName(Long machineId) {
        if (machineId == null) {
            return "Maquina_Generica";
        }
        return switch (machineId.intValue()) {
            case 1 -> "Excavadora";
            case 2 -> "Retroexcavadora";
            case 3 -> "Rodillo Compactador";
            case 5 -> "Martillo Demoledor";
            case 7, 8 -> "Grua";
            case 9, 10 -> "Generador Industrial";
            default -> "Maquina_Generica";
        };
    }
}
