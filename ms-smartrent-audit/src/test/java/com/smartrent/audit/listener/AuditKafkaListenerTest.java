package com.smartrent.audit.listener;

import com.smartrent.audit.dto.RentalEventDto;
import com.smartrent.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditKafkaListenerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditKafkaListener auditKafkaListener;

    @Test
    void consumeEvent_delegatesToAuditService() {
        RentalEventDto dto = new RentalEventDto();
        dto.setRentalId(1L);
        dto.setStatus("APROBADO");

        auditKafkaListener.consumeEvent(dto, "rentals.events", 2, 15L);

        verify(auditService).processRentalEvent(dto, "rentals.events", 2, 15L);
    }

    @Test
    void handleDlt_delegatesToAuditService() {
        auditKafkaListener.handleDlt("failed payload", "rentals.events-dlt", 1, 9L);

        verify(auditService).recordDeadLetterEvent("RENTAL_EVENT_DLT", "failed payload", "rentals.events-dlt", 1, 9L);
    }
}
