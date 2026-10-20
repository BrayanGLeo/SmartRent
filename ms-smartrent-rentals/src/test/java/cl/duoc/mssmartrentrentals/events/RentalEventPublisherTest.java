package cl.duoc.mssmartrentrentals.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
        Map<String, Object> expected = Map.of(
                "rentalId", 1L,
                "userEmail", "user@test.com",
                "status", "APROBADO",
                "action", "email.send"
        );
        verify(rabbitTemplate).convertAndSend("cmd.direct", "email.send", (Object) expected);
    }

    @Test
    void publishPrepEvent_ShouldSendToRabbit() {
        publisher.publishPrepEvent(1L, 100L);
        Map<String, Object> expected = Map.of(
                "rentalId", 1L,
                "machineId", 100L,
                "action", "print.ticket"
        );
        verify(rabbitTemplate).convertAndSend("q.cmd.prep", (Object) expected);
    }

    @Test
    void publishAuditAndReportEvent_ShouldSendToKafka() {
        publisher.publishAuditAndReportEvent(1L, "user@test.com", "APROBADO");
        assertFalse(Mockito.mockingDetails(kafkaTemplate).getInvocations().isEmpty());
    }
}
