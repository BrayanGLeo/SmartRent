package cl.duoc.mssmartrentrentals.events;

import cl.duoc.mssmartrentrentals.dto.RentalEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class RentalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RentalEventPublisher.class);
    public static final String RENTAL_ID_KEY = "rentalId";
    public static final String DEFAULT_RENTALS_TOPIC = "rentals.events";
    public static final String SOURCE_SERVICE = "ms-smartrent-rentals";

    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rentalsTopic;

    public RentalEventPublisher(RabbitTemplate rabbitTemplate, KafkaTemplate<String, Object> kafkaTemplate) {
        this(rabbitTemplate, kafkaTemplate, DEFAULT_RENTALS_TOPIC);
    }

    @Autowired
    public RentalEventPublisher(
            RabbitTemplate rabbitTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${smartrent.kafka.topics.rentals:" + DEFAULT_RENTALS_TOPIC + "}") String rentalsTopic) {
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.rentalsTopic = (rentalsTopic != null && !rentalsTopic.isBlank()) ? rentalsTopic : DEFAULT_RENTALS_TOPIC;
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
        publishAuditAndReportEvent(rentalId, machineId, userId, null, status, "RENTAL_EVENT");
    }

    public void publishAuditAndReportEvent(
            Long rentalId,
            Long machineId,
            String userId,
            String previousStatus,
            String status,
            String eventType) {
        if (rentalId == null || status == null || status.isBlank()) {
            log.warn("Evento Kafka omitido por datos inválidos: rentalId={}, status={}", rentalId, status);
            return;
        }

        Long resolvedMachineId = machineId != null ? machineId : 1L;
        String resolvedUserId = (userId != null && !userId.isBlank()) ? userId : "unknown";
        String resolvedEventType = (eventType != null && !eventType.isBlank()) ? eventType : "RENTAL_EVENT";
        Instant now = Instant.now();

        RentalEventDto payload = new RentalEventDto(
                UUID.randomUUID().toString(),
                resolvedEventType,
                rentalId,
                resolvedMachineId,
                resolveMachineName(resolvedMachineId),
                resolvedUserId,
                previousStatus,
                status,
                SOURCE_SERVICE,
                now.toEpochMilli(),
                now.toString()
        );

        try {
            String key = Objects.requireNonNull(String.valueOf(rentalId));
            var future = kafkaTemplate.send(Objects.requireNonNull(rentalsTopic), key, payload);
            if (Objects.nonNull(future)) {
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error asíncrono enviando evento a Kafka ({}) para rentalId={}, eventId={}: {}",
                                rentalsTopic, rentalId, payload.getEventId(), ex.getMessage());
                    } else if (result != null && result.getRecordMetadata() != null) {
                        var meta = result.getRecordMetadata();
                        log.info("Evento Kafka publicado: topic={}, partition={}, offset={}, rentalId={}, eventId={}",
                                meta.topic(), meta.partition(), meta.offset(), rentalId, payload.getEventId());
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Error enviando evento a Kafka ({}) para rentalId={}: {}", rentalsTopic, rentalId, ex.getMessage());
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
