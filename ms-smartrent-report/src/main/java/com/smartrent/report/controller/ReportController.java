package com.smartrent.report.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartrent.report.service.ReportService;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKpis() {
        Map<String, Object> response = new HashMap<>();
        response.put("dailyRentals", reportService.getDailyRentals());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-services")
    public ResponseEntity<Object> getTopServices() {
        return ResponseEntity.ok(reportService.getTopServices());
    }
}
