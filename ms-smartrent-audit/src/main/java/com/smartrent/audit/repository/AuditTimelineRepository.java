package com.smartrent.audit.repository;

import com.smartrent.audit.entity.AuditTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditTimelineRepository extends JpaRepository<AuditTimeline, Long> {
    List<AuditTimeline> findAllByOrderByTimestampDesc();
}
