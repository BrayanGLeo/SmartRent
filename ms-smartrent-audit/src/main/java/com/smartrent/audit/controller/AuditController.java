package com.smartrent.audit.controller;

import com.smartrent.audit.entity.AuditTimeline;
import com.smartrent.audit.repository.AuditTimelineRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditTimelineRepository repository;

    public AuditController(AuditTimelineRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<AuditTimeline>> getAuditHistory() {
        return ResponseEntity.ok(repository.findAllByOrderByTimestampDesc());
    }
}
