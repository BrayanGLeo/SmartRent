package com.smartrent.audit.listener;

import com.smartrent.audit.dto.CatalogEventDto;
import com.smartrent.audit.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class CatalogAuditKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogAuditKafkaListener.class);
    private final AuditService auditService;

    public CatalogAuditKafkaListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${smartrent.kafka.topics.catalog:catalog.events}",
            groupId = "${smartrent.kafka.consumer.group-id:audit-group}",
            containerFactory = "catalogKafkaListenerContainerFactory"
    )
    public void consumeCatalogEvent(
            CatalogEventDto event,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, defaultValue = "0") int partition,
            @Header(name = KafkaHeaders.OFFSET, defaultValue = "0") long offset) {
        log.debug("Mensaje recibido en CatalogAuditKafkaListener: topic={}, partition={}, offset={}", topic, partition, offset);
        auditService.processCatalogEvent(event, topic, partition, offset);
    }

    @KafkaListener(
            topics = "${smartrent.kafka.topics.catalog-dlt:catalog.events-dlt}",
            groupId = "${smartrent.kafka.consumer.dlt-group-id:audit-group-dlt}",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    public void handleCatalogDlt(
            String message,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, defaultValue = "0") int partition,
            @Header(name = KafkaHeaders.OFFSET, defaultValue = "0") long offset) {
        auditService.recordDeadLetterEvent("CATALOG_EVENT_DLT", message, topic, partition, offset);
    }
}
