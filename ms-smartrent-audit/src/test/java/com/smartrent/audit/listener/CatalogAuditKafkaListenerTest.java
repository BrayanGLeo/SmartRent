package com.smartrent.audit.listener;

import com.smartrent.audit.dto.CatalogEventDto;
import com.smartrent.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogAuditKafkaListenerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CatalogAuditKafkaListener listener;

    @Test
    void consumeCatalogEvent_delegatesToAuditService() {
        CatalogEventDto dto = new CatalogEventDto();
        dto.setMachineId(5L);
        dto.setEventType("MACHINE_UPDATED");

        listener.consumeCatalogEvent(dto, "catalog.events", 0, 12L);

        verify(auditService).processCatalogEvent(dto, "catalog.events", 0, 12L);
    }

    @Test
    void handleCatalogDlt_delegatesToAuditService() {
        listener.handleCatalogDlt("bad catalog msg", "catalog.events-dlt", 2, 4L);

        verify(auditService).recordDeadLetterEvent("CATALOG_EVENT_DLT", "bad catalog msg", "catalog.events-dlt", 2, 4L);
    }
}
