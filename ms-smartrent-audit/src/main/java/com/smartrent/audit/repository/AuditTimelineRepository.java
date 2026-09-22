package com.smartrent.audit.repository;

import com.smartrent.audit.entity.AuditTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditTimelineRepository extends JpaRepository<AuditTimeline, Long> {
    List<AuditTimeline> findAllByOrderByTimestampDesc();
}
