package com.smartrent.report.controller;

import com.smartrent.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void getKpis_returnsDailyRentalsAndSummaries() {
        when(reportService.getDailyRentals()).thenReturn(10);
        when(reportService.getRentalsByStatus()).thenReturn(Map.of("SOLICITADO", 4, "APROBADO", 6));
        when(reportService.getCatalogActivitySummary()).thenReturn(Map.of("totalEvents", 3));

        ResponseEntity<Map<String, Object>> response = reportController.getKpis();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(10, body.get("dailyRentals"));
        assertEquals(Map.of("SOLICITADO", 4, "APROBADO", 6), body.get("rentalsByStatus"));
        assertEquals(Map.of("totalEvents", 3), body.get("catalogSummary"));
    }

    @Test
    void getTopServices_returnsList() {
        when(reportService.getTopServices()).thenReturn(List.of(
                Map.entry("Excavadora", 5)
        ));

        ResponseEntity<Object> response = reportController.getTopServices();

        assertEquals(200, response.getStatusCode().value());
        java.util.Objects.requireNonNull(response.getBody());
    }

    @Test
    void getCatalogActivity_returnsSummary() {
        when(reportService.getCatalogActivitySummary()).thenReturn(Map.of("totalEvents", 7, "trackedMachines", 4));

        ResponseEntity<Map<String, Object>> response = reportController.getCatalogActivity();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(7, body.get("totalEvents"));
    }
}
