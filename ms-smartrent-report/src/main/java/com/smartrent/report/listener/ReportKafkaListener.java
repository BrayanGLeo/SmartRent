package com.smartrent.report.listener;

import com.smartrent.report.dto.RentalEventDto;
import com.smartrent.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ReportKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ReportKafkaListener.class);
    private final ReportService reportService;

    public ReportKafkaListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(
            topics = "${smartrent.kafka.topics.rentals:rentals.events}",
            groupId = "${smartrent.kafka.consumer.group-id:report-group}",
            containerFactory = "rentalKafkaListenerContainerFactory"
    )
    public void consumeEvent(
            RentalEventDto event,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, defaultValue = "0") int partition,
            @Header(name = KafkaHeaders.OFFSET, defaultValue = "0") long offset) {
        log.info("Evento de arriendo recibido para reportería: topic={}, partition={}, offset={}, rentalId={}",
                topic, partition, offset, event != null ? event.getRentalId() : null);
        reportService.recordRentalEvent(event);
    }

    @KafkaListener(
            topics = "${smartrent.kafka.topics.rentals-dlt:rentals.events-dlt}",
            groupId = "${smartrent.kafka.consumer.dlt-group-id:report-group-dlt}",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    public void handleDlt(
            String message,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, defaultValue = "0") int partition,
            @Header(name = KafkaHeaders.OFFSET, defaultValue = "0") long offset) {
        log.error("Evento de reportería recibido en DLT: topic={}, partition={}, offset={}, payload={}",
                topic, partition, offset, message);
    }
}
