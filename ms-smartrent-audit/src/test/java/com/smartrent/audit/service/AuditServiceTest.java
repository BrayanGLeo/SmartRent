package com.smartrent.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.audit.dto.CatalogEventDto;
import com.smartrent.audit.dto.RentalEventDto;
import com.smartrent.audit.entity.AuditTimeline;
import com.smartrent.audit.repository.AuditTimelineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuditServiceTest {

    private static final String RENTALS_TOPIC = "rentals.events";
    private static final String RENTALS_DLT_TOPIC = "rentals.events-dlt";
    private static final String CATALOG_TOPIC = "catalog.events";
    private static final String EVENT_MACHINE_CREATED = "MACHINE_CREATED";

    @Mock
    private AuditTimelineRepository repository;

    @Test
    void processRentalEvent_ShouldValidateAndPersistJsonTimeline() {
        AuditService service = new AuditService(repository);
        when(repository.save(any(AuditTimeline.class))).thenAnswer(inv -> inv.getArgument(0));

        RentalEventDto dto = new RentalEventDto(
                "ev-1", "RENTAL_CREATED", 10L, 1L, "Excavadora",
                "u1", null, "SOLICITADO", "ms-smartrent-rentals", 1000L, "2026-11-20T00:00:00Z");

        AuditTimeline saved = service.processRentalEvent(dto, RENTALS_TOPIC, 1, 5L);

        assertEquals("RENTAL_CREATED", saved.getEventType());
        assertTrue(saved.getEventPayload().contains("\"rentalId\":10"));
        assertNotNull(saved.getTimestamp());

        dto.setEventType("  ");
        AuditTimeline defaultType = service.processRentalEvent(dto, RENTALS_TOPIC, 1, 6L);
        assertEquals("RENTAL_EVENT", defaultType.getEventType());
    }

    @Test
    void processRentalEvent_InvalidPayload_ShouldThrowIllegalArgument() {
        AuditService service = new AuditService(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.processRentalEvent(null, RENTALS_TOPIC, 0, 0L));

        RentalEventDto missingId = new RentalEventDto();
        missingId.setStatus("APROBADO");
        assertThrows(IllegalArgumentException.class,
                () -> service.processRentalEvent(missingId, RENTALS_TOPIC, 0, 0L));

        RentalEventDto missingStatus = new RentalEventDto();
        missingStatus.setRentalId(1L);
        missingStatus.setStatus(" ");
        assertThrows(IllegalArgumentException.class,
                () -> service.processRentalEvent(missingStatus, RENTALS_TOPIC, 0, 0L));

        RentalEventDto nullStatus = new RentalEventDto();
        nullStatus.setRentalId(1L);
        assertThrows(IllegalArgumentException.class,
                () -> service.processRentalEvent(nullStatus, RENTALS_TOPIC, 0, 0L));

        verifyNoInteractions(repository);
    }

    @Test
    void processCatalogEvent_ShouldValidatePersistAndFallbackSerialization() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        JsonProcessingException jsonEx = mock(JsonProcessingException.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(jsonEx);
        when(repository.save(any(AuditTimeline.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditService service = new AuditService(repository, failingMapper);
        CatalogEventDto dto = new CatalogEventDto(
                "cat-1", EVENT_MACHINE_CREATED, 3L, "Rodillo", "SN-3",
                "Compactación", 85000.0, true, "ms-smartrent-catalog", 2000L, "2026-11-20T00:00:00Z");

        AuditTimeline saved = service.processCatalogEvent(dto, CATALOG_TOPIC, 2, 10L);
        assertEquals(EVENT_MACHINE_CREATED, saved.getEventType());
        assertNotNull(saved.getEventPayload());

        assertThrows(IllegalArgumentException.class,
                () -> service.processCatalogEvent(null, CATALOG_TOPIC, 0, 0L));

        CatalogEventDto missingMachine = new CatalogEventDto();
        missingMachine.setEventType(EVENT_MACHINE_CREATED);
        assertThrows(IllegalArgumentException.class,
                () -> service.processCatalogEvent(missingMachine, CATALOG_TOPIC, 0, 0L));

        CatalogEventDto missingType = new CatalogEventDto();
        missingType.setMachineId(1L);
        missingType.setEventType(" ");
        assertThrows(IllegalArgumentException.class,
                () -> service.processCatalogEvent(missingType, CATALOG_TOPIC, 0, 0L));

        CatalogEventDto nullType = new CatalogEventDto();
        nullType.setMachineId(1L);
        assertThrows(IllegalArgumentException.class,
                () -> service.processCatalogEvent(nullType, CATALOG_TOPIC, 0, 0L));
    }

    @Test
    void recordDeadLetterEvent_ShouldPersistAndSwallowErrors() {
        AuditService service = new AuditService(repository, null);

        service.recordDeadLetterEvent(null, "   ", RENTALS_DLT_TOPIC, 0, 1L);
        service.recordDeadLetterEvent("RENTAL_EVENT_DLT", null, RENTALS_DLT_TOPIC, 0, 1L);
        verifyNoInteractions(repository);

        service.recordDeadLetterEvent("", "{\"bad\":true}", RENTALS_DLT_TOPIC, 0, 2L);
        ArgumentCaptor<AuditTimeline> captor = ArgumentCaptor.forClass(AuditTimeline.class);
        verify(repository).save(captor.capture());
        assertEquals("KAFKA_EVENT_DLT", captor.getValue().getEventType());

        when(repository.save(any(AuditTimeline.class))).thenThrow(new RuntimeException("DB error"));
        assertDoesNotThrow(() ->
                service.recordDeadLetterEvent("CATALOG_EVENT_DLT", "raw-error", "catalog.events-dlt", 1, 3L));
    }
}
