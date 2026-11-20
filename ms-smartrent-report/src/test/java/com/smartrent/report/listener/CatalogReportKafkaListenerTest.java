package com.smartrent.report.listener;

import com.smartrent.report.dto.CatalogEventDto;
import com.smartrent.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogReportKafkaListenerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private CatalogReportKafkaListener listener;

    @Test
    void consumeCatalogEvent_delegatesToService() {
        CatalogEventDto dto = new CatalogEventDto();
        dto.setMachineId(4L);
        dto.setEventType("MACHINE_AVAILABILITY_CHANGED");

        listener.consumeCatalogEvent(dto, "catalog.events", 2, 11L);
        Mockito.verify(reportService).recordCatalogEvent(dto);

        listener.consumeCatalogEvent(null, "catalog.events", 2, 12L);
        Mockito.verify(reportService).recordCatalogEvent((CatalogEventDto) null);
    }

    @Test
    void handleCatalogDlt_doesNotThrow() {
        listener.handleCatalogDlt("dead catalog payload", "catalog.events-dlt", 1, 3L);
        Mockito.verifyNoInteractions(reportService);
    }
}
