package cl.duoc.mssmartrentrentals.events;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RentalEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RentalEventPublisher publisher;

    @Test
    void publishEmailEvent_ShouldSendToRabbit() {
        publisher.publishEmailEvent(1L, "user@test.com", "APROBADO");
        verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), any(Map.class));
    }

    @Test
    void publishPrepEvent_ShouldSendToRabbit() {
        publisher.publishPrepEvent(1L, 100L);
        verify(rabbitTemplate).convertAndSend(eq("q.cmd.prep"), any(Map.class));
    }

    @Test
    void publishAuditAndReportEvent_ShouldSendToKafka() {
        publisher.publishAuditAndReportEvent(1L, "user@test.com", "APROBADO");
        verify(kafkaTemplate).send(eq("rentals.events"), any(Map.class));
    }
}
