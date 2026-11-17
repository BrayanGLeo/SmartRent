package com.smartrent.audit.listener;

import com.smartrent.audit.entity.AuditTimeline;
import com.smartrent.audit.repository.AuditTimelineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AuditKafkaListenerTest {

    @Mock
    private AuditTimelineRepository repository;

    @InjectMocks
    private AuditKafkaListener auditKafkaListener;

    @Test
    @SuppressWarnings("null")
    void consumeEvent_savesAuditTimeline() {
        String message = "Rental accepted event";

        auditKafkaListener.consumeEvent(message);

        ArgumentCaptor<AuditTimeline> captor = ArgumentCaptor.forClass(AuditTimeline.class);
        Mockito.verify(repository, Mockito.times(1)).save(captor.capture());

        AuditTimeline savedEntity = captor.getValue();
        assertEquals(message, savedEntity.getEventPayload());
        assertNotNull(savedEntity.getTimestamp());
    }

    @Test
    void consumeEvent_ignoresBlankMessage() {
        auditKafkaListener.consumeEvent("   ");
        auditKafkaListener.consumeEvent(null);
        Mockito.verifyNoInteractions(repository);
    }

    @Test
    @SuppressWarnings("null")
    void handleDlt_savesDltTimelineAndSwallowsPersistenceErrors() {
        auditKafkaListener.handleDlt("failed payload");

        ArgumentCaptor<AuditTimeline> captor = ArgumentCaptor.forClass(AuditTimeline.class);
        Mockito.verify(repository, Mockito.times(1)).save(captor.capture());
        assertEquals("RENTAL_EVENT_DLT", captor.getValue().getEventType());
        assertEquals("failed payload", captor.getValue().getEventPayload());

        Mockito.when(repository.save(Mockito.any(AuditTimeline.class)))
                .thenThrow(new RuntimeException("DB down"));
        auditKafkaListener.handleDlt("another payload");

        Mockito.reset(repository);
        auditKafkaListener.handleDlt(null);
        auditKafkaListener.handleDlt("   ");
        Mockito.verifyNoInteractions(repository);
    }
}
