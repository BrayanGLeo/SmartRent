package cl.duoc.mssmartrentrentals.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RentalEventDtoTest {

    @Test
    void gettersAndSetters_ShouldStoreAllFields() {
        RentalEventDto dto = new RentalEventDto();
        assertNull(dto.getEventId());

        dto.setEventId("ev-1");
        dto.setEventType("RENTAL_CREATED");
        dto.setRentalId(10L);
        dto.setMachineId(2L);
        dto.setMachineName("Retroexcavadora");
        dto.setUserId("user@smartrent.cl");
        dto.setPreviousStatus("SOLICITADO");
        dto.setStatus("APROBADO");
        dto.setSourceService("ms-smartrent-rentals");
        dto.setTimestamp(1700000000L);
        dto.setOccurredAt("2026-11-20T00:00:00Z");

        assertEquals("ev-1", dto.getEventId());
        assertEquals("RENTAL_CREATED", dto.getEventType());
        assertEquals(10L, dto.getRentalId());
        assertEquals(2L, dto.getMachineId());
        assertEquals("Retroexcavadora", dto.getMachineName());
        assertEquals("user@smartrent.cl", dto.getUserId());
        assertEquals("SOLICITADO", dto.getPreviousStatus());
        assertEquals("APROBADO", dto.getStatus());
        assertEquals("ms-smartrent-rentals", dto.getSourceService());
        assertEquals(1700000000L, dto.getTimestamp());
        assertEquals("2026-11-20T00:00:00Z", dto.getOccurredAt());

        RentalEventDto full = new RentalEventDto(
                "ev-2", "RENTAL_STATUS_CHANGED", 11L, 3L, "Rodillo Compactador",
                "u2", "APROBADO", "EN_PREPARACION", "ms-smartrent-rentals", 123L, "2026-11-20T00:01:00Z");
        assertEquals("ev-2", full.getEventId());
        assertEquals(11L, full.getRentalId());
    }
}
