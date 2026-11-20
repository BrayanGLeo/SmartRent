package com.smartrent.report.service;

import com.smartrent.report.dto.CatalogEventDto;
import com.smartrent.report.dto.RentalEventDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    private final ReportService reportService = new ReportService();

    @Test
    void recordRentalEvent_incrementsDailyRentals() {
        reportService.recordRentalEvent("mensaje generico");
        reportService.recordRentalEvent("otro mensaje");

        assertEquals(2, reportService.getDailyRentals());
    }

    @Test
    void recordRentalEvent_extractsMachineName() {
        reportService.recordRentalEvent("arriendo de excavadora realizado");
        reportService.recordRentalEvent("se necesita una excavadora");
        reportService.recordRentalEvent("solicitud de grua");

        List<Map.Entry<String, Integer>> topServices = reportService.getTopServices();

        assertEquals(2, topServices.size());
        assertEquals("Excavadora", topServices.get(0).getKey());
        assertEquals(2, topServices.get(0).getValue());
        assertEquals("Grua", topServices.get(1).getKey());
        assertEquals(1, topServices.get(1).getValue());
    }

    @Test
    void recordRentalEvent_extractsExplicitMachineNameFromJsonAndRetroexcavadora() {
        reportService.recordRentalEvent("{\"rentalId\":20,\"machineId\":2,\"machineName\":\"Retroexcavadora\"}");
        reportService.recordRentalEvent("{\"rentalId\":21,\"machineId\":99,\"machineName\":\"Camion Pluma\"}");

        List<Map.Entry<String, Integer>> topServices = reportService.getTopServices();

        assertEquals(2, reportService.getDailyRentals());
        assertEquals(2, topServices.size());
    }

    @Test
    void recordRentalEvent_coversAllMachinePatternsAndNull() {
        reportService.recordRentalEvent((String) null);
        reportService.recordRentalEvent("rodillo compactador");
        reportService.recordRentalEvent("generador");
        reportService.recordRentalEvent("martillo demoledor");
        reportService.recordRentalEvent("grúa torre");
        reportService.recordRentalEvent("{\"machineId\":1}");
        reportService.recordRentalEvent("{\"rentalId\":1}");
        reportService.recordRentalEvent("{\"machineId\":3}");
        reportService.recordRentalEvent("{\"machineId\":5}");
        reportService.recordRentalEvent("{\"machineId\":7}");
        reportService.recordRentalEvent("{\"machineId\":8}");
        reportService.recordRentalEvent("{\"machineId\":9}");
        reportService.recordRentalEvent("{\"machineId\":10}");
        reportService.recordRentalEvent("{\"machineName\":\"Maquina_Generica\"}");
        reportService.recordRentalEvent("{\"machineName\":\"   \"}");

        assertEquals(15, reportService.getDailyRentals());
        assertEquals(5, reportService.getTopServices().size());
    }

    @Test
    void recordRentalEventDto_tracksStatusAndMachineFallbacksAndValidates() {
        assertThrows(IllegalArgumentException.class, () -> reportService.recordRentalEvent((RentalEventDto) null));
        RentalEventDto noId = new RentalEventDto();
        noId.setStatus("APROBADO");
        assertThrows(IllegalArgumentException.class, () -> reportService.recordRentalEvent(noId));

        RentalEventDto blankStatus = new RentalEventDto();
        blankStatus.setRentalId(1L);
        blankStatus.setStatus(" ");
        assertThrows(IllegalArgumentException.class, () -> reportService.recordRentalEvent(blankStatus));

        RentalEventDto nullStatus = new RentalEventDto();
        nullStatus.setRentalId(1L);
        assertThrows(IllegalArgumentException.class, () -> reportService.recordRentalEvent(nullStatus));

        reportService.recordRentalEvent(new RentalEventDto(
                "e1", "RENTAL_CREATED", 1L, 1L, "Excavadora", "u1", null, "solicitado", "ms-smartrent-rentals", 10L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e2", "RENTAL_STATUS_CHANGED", 2L, 2L, "Maquina_Generica", "u1", "SOLICITADO", "APROBADO", "ms-smartrent-rentals", 11L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e3", "RENTAL_STATUS_CHANGED", 3L, 3L, null, "u1", "APROBADO", "APROBADO", "ms-smartrent-rentals", 12L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e4", "RENTAL_STATUS_CHANGED", 4L, 5L, " ", "u1", "APROBADO", "DEVUELTO", "ms-smartrent-rentals", 13L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e5", "RENTAL_STATUS_CHANGED", 5L, 7L, null, "u1", "SOLICITADO", "EN_TERRENO", "ms-smartrent-rentals", 14L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e6", "RENTAL_STATUS_CHANGED", 6L, 9L, null, "u1", "SOLICITADO", "CANCELADO", "ms-smartrent-rentals", 15L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e7", "RENTAL_STATUS_CHANGED", 7L, 99L, null, "u1", "SOLICITADO", "EN_PREPARACION", "ms-smartrent-rentals", 16L, "t"));
        reportService.recordRentalEvent(new RentalEventDto(
                "e8", "RENTAL_STATUS_CHANGED", 8L, null, null, "u1", "SOLICITADO", "SOLICITADO", "ms-smartrent-rentals", 17L, "t"));

        assertEquals(8, reportService.getDailyRentals());
        Map<String, Integer> byStatus = reportService.getRentalsByStatus();
        assertEquals(2, byStatus.get("SOLICITADO"));
        assertEquals(2, byStatus.get("APROBADO"));
        assertEquals(1, byStatus.get("DEVUELTO"));
    }

    @Test
    void recordCatalogEvent_tracksActivityAvailabilityAndRecentCap() {
        assertThrows(IllegalArgumentException.class, () -> reportService.recordCatalogEvent(null));

        CatalogEventDto missingMachine = new CatalogEventDto();
        missingMachine.setEventType("MACHINE_CREATED");
        assertThrows(IllegalArgumentException.class, () -> reportService.recordCatalogEvent(missingMachine));

        CatalogEventDto blankType = new CatalogEventDto();
        blankType.setMachineId(1L);
        blankType.setEventType(" ");
        assertThrows(IllegalArgumentException.class, () -> reportService.recordCatalogEvent(blankType));

        CatalogEventDto nullType = new CatalogEventDto();
        nullType.setMachineId(1L);
        assertThrows(IllegalArgumentException.class, () -> reportService.recordCatalogEvent(nullType));

        for (long i = 1; i <= 22; i++) {
            reportService.recordCatalogEvent(new CatalogEventDto(
                    "c-" + i,
                    i == 22 ? "MACHINE_DELETED" : "MACHINE_CREATED",
                    i,
                    "Equipo " + i,
                    "SN-" + i,
                    "Pesada",
                    100000.0,
                    true,
                    "ms-smartrent-catalog",
                    1000L + i,
                    "2026-11-20T00:00:00Z"
            ));
        }

        Map<String, Object> summary = reportService.getCatalogActivitySummary();
        assertEquals(22, summary.get("totalEvents"));
        assertEquals(21L, summary.get("availableMachinesCount"));
        assertEquals(22, summary.get("trackedMachines"));
        @SuppressWarnings("unchecked")
        List<CatalogEventDto> recent = (List<CatalogEventDto>) summary.get("recentEvents");
        assertEquals(20, recent.size());
        assertTrue(recent.get(0).getMachineId() == 22L);
    }
}
