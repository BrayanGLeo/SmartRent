package cl.duoc.mssmartrentbff.controllers;

import cl.duoc.mssmartrentbff.dto.KafkaTopicConfigUpdateDto;
import cl.duoc.mssmartrentbff.dto.KafkaTopicRequestDto;
import cl.duoc.mssmartrentbff.services.KafkaAdminService;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdminControllerTest {

    @Mock
    private KafkaAdminService kafkaAdminService;

    @InjectMocks
    private KafkaAdminController controller;

    @Test
    void endpoints_ShouldReturnSuccessResponses() {
        when(kafkaAdminService.getClusterInfo()).thenReturn(Map.of("brokerCount", 3));
        assertEquals(HttpStatus.OK, controller.getClusterInfo().getStatusCode());

        when(kafkaAdminService.listTopics(false)).thenReturn(List.of(Map.of("name", "rentals.events")));
        assertEquals(HttpStatus.OK, controller.listTopics(false).getStatusCode());

        when(kafkaAdminService.describeTopic("rentals.events")).thenReturn(Map.of("partitions", 3));
        assertEquals(HttpStatus.OK, controller.describeTopic("rentals.events").getStatusCode());

        KafkaTopicRequestDto req = new KafkaTopicRequestDto("test.topic", 3, (short) 3, null);
        when(kafkaAdminService.createTopic(req)).thenReturn(Map.of("name", "test.topic"));
        assertEquals(HttpStatus.CREATED, controller.createTopic(req).getStatusCode());

        KafkaTopicConfigUpdateDto updateDto = new KafkaTopicConfigUpdateDto(3, Map.of("retention.ms", "86400000"));
        when(kafkaAdminService.updateTopicConfig("test.topic", updateDto)).thenReturn(Map.of("name", "test.topic"));
        assertEquals(HttpStatus.OK, controller.updateTopicConfig("test.topic", updateDto).getStatusCode());

        doNothing().when(kafkaAdminService).deleteTopic("test.topic");
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteTopic("test.topic").getStatusCode());

        when(kafkaAdminService.listConsumerGroups()).thenReturn(List.of(Map.of("groupId", "audit-group")));
        assertEquals(HttpStatus.OK, controller.listConsumerGroups().getStatusCode());

        when(kafkaAdminService.describeConsumerGroup("audit-group")).thenReturn(Map.of("totalLag", 0L));
        assertEquals(HttpStatus.OK, controller.describeConsumerGroup("audit-group").getStatusCode());

        doNothing().when(kafkaAdminService).deleteConsumerGroup("old-group");
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteConsumerGroup("old-group").getStatusCode());
    }

    @Test
    void endpoints_ShouldMapErrorsToAppropriateHttpStatuses() {
        doThrow(new IllegalArgumentException("Nombre inválido")).when(kafkaAdminService).createTopic(any());
        assertEquals(HttpStatus.BAD_REQUEST, controller.createTopic(new KafkaTopicRequestDto()).getStatusCode());

        doThrow(new UnknownTopicOrPartitionException("Not found")).when(kafkaAdminService).describeTopic("missing");
        assertEquals(HttpStatus.NOT_FOUND, controller.describeTopic("missing").getStatusCode());

        doThrow(new GroupIdNotFoundException("No group")).when(kafkaAdminService).describeConsumerGroup("missing-cg");
        assertEquals(HttpStatus.NOT_FOUND, controller.describeConsumerGroup("missing-cg").getStatusCode());

        doThrow(new TopicExistsException("Already exists")).when(kafkaAdminService).createTopic(any());
        assertEquals(HttpStatus.CONFLICT, controller.createTopic(new KafkaTopicRequestDto()).getStatusCode());

        doThrow(new RuntimeException(" ")).when(kafkaAdminService).getClusterInfo();
        ResponseEntity<Object> err503 = controller.getClusterInfo();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, err503.getStatusCode());

        doThrow(new RuntimeException()).when(kafkaAdminService).getClusterInfo();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller.getClusterInfo().getStatusCode());
    }
}
