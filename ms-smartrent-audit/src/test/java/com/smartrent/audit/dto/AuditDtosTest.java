package com.smartrent.audit.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditDtosTest {

    @Test
    void rentalAndCatalogEventDtos_ShouldStoreAllFields() {
        RentalEventDto rental = new RentalEventDto();
        assertNull(rental.getEventId());
        rental.setEventId("e1");
        rental.setEventType("RENTAL_CREATED");
        rental.setRentalId(10L);
        rental.setMachineId(1L);
        rental.setMachineName("Excavadora");
        rental.setUserId("u1");
        rental.setPreviousStatus("NONE");
        rental.setStatus("SOLICITADO");
        rental.setSourceService("ms-smartrent-rentals");
        rental.setTimestamp(100L);
        rental.setOccurredAt("2026-11-20T00:00:00Z");

        assertEquals("e1", rental.getEventId());
        assertEquals("RENTAL_CREATED", rental.getEventType());
        assertEquals(10L, rental.getRentalId());
        assertEquals(1L, rental.getMachineId());
        assertEquals("Excavadora", rental.getMachineName());
        assertEquals("u1", rental.getUserId());
        assertEquals("NONE", rental.getPreviousStatus());
        assertEquals("SOLICITADO", rental.getStatus());
        assertEquals("ms-smartrent-rentals", rental.getSourceService());
        assertEquals(100L, rental.getTimestamp());
        assertEquals("2026-11-20T00:00:00Z", rental.getOccurredAt());

        CatalogEventDto catalog = new CatalogEventDto();
        assertNull(catalog.getEventId());
        catalog.setEventId("c1");
        catalog.setEventType("MACHINE_CREATED");
        catalog.setMachineId(2L);
        catalog.setMachineName("Retroexcavadora");
        catalog.setSerialNumber("JD-310");
        catalog.setCategoryName("Pesada");
        catalog.setDailyPrice(120000.0);
        catalog.setAvailable(true);
        catalog.setSourceService("ms-smartrent-catalog");
        catalog.setTimestamp(200L);
        catalog.setOccurredAt("2026-11-20T00:00:01Z");

        assertEquals("c1", catalog.getEventId());
        assertEquals("MACHINE_CREATED", catalog.getEventType());
        assertEquals(2L, catalog.getMachineId());
        assertEquals("Retroexcavadora", catalog.getMachineName());
        assertEquals("JD-310", catalog.getSerialNumber());
        assertEquals("Pesada", catalog.getCategoryName());
        assertEquals(120000.0, catalog.getDailyPrice());
        assertTrue(catalog.getAvailable());
        assertEquals("ms-smartrent-catalog", catalog.getSourceService());
        assertEquals(200L, catalog.getTimestamp());
        assertEquals("2026-11-20T00:00:01Z", catalog.getOccurredAt());
    }
}
