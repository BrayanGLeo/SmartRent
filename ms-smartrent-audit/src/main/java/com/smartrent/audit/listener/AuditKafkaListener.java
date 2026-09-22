package com.smartrent.audit.listener;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
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

    @KafkaListener(topics = "rentals.events", groupId = "audit-group")
    public void consumeEvent(String message) {
        log.info("Evento recibido para auditoría: {}", message);
        AuditTimeline timeline = new AuditTimeline(
            "RENTAL_EVENT", 
            message, 
            LocalDateTime.now(ZoneId.systemDefault())
        );
        repository.save(timeline);
    }
}
