package com.smartrent.report.listener;

import com.smartrent.report.dto.RentalEventDto;
import com.smartrent.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportKafkaListenerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportKafkaListener reportKafkaListener;

    @Test
    void consumeEvent_delegatesToService() {
        RentalEventDto dto = new RentalEventDto();
        dto.setRentalId(1L);
        dto.setStatus("APROBADO");

        reportKafkaListener.consumeEvent(dto, "rentals.events", 1, 20L);

        Mockito.verify(reportService, Mockito.times(1)).recordRentalEvent(dto);

        reportKafkaListener.consumeEvent(null, "rentals.events", 1, 21L);
        Mockito.verify(reportService, Mockito.times(1)).recordRentalEvent((RentalEventDto) null);
    }

    @Test
    void handleDlt_doesNotThrow() {
        reportKafkaListener.handleDlt("dead letter payload", "rentals.events-dlt", 0, 1L);
        Mockito.verifyNoInteractions(reportService);
    }
}
