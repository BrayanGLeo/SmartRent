package cl.duoc.mssmartrentbff.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RabbitTopologyControllerTest {

    private final RabbitTopologyController controller = new RabbitTopologyController();

    @Test
    void queuesCrud_ShouldCreateAndDeleteQueue() {
        ResponseEntity<List<Map<String, Object>>> initial = controller.getQueues();
        assertNotNull(initial.getBody());
        int initialCount = initial.getBody().size();

        Map<String, Object> newQueue = Map.of("name", "q.cmd.test", "type", "standard", "durable", true);
        controller.createQueue(newQueue);
        assertEquals(initialCount + 1, controller.getQueues().getBody().size());

        controller.deleteQueue("q.cmd.test");
        assertEquals(initialCount, controller.getQueues().getBody().size());
    }

    @Test
    void exchangesAndBindingsCrud_ShouldCreateAndDelete() {
        int initialExchanges = controller.getExchanges().getBody().size();
        controller.createExchange(Map.of("name", "cmd.custom", "type", "topic"));
        assertEquals(initialExchanges + 1, controller.getExchanges().getBody().size());

        int initialBindings = controller.getBindings().getBody().size();
        controller.createBinding(Map.of("id", "b-custom", "source", "cmd.custom", "destination", "q.cmd.email", "routingKey", "email.#"));
        assertEquals(initialBindings + 1, controller.getBindings().getBody().size());

        controller.deleteBinding(Map.of("id", "b-custom"));
        assertEquals(initialBindings, controller.getBindings().getBody().size());

        controller.deleteExchange("cmd.custom");
        assertEquals(initialExchanges, controller.getExchanges().getBody().size());
    }
}
