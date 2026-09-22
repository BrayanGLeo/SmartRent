package com.smartrent.audit.controller;

import com.smartrent.audit.entity.AuditTimeline;
import com.smartrent.audit.repository.AuditTimelineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditTimelineRepository repository;

    @InjectMocks
    private AuditController auditController;

    @Test
    void getAuditHistory_returnsListOfAuditTimeline() {
        AuditTimeline audit = new AuditTimeline();
        audit.setId(1L);
        audit.setEventType("TEST_EVENT");
        audit.setEventPayload("test event");
        audit.setTimestamp(LocalDateTime.now());

        Mockito.when(repository.findAllByOrderByTimestampDesc()).thenReturn(List.of(audit));

        ResponseEntity<List<AuditTimeline>> response = auditController.getAuditHistory();

        assertEquals(200, response.getStatusCode().value());
        List<AuditTimeline> body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(1, body.size());
        assertEquals("test event", body.get(0).getEventPayload());
    }
}
