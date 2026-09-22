package com.smartrent.report.controller;

import com.smartrent.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void getKpis_returnsDailyRentals() {
        when(reportService.getDailyRentals()).thenReturn(10);

        ResponseEntity<Map<String, Object>> response = reportController.getKpis();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(10, body.get("dailyRentals"));
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
}
