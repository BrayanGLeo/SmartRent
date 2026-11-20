package cl.duoc.mssmartrentcatalog.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogEventDtoTest {

    @Test
    void gettersAndSetters_ShouldStoreAllFields() {
        CatalogEventDto dto = new CatalogEventDto();
        assertNull(dto.getEventId());

        dto.setEventId("cat-1");
        dto.setEventType("MACHINE_CREATED");
        dto.setMachineId(4L);
        dto.setMachineName("Taladro Makita");
        dto.setSerialNumber("MAK-01");
        dto.setCategoryName("Herramientas");
        dto.setDailyPrice(15000.0);
        dto.setAvailable(true);
        dto.setSourceService("ms-smartrent-catalog");
        dto.setTimestamp(1700000001L);
        dto.setOccurredAt("2026-11-20T00:00:00Z");

        assertEquals("cat-1", dto.getEventId());
        assertEquals("MACHINE_CREATED", dto.getEventType());
        assertEquals(4L, dto.getMachineId());
        assertEquals("Taladro Makita", dto.getMachineName());
        assertEquals("MAK-01", dto.getSerialNumber());
        assertEquals("Herramientas", dto.getCategoryName());
        assertEquals(15000.0, dto.getDailyPrice());
        assertTrue(dto.getAvailable());
        assertEquals("ms-smartrent-catalog", dto.getSourceService());
        assertEquals(1700000001L, dto.getTimestamp());
        assertEquals("2026-11-20T00:00:00Z", dto.getOccurredAt());
    }
}
