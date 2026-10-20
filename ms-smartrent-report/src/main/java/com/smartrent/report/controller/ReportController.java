package com.smartrent.report.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartrent.report.service.ReportService;
import java.util.List;

@SuppressWarnings("java:S5122")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKpis() {
        Map<String, Object> response = new HashMap<>();
        int count = reportService.getDailyRentals();
        response.put("dailyRentals", count);
        response.put("rentalsPerDay", List.of(count, 5, 8, 12, 15, 10, 7));
        response.put("labels", List.of("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-services")
    public ResponseEntity<Object> getTopServices() {
        return ResponseEntity.ok(reportService.getTopServices());
    }
}
