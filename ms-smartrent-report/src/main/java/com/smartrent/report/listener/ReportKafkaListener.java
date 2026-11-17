package com.smartrent.report.listener;

import com.smartrent.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class ReportKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ReportKafkaListener.class);
    private final ReportService reportService;

    public ReportKafkaListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "false",
        dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "rentals.events", groupId = "report-group")
    public void consumeEvent(String message) {
        if (message == null || message.isBlank()) {
            log.warn("Mensaje nulo o vacío ignorado en tópico rentals.events (reportería)");
            return;
        }
        log.info("Evento recibido para reportería: {}", message);
        reportService.recordRentalEvent(message);
    }

    @DltHandler
    public void handleDlt(String message) {
        log.error("Evento de reportería enviado a Dead Letter Topic (rentals.events-dlt): {}", message);
    }
}
