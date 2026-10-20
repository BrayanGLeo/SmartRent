package cl.duoc.mssmartrentcatalog.controllers;

import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.services.CatalogService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog/services")
public class CatalogController {

    private static final String ADMIN_OR_BODEGA_AUTH =
            "hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')";

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Lectura pública de catálogo
    @GetMapping
    public ResponseEntity<List<Machine>> getCatalog() {
        return ResponseEntity.ok(catalogService.getAllAvailableMachines());
    }

    @GetMapping("/all")
    @PreAuthorize(ADMIN_OR_BODEGA_AUTH)
    public ResponseEntity<List<Machine>> getAllCatalog() {
        return ResponseEntity.ok(catalogService.getAllMachines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Machine> getMachineById(@org.springframework.lang.NonNull @PathVariable Long id) {
        return catalogService.getMachineById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record MachineRequest(
            String name,
            String serialNumber,
            Long categoryId,
            Double dailyPrice,
            Boolean isAvailable,
            String sku,
            Double dailyRate,
            Object category,
            Integer availableStock,
            String status
    ) {
        public MachineRequest(String name, String serialNumber, Long categoryId, Double dailyPrice, Boolean isAvailable) {
            this(name, serialNumber, categoryId, dailyPrice, isAvailable, null, null, null, null, null);
        }
    }

    // Admin o Jefe de Bodega crea una nueva máquina
    @PostMapping
    @PreAuthorize(ADMIN_OR_BODEGA_AUTH)
    public ResponseEntity<Machine> createMachine(@RequestBody MachineRequest request) {
        Machine machine = new Machine();
        machine.setName(resolveName(request));
        machine.setSerialNumber(resolveSerialNumber(request));
        machine.setDailyPrice(resolvePrice(request));
        machine.setCategory(resolveCategory(request));
        machine.setIsAvailable(resolveAvailable(request));

        return ResponseEntity.ok(catalogService.createMachine(machine));
    }

    // Admin o Jefe de Bodega actualiza stock, tarifa o datos de una máquina
    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_OR_BODEGA_AUTH)
    public ResponseEntity<Machine> updateMachine(
            @org.springframework.lang.NonNull @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(catalogService.updateMachine(id, body));
    }

    // Llamado interno o por JefeBodega/Admin para descontar/retornar disponibilidad
    @PutMapping("/{id}/availability")
    @PreAuthorize(ADMIN_OR_BODEGA_AUTH)
    public ResponseEntity<Machine> updateAvailability(
            @org.springframework.lang.NonNull @PathVariable Long id,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestBody(required = false) Map<String, Object> body) {
        Boolean resolved = isAvailable;
        if (resolved == null && body != null && body.get("isAvailable") != null) {
            resolved = Boolean.parseBoolean(body.get("isAvailable").toString());
        }
        return updateAvailability(id, resolved != null ? resolved : Boolean.TRUE);
    }

    public ResponseEntity<Machine> updateAvailability(@org.springframework.lang.NonNull Long id, Boolean isAvailable) {
        try {
            return ResponseEntity.ok(catalogService.updateMachineAvailability(id, isAvailable));
        } catch (RuntimeException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    // Admin o Jefe de Bodega elimina/da de baja un producto
    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_OR_BODEGA_AUTH)
    public ResponseEntity<Void> deleteMachine(@org.springframework.lang.NonNull @PathVariable Long id) {
        catalogService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }

    private static String resolveName(MachineRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            return request.name().trim();
        }
        return "Equipo Nuevo";
    }

    private static String resolveSerialNumber(MachineRequest request) {
        if (request.serialNumber() != null && !request.serialNumber().isBlank()) {
            return request.serialNumber().trim();
        }
        if (request.sku() != null && !request.sku().isBlank()) {
            return request.sku().trim();
        }
        return "SKU-" + System.currentTimeMillis();
    }

    private static Double resolvePrice(MachineRequest request) {
        if (request.dailyPrice() != null) {
            return request.dailyPrice();
        }
        if (request.dailyRate() != null) {
            return request.dailyRate();
        }
        return 0.0;
    }

    private static Category resolveCategory(MachineRequest request) {
        Category category = new Category();
        if (request.categoryId() != null) {
            category.setId(request.categoryId());
        } else if (request.category() != null) {
            category.setName(request.category().toString());
        } else {
            category.setId(1L);
        }
        return category;
    }

    private static Boolean resolveAvailable(MachineRequest request) {
        if (request.isAvailable() != null) {
            return request.isAvailable();
        }
        if (request.status() != null) {
            return !"SIN_STOCK".equalsIgnoreCase(request.status())
                    && !"EN_MANTENCION".equalsIgnoreCase(request.status());
        }
        if (request.availableStock() != null) {
            return request.availableStock() > 0;
        }
        return true;
    }
}
