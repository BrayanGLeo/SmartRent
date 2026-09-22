package cl.duoc.mssmartrentrentals.controllers;

import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.services.RentalService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rentals")
public class RentalsController {

    private final RentalService rentalService;

    public RentalsController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    public record RentalRequest(Long machineId, String userId, LocalDate startDate, LocalDate endDate) {}

    // Usuario final crea arriendo
    @PostMapping
    @PreAuthorize("hasRole('Arrendatario')")
    public ResponseEntity<Rental> createRental(@RequestBody RentalRequest request) {
        Rental rental = new Rental();
        rental.setMachineId(request.machineId());
        rental.setUserId(request.userId());
        rental.setStartDate(request.startDate());
        rental.setEndDate(request.endDate());
        // En un entorno real validaríamos que el usuario del token coincida con el del body
        return ResponseEntity.ok(rentalService.createRental(rental));
    }

    // Usuario ve sus propios arriendos
    @GetMapping("/my-rentals")
    @PreAuthorize("hasRole('Arrendatario')")
    public ResponseEntity<List<Rental>> getMyRentals(@RequestParam String userId) {
        return ResponseEntity.ok(rentalService.getRentalsByUser(userId));
    }

    // Jefe de Bodega o Admin ve todos los arriendos
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'JefeBodega')")
    public ResponseEntity<List<Rental>> getAllRentals() {
        return ResponseEntity.ok(rentalService.getAllRentals());
    }

    // Jefe de Bodega cambia el estado del arriendo
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'JefeBodega')")
    public ResponseEntity<Rental> updateRentalStatus(@org.springframework.lang.NonNull @PathVariable Long id, @RequestParam RentalStatus status) {
        String userEmail = getUserEmailFromToken();
        try {
            return ResponseEntity.ok(rentalService.changeRentalStatus(id, status, userEmail));
        } catch (IllegalStateException ignored) {
            return ResponseEntity.badRequest().build(); // Estado no permitido
        } catch (RuntimeException ignored) {
            return ResponseEntity.notFound().build(); // No existe
        }
    }

    private String getUserEmailFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            // Buscamos el correo (email o preferred_username dependiendo de Entra ID)
            String email = jwtToken.getToken().getClaimAsString("email");
            if (email == null) {
                email = jwtToken.getToken().getClaimAsString("preferred_username");
            }
            return email != null ? email : "unknown@user.com";
        }
        return "unknown@user.com";
    }
}
