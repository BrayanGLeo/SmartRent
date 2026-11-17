package com.smartrent.audit.listener;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import com.smartrent.audit.entity.AuditTimeline;
import com.smartrent.audit.repository.AuditTimelineRepository;

@Component
public class AuditKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaListener.class);
    private final AuditTimelineRepository repository;

    public AuditKafkaListener(AuditTimelineRepository repository) {
        this.repository = repository;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "false",
        dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "rentals.events", groupId = "audit-group")
    public void consumeEvent(String message) {
        if (message == null || message.isBlank()) {
            log.warn("Mensaje nulo o vacío ignorado en tópico rentals.events");
            return;
        }
        log.info("Evento recibido para auditoría: {}", message);
        AuditTimeline timeline = new AuditTimeline(
            "RENTAL_EVENT",
            message,
            LocalDateTime.now(ZoneId.systemDefault())
        );
        repository.save(timeline);
    }

    @DltHandler
    public void handleDlt(String message) {
        log.error("Evento de auditoría enviado a Dead Letter Topic (rentals.events-dlt): {}", message);
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            AuditTimeline fallbackTimeline = new AuditTimeline(
                "RENTAL_EVENT_DLT",
                message,
                LocalDateTime.now(ZoneId.systemDefault())
            );
            repository.save(fallbackTimeline);
        } catch (Exception ex) {
            log.error("No se pudo persistir evento DLT en base de datos de auditoría: {}", ex.getMessage());
        }
    }
}
