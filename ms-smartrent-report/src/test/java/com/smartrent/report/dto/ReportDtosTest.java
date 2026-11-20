package com.smartrent.report.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDtosTest {

    @Test
    void rentalAndCatalogDtos_ShouldStoreAllFields() {
        RentalEventDto rental = new RentalEventDto();
        assertNull(rental.getEventId());
        rental.setEventId("r1");
        rental.setEventType("RENTAL_CREATED");
        rental.setRentalId(1L);
        rental.setMachineId(2L);
        rental.setMachineName("Excavadora");
        rental.setUserId("u1");
        rental.setPreviousStatus("NONE");
        rental.setStatus("SOLICITADO");
        rental.setSourceService("ms-smartrent-rentals");
        rental.setTimestamp(10L);
        rental.setOccurredAt("2026-11-20T00:00:00Z");

        assertEquals("r1", rental.getEventId());
        assertEquals("RENTAL_CREATED", rental.getEventType());
        assertEquals(1L, rental.getRentalId());
        assertEquals(2L, rental.getMachineId());
        assertEquals("Excavadora", rental.getMachineName());
        assertEquals("u1", rental.getUserId());
        assertEquals("NONE", rental.getPreviousStatus());
        assertEquals("SOLICITADO", rental.getStatus());
        assertEquals("ms-smartrent-rentals", rental.getSourceService());
        assertEquals(10L, rental.getTimestamp());
        assertEquals("2026-11-20T00:00:00Z", rental.getOccurredAt());

        CatalogEventDto catalog = new CatalogEventDto();
        assertNull(catalog.getEventId());
        catalog.setEventId("c1");
        catalog.setEventType("MACHINE_CREATED");
        catalog.setMachineId(3L);
        catalog.setMachineName("Rodillo");
        catalog.setSerialNumber("SN-3");
        catalog.setCategoryName("Compactación");
        catalog.setDailyPrice(85000.0);
        catalog.setAvailable(true);
        catalog.setSourceService("ms-smartrent-catalog");
        catalog.setTimestamp(20L);
        catalog.setOccurredAt("2026-11-20T00:00:01Z");

        assertEquals("c1", catalog.getEventId());
        assertEquals("MACHINE_CREATED", catalog.getEventType());
        assertEquals(3L, catalog.getMachineId());
        assertEquals("Rodillo", catalog.getMachineName());
        assertEquals("SN-3", catalog.getSerialNumber());
        assertEquals("Compactación", catalog.getCategoryName());
        assertEquals(85000.0, catalog.getDailyPrice());
        assertTrue(catalog.getAvailable());
        assertEquals("ms-smartrent-catalog", catalog.getSourceService());
        assertEquals(20L, catalog.getTimestamp());
        assertEquals("2026-11-20T00:00:01Z", catalog.getOccurredAt());
    }
}
