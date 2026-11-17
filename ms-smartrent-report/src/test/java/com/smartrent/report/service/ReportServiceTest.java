package com.smartrent.report.service;


import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        reportService.recordRentalEvent(null);
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
}
