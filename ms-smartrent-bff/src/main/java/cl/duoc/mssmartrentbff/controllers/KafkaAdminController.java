package cl.duoc.mssmartrentbff.controllers;

import cl.duoc.mssmartrentbff.dto.KafkaTopicConfigUpdateDto;
import cl.duoc.mssmartrentbff.dto.KafkaTopicRequestDto;
import cl.duoc.mssmartrentbff.services.KafkaAdminService;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Administrador')")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    @GetMapping({"/api/kafka/cluster", "/v1/api/kafka/cluster", "/v2/api/kafka/cluster"})
    public ResponseEntity<Object> getClusterInfo() {
        return executeKafkaCall(() -> ResponseEntity.ok(kafkaAdminService.getClusterInfo()));
    }

    @GetMapping({"/api/kafka/topics", "/v1/api/kafka/topics", "/v2/api/kafka/topics"})
    public ResponseEntity<Object> listTopics(@RequestParam(defaultValue = "false") boolean includeInternal) {
        return executeKafkaCall(() -> ResponseEntity.ok(kafkaAdminService.listTopics(includeInternal)));
    }

    @GetMapping({"/api/kafka/topics/{name}", "/v1/api/kafka/topics/{name}", "/v2/api/kafka/topics/{name}"})
    public ResponseEntity<Object> describeTopic(@PathVariable String name) {
        return executeKafkaCall(() -> ResponseEntity.ok(kafkaAdminService.describeTopic(name)));
    }

    @PostMapping({"/api/kafka/topics", "/v1/api/kafka/topics", "/v2/api/kafka/topics"})
    public ResponseEntity<Object> createTopic(@RequestBody(required = false) KafkaTopicRequestDto request) {
        return executeKafkaCall(() -> ResponseEntity.status(HttpStatus.CREATED).body(kafkaAdminService.createTopic(request)));
    }

    @PutMapping({"/api/kafka/topics/{name}/config", "/v1/api/kafka/topics/{name}/config", "/v2/api/kafka/topics/{name}/config"})
    public ResponseEntity<Object> updateTopicConfig(
            @PathVariable String name,
            @RequestBody(required = false) KafkaTopicConfigUpdateDto request) {
        return executeKafkaCall(() -> ResponseEntity.ok(kafkaAdminService.updateTopicConfig(name, request)));
    }

    @DeleteMapping({"/api/kafka/topics/{name}", "/v1/api/kafka/topics/{name}", "/v2/api/kafka/topics/{name}"})
    public ResponseEntity<Object> deleteTopic(@PathVariable String name) {
        return executeKafkaCall(() -> {
            kafkaAdminService.deleteTopic(name);
            return ResponseEntity.noContent().build();
        });
    }

    @GetMapping({"/api/kafka/consumer-groups", "/v1/api/kafka/consumer-groups", "/v2/api/kafka/consumer-groups"})
    public ResponseEntity<Object> listConsumerGroups() {
        return executeKafkaCall(() -> ResponseEntity.ok(kafkaAdminService.listConsumerGroups()));
    }

    @GetMapping({"/api/kafka/consumer-groups/{groupId}", "/v1/api/kafka/consumer-groups/{groupId}", "/v2/api/kafka/consumer-groups/{groupId}"})
    public ResponseEntity<Object> describeConsumerGroup(@PathVariable String groupId) {
        return executeKafkaCall(() -> ResponseEntity.ok(kafkaAdminService.describeConsumerGroup(groupId)));
    }

    @DeleteMapping({"/api/kafka/consumer-groups/{groupId}", "/v1/api/kafka/consumer-groups/{groupId}", "/v2/api/kafka/consumer-groups/{groupId}"})
    public ResponseEntity<Object> deleteConsumerGroup(@PathVariable String groupId) {
        return executeKafkaCall(() -> {
            kafkaAdminService.deleteConsumerGroup(groupId);
            return ResponseEntity.noContent().build();
        });
    }

    private ResponseEntity<Object> executeKafkaCall(Supplier<ResponseEntity<Object>> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex));
        } catch (UnknownTopicOrPartitionException | GroupIdNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex));
        } catch (TopicExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(ex));
        }
    }

    private static Map<String, String> errorBody(Exception ex) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : ex.getClass().getSimpleName();
        return Map.of("error", message);
    }
}
