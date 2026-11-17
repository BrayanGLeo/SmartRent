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
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void publishAuditAndReportEvent_ShouldSendToKafkaWithKeyAndMachineName() {
        publisher.publishAuditAndReportEvent(1L, 1L, "user@test.com", "APROBADO");
        assertFalse(Mockito.mockingDetails(kafkaTemplate).getInvocations().isEmpty());
        verify(kafkaTemplate).send(
                Mockito.eq("rentals.events"),
                Mockito.eq("1"),
                Mockito.argThat(payload -> payload instanceof Map<?, ?> map
                        && "Excavadora".equals(map.get("machineName"))
                        && Long.valueOf(1L).equals(map.get("machineId")))
        );
    }

    @Test
    void publishEvents_WhenBrokersFail_ShouldSwallowExceptions() {
        doThrow(new RuntimeException("Rabbit down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        doThrow(new RuntimeException("Rabbit down"))
                .when(rabbitTemplate).convertAndSend(anyString(), any(Object.class));

        publisher.publishEmailEvent(1L, "user@test.com", "APROBADO");
        publisher.publishPrepEvent(1L, 2L);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka async fail")))
                .thenThrow(new RuntimeException("Kafka sync fail"));

        publisher.publishAuditAndReportEvent(1L, "user@test.com", "APROBADO");
        publisher.publishAuditAndReportEvent(3L, null, "user@test.com", "CANCELADO");
        publisher.publishAuditAndReportEvent(2L, 2L, "user@test.com", "DEVUELTO");
    }

    @Test
    void resolveMachineName_CoversAllCatalogIds() {
        assertEquals("Maquina_Generica", RentalEventPublisher.resolveMachineName(null));
        assertEquals("Excavadora", RentalEventPublisher.resolveMachineName(1L));
        assertEquals("Retroexcavadora", RentalEventPublisher.resolveMachineName(2L));
        assertEquals("Rodillo Compactador", RentalEventPublisher.resolveMachineName(3L));
        assertEquals("Martillo Demoledor", RentalEventPublisher.resolveMachineName(5L));
        assertEquals("Grua", RentalEventPublisher.resolveMachineName(7L));
        assertEquals("Grua", RentalEventPublisher.resolveMachineName(8L));
        assertEquals("Generador Industrial", RentalEventPublisher.resolveMachineName(9L));
        assertEquals("Generador Industrial", RentalEventPublisher.resolveMachineName(10L));
        assertEquals("Maquina_Generica", RentalEventPublisher.resolveMachineName(99L));
    }
}
