package com.smartrent.report.listener;

import com.smartrent.report.dto.CatalogEventDto;
import com.smartrent.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class CatalogReportKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogReportKafkaListener.class);
    private final ReportService reportService;

    public CatalogReportKafkaListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(
            topics = "${smartrent.kafka.topics.catalog:catalog.events}",
            groupId = "${smartrent.kafka.consumer.group-id:report-group}",
            containerFactory = "catalogKafkaListenerContainerFactory"
    )
    public void consumeCatalogEvent(
            CatalogEventDto event,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, defaultValue = "0") int partition,
            @Header(name = KafkaHeaders.OFFSET, defaultValue = "0") long offset) {
        log.info("Evento de catálogo recibido para reportería: topic={}, partition={}, offset={}, machineId={}, eventType={}",
                topic, partition, offset, event != null ? event.getMachineId() : null,
                event != null ? event.getEventType() : null);
        reportService.recordCatalogEvent(event);
    }

    @KafkaListener(
            topics = "${smartrent.kafka.topics.catalog-dlt:catalog.events-dlt}",
            groupId = "${smartrent.kafka.consumer.dlt-group-id:report-group-dlt}",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    public void handleCatalogDlt(
            String message,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, defaultValue = "0") int partition,
            @Header(name = KafkaHeaders.OFFSET, defaultValue = "0") long offset) {
        log.error("Evento de catálogo en reportería enviado a DLT: topic={}, partition={}, offset={}, payload={}",
                topic, partition, offset, message);
    }
}
