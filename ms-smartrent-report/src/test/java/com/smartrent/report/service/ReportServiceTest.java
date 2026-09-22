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
}
