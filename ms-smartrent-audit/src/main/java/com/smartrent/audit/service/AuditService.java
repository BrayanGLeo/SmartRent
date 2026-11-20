package com.smartrent.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.audit.dto.CatalogEventDto;
import com.smartrent.audit.dto.RentalEventDto;
import com.smartrent.audit.entity.AuditTimeline;
import com.smartrent.audit.repository.AuditTimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditTimelineRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditTimelineRepository repository) {
        this(repository, new ObjectMapper());
    }

    @Autowired
    public AuditService(AuditTimelineRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public AuditTimeline processRentalEvent(RentalEventDto event, String topic, int partition, long offset) {
        if (event == null || event.getRentalId() == null
                || event.getStatus() == null || event.getStatus().isBlank()) {
            throw new IllegalArgumentException("Evento de arriendo inválido: rentalId y status son obligatorios");
        }

        String eventType = (event.getEventType() != null && !event.getEventType().isBlank())
                ? event.getEventType()
                : "RENTAL_EVENT";
        String payload = serializePayload(event);

        log.info("Auditando evento de arriendo: topic={}, partition={}, offset={}, rentalId={}, status={}, eventType={}",
                topic, partition, offset, event.getRentalId(), event.getStatus(), eventType);

        AuditTimeline timeline = new AuditTimeline(
                eventType,
                payload,
                LocalDateTime.now(ZoneId.systemDefault())
        );
        return repository.save(timeline);
    }

    public AuditTimeline processCatalogEvent(CatalogEventDto event, String topic, int partition, long offset) {
        if (event == null || event.getMachineId() == null
                || event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("Evento de catálogo inválido: machineId y eventType son obligatorios");
        }

        String payload = serializePayload(event);
        log.info("Auditando evento de catálogo: topic={}, partition={}, offset={}, machineId={}, eventType={}",
                topic, partition, offset, event.getMachineId(), event.getEventType());

        AuditTimeline timeline = new AuditTimeline(
                event.getEventType(),
                payload,
                LocalDateTime.now(ZoneId.systemDefault())
        );
        return repository.save(timeline);
    }

    public void recordDeadLetterEvent(String dltType, String rawPayload, String topic, int partition, long offset) {
        if (rawPayload == null || rawPayload.isBlank()) {
            log.warn("Mensaje DLT nulo o vacío ignorado en topic={}, partition={}, offset={}", topic, partition, offset);
            return;
        }
        String resolvedDltType = (dltType != null && !dltType.isBlank()) ? dltType : "KAFKA_EVENT_DLT";
        log.error("Evento recibido en Dead Letter Topic: topic={}, partition={}, offset={}, type={}, payload={}",
                topic, partition, offset, resolvedDltType, rawPayload);
        try {
            AuditTimeline fallback = new AuditTimeline(
                    resolvedDltType,
                    rawPayload,
                    LocalDateTime.now(ZoneId.systemDefault())
            );
            repository.save(fallback);
        } catch (Exception ex) {
            log.error("No se pudo persistir evento DLT en base de datos de auditoría: {}", ex.getMessage());
        }
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Fallo serializando payload a JSON, usando representación textual: {}", ex.getMessage());
            return String.valueOf(payload);
        }
    }
}
