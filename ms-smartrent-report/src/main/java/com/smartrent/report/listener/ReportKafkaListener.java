package com.smartrent.report.listener;

import com.smartrent.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ReportKafkaListener.class);
    private final ReportService reportService;

    public ReportKafkaListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(topics = "rentals.events", groupId = "report-group")
    public void consumeEvent(String message) {
        log.info("Evento recibido para reportería: {}", message);
        reportService.recordRentalEvent(message);
    }
}
