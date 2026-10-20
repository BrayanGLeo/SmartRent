package cl.duoc.mssmartrentcatalog.controllers;

import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.services.CatalogService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog/services")
public class CatalogController {

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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')")
    public ResponseEntity<Machine> createMachine(@RequestBody MachineRequest request) {
        Machine machine = new Machine();
        machine.setName(request.name() != null && !request.name().isBlank() ? request.name().trim() : "Equipo Nuevo");

        String resolvedSerial = request.serialNumber() != null && !request.serialNumber().isBlank()
                ? request.serialNumber().trim()
                : (request.sku() != null && !request.sku().isBlank() ? request.sku().trim() : "SKU-" + System.currentTimeMillis());
        machine.setSerialNumber(resolvedSerial);

        Double resolvedPrice = request.dailyPrice() != null
                ? request.dailyPrice()
                : (request.dailyRate() != null ? request.dailyRate() : 0.0);
        machine.setDailyPrice(resolvedPrice);

        Category category = new Category();
        if (request.categoryId() != null) {
            category.setId(request.categoryId());
        } else if (request.category() != null) {
            category.setName(request.category().toString());
        } else {
            category.setId(1L);
        }
        machine.setCategory(category);

        Boolean resolvedAvailable = request.isAvailable();
        if (resolvedAvailable == null) {
            if (request.status() != null) {
                resolvedAvailable = !"SIN_STOCK".equalsIgnoreCase(request.status())
                        && !"EN_MANTENCION".equalsIgnoreCase(request.status());
            } else if (request.availableStock() != null) {
                resolvedAvailable = request.availableStock() > 0;
            } else {
                resolvedAvailable = true;
            }
        }
        machine.setIsAvailable(resolvedAvailable);

        return ResponseEntity.ok(catalogService.createMachine(machine));
    }

    // Admin o Jefe de Bodega actualiza stock, tarifa o datos de una máquina
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')")
    public ResponseEntity<Machine> updateMachine(
            @org.springframework.lang.NonNull @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(catalogService.updateMachine(id, body));
    }

    // Llamado interno o por JefeBodega/Admin para descontar/retornar disponibilidad
    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')")
    public ResponseEntity<Machine> updateAvailability(
            @org.springframework.lang.NonNull @PathVariable Long id,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestBody(required = false) Map<String, Object> body) {
        Boolean resolved = isAvailable;
        if (resolved == null && body != null && body.get("isAvailable") != null) {
            resolved = Boolean.parseBoolean(body.get("isAvailable").toString());
        }
        if (resolved == null) {
            resolved = true;
        }
        return updateAvailability(id, resolved);
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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')")
    public ResponseEntity<Void> deleteMachine(@org.springframework.lang.NonNull @PathVariable Long id) {
        catalogService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }
}
