package com.smartrent.notify.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    void processEmail_executesWithoutErrors() {
        String message = "test email payload";
        assertDoesNotThrow(() -> notificationListener.processEmail(message));
    }

    @Test
    void processPrep_executesWithoutErrors() {
        String message = "test prep payload";
        assertDoesNotThrow(() -> notificationListener.processPrep(message));
    }
}
