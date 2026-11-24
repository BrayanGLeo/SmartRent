package cl.duoc.mssmartrentcatalog.events;

import cl.duoc.mssmartrentcatalog.dto.CatalogEventDto;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class CatalogEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventPublisher.class);
    public static final String DEFAULT_CATALOG_TOPIC = "catalog.events";
    public static final String SOURCE_SERVICE = "ms-smartrent-catalog";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String catalogTopic;

    public CatalogEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this(kafkaTemplate, DEFAULT_CATALOG_TOPIC);
    }

    @Autowired
    public CatalogEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${smartrent.kafka.topics.catalog:" + DEFAULT_CATALOG_TOPIC + "}") String catalogTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.catalogTopic = (catalogTopic != null && !catalogTopic.isBlank()) ? catalogTopic : DEFAULT_CATALOG_TOPIC;
    }

    public void publishMachineEvent(String eventType, Machine machine) {
        if (machine == null || machine.getId() == null) {
            log.warn("Evento Kafka de catálogo omitido por máquina nula o sin id: eventType={}", eventType);
            return;
        }
        String resolvedType = (eventType != null && !eventType.isBlank()) ? eventType : "MACHINE_UPDATED";
        String categoryName = machine.getCategory() != null ? machine.getCategory().getName() : null;
        Instant now = Instant.now();

        CatalogEventDto dto = new CatalogEventDto(
                UUID.randomUUID().toString(),
                resolvedType,
                machine.getId(),
                machine.getName(),
                machine.getSerialNumber(),
                categoryName,
                machine.getDailyPrice(),
                machine.getIsAvailable(),
                SOURCE_SERVICE,
                now.toEpochMilli(),
                now.toString()
        );
        sendEvent(String.valueOf(machine.getId()), dto);
    }

    public void publishMachineDeleted(Long machineId) {
        if (machineId == null) {
            log.warn("Evento Kafka MACHINE_DELETED omitido por machineId nulo");
            return;
        }
        Instant now = Instant.now();
        CatalogEventDto dto = new CatalogEventDto(
                UUID.randomUUID().toString(),
                "MACHINE_DELETED",
                machineId,
                null,
                null,
                null,
                null,
                false,
                SOURCE_SERVICE,
                now.toEpochMilli(),
                now.toString()
        );
        sendEvent(String.valueOf(machineId), dto);
    }

    private void sendEvent(String key, CatalogEventDto dto) {
        try {
            var future = kafkaTemplate.send(Objects.requireNonNull(catalogTopic), Objects.requireNonNull(key), dto);
            if (Objects.nonNull(future)) {
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error asíncrono enviando evento a Kafka ({}) para machineId={}, eventId={}: {}",
                                catalogTopic, dto.getMachineId(), dto.getEventId(), ex.getMessage());
                    } else if (result != null && result.getRecordMetadata() != null) {
                        var meta = result.getRecordMetadata();
                        log.info("Evento catálogo publicado: topic={}, partition={}, offset={}, machineId={}, eventType={}",
                                meta.topic(), meta.partition(), meta.offset(), dto.getMachineId(), dto.getEventType());
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Error enviando evento a Kafka ({}) para machineId={}: {}",
                    catalogTopic, dto.getMachineId(), ex.getMessage());
        }
    }
}
