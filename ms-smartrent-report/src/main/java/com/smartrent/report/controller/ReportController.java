package com.smartrent.report.controller;

import com.smartrent.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S5122")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String REPORT_ROLES_AUTH =
            "hasAnyAuthority('ROLE_ADMIN', 'Administrador', 'ROLE_JEFEBODEGA', 'JefeBodega', 'ROLE_AUDITOR', 'Auditor')";

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/kpis")
    @PreAuthorize(REPORT_ROLES_AUTH)
    public ResponseEntity<Map<String, Object>> getKpis() {
        Map<String, Object> response = new HashMap<>();
        int count = reportService.getDailyRentals();
        response.put("dailyRentals", count);
        response.put("rentalsPerDay", List.of(count, 5, 8, 12, 15, 10, 7));
        response.put("labels", List.of("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"));
        response.put("rentalsByStatus", reportService.getRentalsByStatus());
        response.put("catalogSummary", reportService.getCatalogActivitySummary());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-services")
    @PreAuthorize(REPORT_ROLES_AUTH)
    public ResponseEntity<Object> getTopServices() {
        return ResponseEntity.ok(reportService.getTopServices());
    }

    @GetMapping("/catalog-activity")
    @PreAuthorize(REPORT_ROLES_AUTH)
    public ResponseEntity<Map<String, Object>> getCatalogActivity() {
        return ResponseEntity.ok(reportService.getCatalogActivitySummary());
    }
}
