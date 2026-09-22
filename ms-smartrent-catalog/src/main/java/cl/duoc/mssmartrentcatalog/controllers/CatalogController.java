package cl.duoc.mssmartrentcatalog.controllers;

import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.services.CatalogService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/services")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Cualquier persona autenticada con scope adecuado puede leer
    @GetMapping
    public ResponseEntity<List<Machine>> getCatalog() {
        return ResponseEntity.ok(catalogService.getAllAvailableMachines());
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('Admin', 'JefeBodega')")
    public ResponseEntity<List<Machine>> getAllCatalog() {
        return ResponseEntity.ok(catalogService.getAllMachines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Machine> getMachineById(@org.springframework.lang.NonNull @PathVariable Long id) {
        return catalogService.getMachineById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record MachineRequest(String name, String serialNumber, Long categoryId, Double dailyPrice, Boolean isAvailable) {}

    // Admin crea una nueva máquina
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Machine> createMachine(@RequestBody MachineRequest request) {
        Machine machine = new Machine();
        machine.setName(request.name());
        machine.setSerialNumber(request.serialNumber());
        machine.setDailyPrice(request.dailyPrice());
        Category category = new Category();
        category.setId(request.categoryId());
        machine.setCategory(category);
        machine.setIsAvailable(request.isAvailable() == null || request.isAvailable());
        return ResponseEntity.ok(catalogService.createMachine(machine));
    }

    // Llamado interno o por JefeBodega para descontar/retornar disponibilidad
    @PutMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('Admin', 'JefeBodega')")
    public ResponseEntity<Machine> updateAvailability(@org.springframework.lang.NonNull @PathVariable Long id, @RequestParam Boolean isAvailable) {
        try {
            return ResponseEntity.ok(catalogService.updateMachineAvailability(id, isAvailable));
        } catch (RuntimeException ignored) {
            return ResponseEntity.notFound().build();
        }
    }
}
