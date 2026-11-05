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

    private final List<Map<String, Object>> queues = new CopyOnWriteArrayList<>(List.of(
            Map.of(KEY_NAME, "q.cmd.email", KEY_TYPE, "standard", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "dlqAssociated", "q.cmd.email.dlq", "purpose", "Despacho de notificaciones por correo", "isOfficial", true),
            Map.of(KEY_NAME, "q.cmd.prep", KEY_TYPE, "standard", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "dlqAssociated", "q.cmd.prep.dlq", "purpose", "Generación de ticket de alistamiento en bodega", "isOfficial", true),
            Map.of(KEY_NAME, "q.rentals.order.created", KEY_TYPE, "standard", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "purpose", "Creación automática de arriendos desde órdenes de Checkout", "isOfficial", true),
            Map.of(KEY_NAME, "q.cmd.email.dlq", KEY_TYPE, "dlq", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "purpose", "Dead Letter Queue para contingencia de correos", "isOfficial", true)
    ));

    private final List<Map<String, Object>> exchanges = new CopyOnWriteArrayList<>(List.of(
            Map.of(KEY_NAME, "cmd.direct", KEY_TYPE, "direct", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "description", "Enrutamiento directo de comandos operacionales", "isOfficial", true),
            Map.of(KEY_NAME, "smartrent.exchange", KEY_TYPE, "direct", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "description", "Exchange de eventos de negocio para carrito y checkout", "isOfficial", true),
            Map.of(KEY_NAME, "x.notify", KEY_TYPE, "direct", KEY_DURABLE, true, KEY_AUTO_DELETE, false,
                    "description", "Exchange directo de notificaciones", "isOfficial", true)
    ));

    private final List<Map<String, Object>> bindings = new CopyOnWriteArrayList<>(List.of(
            Map.of("id", "b-1", "source", "cmd.direct", "destination", "q.cmd.email", "routingKey", "email.send"),
            Map.of("id", "b-2", "source", "smartrent.exchange", "destination", "q.cmd.email", "routingKey", "order.created"),
            Map.of("id", "b-3", "source", "smartrent.exchange", "destination", "q.rentals.order.created", "routingKey", "order.created"),
            Map.of("id", "b-4", "source", "smartrent.exchange", "destination", "q.cmd.email", "routingKey", "smartrent.routing.checkout"),
            Map.of("id", "b-5", "source", "x.notify", "destination", "q.cmd.prep", "routingKey", "prep")
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
        bindings.removeIf(b -> name.equalsIgnoreCase(String.valueOf(b.get("destination"))));
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
        bindings.removeIf(b -> name.equalsIgnoreCase(String.valueOf(b.get("source"))));
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
                        String.valueOf(binding.get("source")).equals(String.valueOf(b.get("source")))
                                && String.valueOf(binding.get("destination")).equals(String.valueOf(b.get("destination")))
                                && String.valueOf(binding.get("routingKey")).equals(String.valueOf(b.get("routingKey")))
                );
            }
        }
        return ResponseEntity.noContent().build();
    }
}
