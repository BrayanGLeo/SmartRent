package com.smartrent.audit.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditTimelineTest {

    @Test
    void defaultConstructor_createsEmptyEntity() {
        AuditTimeline timeline = new AuditTimeline();
        assertNull(timeline.getId());
        assertNull(timeline.getEventType());
        assertNull(timeline.getEventPayload());
        assertNull(timeline.getTimestamp());
    }

    @Test
    void parameterizedConstructor_setsFields() {
        LocalDateTime now = LocalDateTime.now();
        AuditTimeline timeline = new AuditTimeline("TEST_EVENT", "Payload data", now);
        
        assertNull(timeline.getId());
        assertEquals("TEST_EVENT", timeline.getEventType());
        assertEquals("Payload data", timeline.getEventPayload());
        assertEquals(now, timeline.getTimestamp());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        AuditTimeline timeline = new AuditTimeline();
        LocalDateTime now = LocalDateTime.now();
        
        timeline.setId(10L);
        timeline.setEventType("UPDATE_EVENT");
        timeline.setEventPayload("Updated payload");
        timeline.setTimestamp(now);
        
        assertEquals(10L, timeline.getId());
        assertEquals("UPDATE_EVENT", timeline.getEventType());
        assertEquals("Updated payload", timeline.getEventPayload());
        assertEquals(now, timeline.getTimestamp());
    }
}
