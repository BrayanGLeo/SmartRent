package cl.duoc.mssmartrentbff.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Administrador')")
public class RabbitTopologyController {

    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_DURABLE = "durable";
    private static final String KEY_AUTO_DELETE = "autoDelete";
    private static final String KEY_PURPOSE = "purpose";
    private static final String KEY_IS_OFFICIAL = "isOfficial";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_DESTINATION = "destination";
    private static final String KEY_ROUTING_KEY = "routingKey";
    private static final String TYPE_STANDARD = "standard";
    private static final String TYPE_DIRECT = "direct";
    private static final String QUEUE_CMD_EMAIL = "q.cmd.email";
    private static final String EXCHANGE_SMARTRENT = "smartrent.exchange";

    private final List<Map<String, Object>> queues = new CopyOnWriteArrayList<>(List.of(
            Map.of(KEY_NAME, QUEUE_CMD_EMAIL, KEY_TYPE, TYPE_STANDARD, KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "dlqAssociated", "q.cmd.email.dlq", KEY_PURPOSE, "Despacho de notificaciones por correo", KEY_IS_OFFICIAL, true),
            Map.of(KEY_NAME, "q.cmd.prep", KEY_TYPE, TYPE_STANDARD, KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "dlqAssociated", "q.cmd.prep.dlq", KEY_PURPOSE, "Generación de ticket de alistamiento en bodega", KEY_IS_OFFICIAL, true),
            Map.of(KEY_NAME, "q.rentals.order.created", KEY_TYPE, TYPE_STANDARD, KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    KEY_PURPOSE, "Creación automática de arriendos desde órdenes de Checkout", KEY_IS_OFFICIAL, true),
            Map.of(KEY_NAME, "q.cmd.email.dlq", KEY_TYPE, "dlq", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    KEY_PURPOSE, "Dead Letter Queue para contingencia de correos", KEY_IS_OFFICIAL, true)
    ));

    private final List<Map<String, Object>> exchanges = new CopyOnWriteArrayList<>(List.of(
            Map.of(KEY_NAME, "cmd.direct", KEY_TYPE, TYPE_DIRECT, KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    KEY_DESCRIPTION, "Enrutamiento directo de comandos operacionales", KEY_IS_OFFICIAL, true),
            Map.of(KEY_NAME, EXCHANGE_SMARTRENT, KEY_TYPE, TYPE_DIRECT, KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    KEY_DESCRIPTION, "Exchange de eventos de negocio para carrito y checkout", KEY_IS_OFFICIAL, true),
            Map.of(KEY_NAME, "x.notify", KEY_TYPE, TYPE_DIRECT, KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    KEY_DESCRIPTION, "Exchange directo de notificaciones", KEY_IS_OFFICIAL, true)
    ));

    private final List<Map<String, Object>> bindings = new CopyOnWriteArrayList<>(List.of(
            Map.of("id", "b-1", KEY_SOURCE, "cmd.direct", KEY_DESTINATION, QUEUE_CMD_EMAIL, KEY_ROUTING_KEY, "email.send"),
            Map.of("id", "b-2", KEY_SOURCE, EXCHANGE_SMARTRENT, KEY_DESTINATION, QUEUE_CMD_EMAIL, KEY_ROUTING_KEY, "order.created"),
            Map.of("id", "b-3", KEY_SOURCE, EXCHANGE_SMARTRENT, KEY_DESTINATION, "q.rentals.order.created", KEY_ROUTING_KEY, "order.created"),
            Map.of("id", "b-4", KEY_SOURCE, EXCHANGE_SMARTRENT, KEY_DESTINATION, QUEUE_CMD_EMAIL, KEY_ROUTING_KEY, "smartrent.routing.checkout"),
            Map.of("id", "b-5", KEY_SOURCE, "x.notify", KEY_DESTINATION, "q.cmd.prep", KEY_ROUTING_KEY, "prep")
    ));

