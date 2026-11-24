package cl.duoc.mssmartrentcatalog.events;

import cl.duoc.mssmartrentcatalog.dto.CatalogEventDto;
import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CatalogEventPublisherTest {

    private static final String CATALOG_TOPIC = "catalog.events";
    private static final String EVENT_MACHINE_CREATED = "MACHINE_CREATED";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CatalogEventPublisher publisher;

    @Test
    void publishMachineEvent_ShouldSendTypedDtoWithMachineIdKey() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Maquinaria Pesada");

        Machine machine = new Machine();
        machine.setId(7L);
        machine.setName("Plataforma JLG 450AJ");
        machine.setSerialNumber("JLG-450-X");
        machine.setDailyPrice(95000.0);
        machine.setIsAvailable(true);
        machine.setCategory(category);

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(CATALOG_TOPIC, 1), 0L, 12, 0L, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(
                new ProducerRecord<>(CATALOG_TOPIC, "7", "ok"), metadata);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishMachineEvent(EVENT_MACHINE_CREATED, machine);

        verify(kafkaTemplate).send(
                eq(CATALOG_TOPIC),
                eq("7"),
                argThat(payload -> payload instanceof CatalogEventDto dto
                        && EVENT_MACHINE_CREATED.equals(dto.getEventType())
                        && Long.valueOf(7L).equals(dto.getMachineId())
                        && "Plataforma JLG 450AJ".equals(dto.getMachineName())
                        && "JLG-450-X".equals(dto.getSerialNumber())
                        && "Maquinaria Pesada".equals(dto.getCategoryName())
                        && Double.valueOf(95000.0).equals(dto.getDailyPrice())
                        && Boolean.TRUE.equals(dto.getAvailable())
                        && "ms-smartrent-catalog".equals(dto.getSourceService())
                        && dto.getEventId() != null)
        );
    }

    @Test
    void publishMachineEvent_ShouldHandleInvalidNullsAndDefaults() {
        publisher.publishMachineEvent(EVENT_MACHINE_CREATED, null);
        publisher.publishMachineEvent(EVENT_MACHINE_CREATED, new Machine());
        publisher.publishMachineDeleted(null);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());

        CatalogEventPublisher custom = new CatalogEventPublisher(kafkaTemplate, "custom.catalog");
        Machine m = new Machine();
        m.setId(9L);
        m.setName("Generador");
        custom.publishMachineEvent(" ", m);
        verify(kafkaTemplate).send(
                eq("custom.catalog"),
                eq("9"),
                argThat(payload -> payload instanceof CatalogEventDto dto
                        && "MACHINE_UPDATED".equals(dto.getEventType())
                        && dto.getCategoryName() == null)
        );
    }

    @Test
    void publishMachineDeleted_AndFailureBranches_ShouldSwallowExceptions() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.<SendResult<String, Object>>completedFuture(null))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Async broker failure")))
                .thenThrow(new RuntimeException("Sync broker failure"));

        publisher.publishMachineDeleted(5L);
        publisher.publishMachineDeleted(6L);
        publisher.publishMachineDeleted(7L);

        verify(kafkaTemplate, times(3)).send(
                eq(CATALOG_TOPIC),
                anyString(),
                argThat(payload -> payload instanceof CatalogEventDto dto
                        && "MACHINE_DELETED".equals(dto.getEventType())
                        && Boolean.FALSE.equals(dto.getAvailable()))
        );
    }
}
