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
}
