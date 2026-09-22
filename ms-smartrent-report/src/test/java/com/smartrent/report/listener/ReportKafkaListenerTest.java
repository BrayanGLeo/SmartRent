package com.smartrent.report.listener;

import com.smartrent.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportKafkaListenerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportKafkaListener reportKafkaListener;

    @Test
    void consumeEvent_delegatesToService() {
        String message = "test event excavadora";
        
        reportKafkaListener.consumeEvent(message);
        
        Mockito.verify(reportService, Mockito.times(1)).recordRentalEvent(message);
    }
}