    @GetMapping({"/api/queues", "/v1/api/queues", "/v2/api/queues"})
    public ResponseEntity<List<Map<String, Object>>> getQueues() {
        return ResponseEntity.ok(new ArrayList<>(queues));
    }

    @PostMapping({"/api/queues", "/v1/api/queues", "/v2/api/queues"})
    public ResponseEntity<Map<String, Object>> createQueue(@RequestBody Map<String, Object> queue) {
        if (queue != null && queue.get(KEY_NAME) != null) {
            String name = queue.get(KEY_NAME).toString().trim();
            queues.removeIf(q -> name.equalsIgnoreCase(String.valueOf(q.get(KEY_NAME))));
            queues.add(queue);
        }
        return ResponseEntity.ok(queue != null ? queue : Map.of());
    }

    @DeleteMapping({"/api/queues/{name}", "/v1/api/queues/{name}", "/v2/api/queues/{name}"})
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        queues.removeIf(q -> name.equalsIgnoreCase(String.valueOf(q.get(KEY_NAME))));
        bindings.removeIf(b -> name.equalsIgnoreCase(String.valueOf(b.get(KEY_DESTINATION))));
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/api/exchanges", "/v1/api/exchanges", "/v2/api/exchanges"})
    public ResponseEntity<List<Map<String, Object>>> getExchanges() {
        return ResponseEntity.ok(new ArrayList<>(exchanges));
    }

    @PostMapping({"/api/exchanges", "/v1/api/exchanges", "/v2/api/exchanges"})
    public ResponseEntity<Map<String, Object>> createExchange(@RequestBody Map<String, Object> exchange) {
        if (exchange != null && exchange.get(KEY_NAME) != null) {
            String name = exchange.get(KEY_NAME).toString().trim();
            exchanges.removeIf(e -> name.equalsIgnoreCase(String.valueOf(e.get(KEY_NAME))));
            exchanges.add(exchange);
        }
        return ResponseEntity.ok(exchange != null ? exchange : Map.of());
    }

    @DeleteMapping({"/api/exchanges/{name}", "/v1/api/exchanges/{name}", "/v2/api/exchanges/{name}"})
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        exchanges.removeIf(e -> name.equalsIgnoreCase(String.valueOf(e.get(KEY_NAME))));
        bindings.removeIf(b -> name.equalsIgnoreCase(String.valueOf(b.get(KEY_SOURCE))));
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/api/bindings", "/v1/api/bindings", "/v2/api/bindings"})
    public ResponseEntity<List<Map<String, Object>>> getBindings() {
        return ResponseEntity.ok(new ArrayList<>(bindings));
    }

    @PostMapping({"/api/bindings", "/v1/api/bindings", "/v2/api/bindings"})
    public ResponseEntity<Map<String, Object>> createBinding(@RequestBody Map<String, Object> binding) {
        if (binding != null) {
            bindings.add(binding);
        }
        return ResponseEntity.ok(binding != null ? binding : Map.of());
    }

    @DeleteMapping({"/api/bindings", "/v1/api/bindings", "/v2/api/bindings"})
    public ResponseEntity<Void> deleteBinding(@RequestBody(required = false) Map<String, Object> binding) {
        if (binding != null) {
            Object id = binding.get("id");
            if (id != null) {
                bindings.removeIf(b -> id.toString().equals(String.valueOf(b.get("id"))));
            } else {
                bindings.removeIf(b ->
                        String.valueOf(binding.get(KEY_SOURCE)).equals(String.valueOf(b.get(KEY_SOURCE)))
                                && String.valueOf(binding.get(KEY_DESTINATION)).equals(String.valueOf(b.get(KEY_DESTINATION)))
                                && String.valueOf(binding.get(KEY_ROUTING_KEY)).equals(String.valueOf(b.get(KEY_ROUTING_KEY)))
                );
            }
        }
        return ResponseEntity.noContent().build();
    }
}
