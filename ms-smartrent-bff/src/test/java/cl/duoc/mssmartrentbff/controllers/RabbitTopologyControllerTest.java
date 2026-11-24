package cl.duoc.mssmartrentbff.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RabbitTopologyControllerTest {

    private final RabbitTopologyController controller = new RabbitTopologyController();

    @Test
    void queuesCrud_ShouldCreateAndDeleteQueue() {
        ResponseEntity<List<Map<String, Object>>> initial = controller.getQueues();
        List<Map<String, Object>> initialBody = Objects.requireNonNull(initial.getBody());
        int initialCount = initialBody.size();

        Map<String, Object> newQueue = Map.of("name", "q.cmd.test", "type", "standard", "durable", true);
        controller.createQueue(newQueue);
        assertEquals(initialCount + 1, Objects.requireNonNull(controller.getQueues().getBody()).size());

        controller.deleteQueue("q.cmd.test");
        assertEquals(initialCount, Objects.requireNonNull(controller.getQueues().getBody()).size());
    }

    @Test
    void exchangesAndBindingsCrud_ShouldCreateAndDelete() {
        int initialExchanges = Objects.requireNonNull(controller.getExchanges().getBody()).size();
        controller.createExchange(Map.of("name", "cmd.custom", "type", "topic"));
        assertEquals(initialExchanges + 1, Objects.requireNonNull(controller.getExchanges().getBody()).size());

        int initialBindings = Objects.requireNonNull(controller.getBindings().getBody()).size();
        controller.createBinding(Map.of("id", "b-custom", "source", "cmd.custom", "destination", "q.cmd.email", "routingKey", "email.#"));
        assertEquals(initialBindings + 1, Objects.requireNonNull(controller.getBindings().getBody()).size());

        controller.deleteBinding(Map.of("id", "b-custom"));
        assertEquals(initialBindings, Objects.requireNonNull(controller.getBindings().getBody()).size());

        controller.deleteExchange("cmd.custom");
        assertEquals(initialExchanges, Objects.requireNonNull(controller.getExchanges().getBody()).size());
    }

    @Test
    void nullInputsAndAttributeBasedDeleteBinding_ShouldBeHandledSafely() {
        assertTrue(Objects.requireNonNull(controller.createQueue(null).getBody()).isEmpty());
        assertTrue(Objects.requireNonNull(controller.createExchange(null).getBody()).isEmpty());
        assertTrue(Objects.requireNonNull(controller.createBinding(null).getBody()).isEmpty());
        assertEquals(204, controller.deleteBinding(null).getStatusCode().value());

        int before = Objects.requireNonNull(controller.getBindings().getBody()).size();
        controller.deleteBinding(Map.of(
                "source", "cmd.direct",
                "destination", "q.cmd.email",
                "routingKey", "email.send"
        ));
        assertEquals(before - 1, Objects.requireNonNull(controller.getBindings().getBody()).size());
    }
}
