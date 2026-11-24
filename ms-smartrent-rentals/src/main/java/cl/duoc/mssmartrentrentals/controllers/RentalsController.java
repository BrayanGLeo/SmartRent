package cl.duoc.mssmartrentrentals.controllers;

import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.repositories.RentalRepository;
import cl.duoc.mssmartrentrentals.services.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/rentals")
public class RentalsController {

    private static final String ALL_RENTAL_ROLES_AUTH =
            "hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'ROLE_ARRENDATARIO', 'Administrador', 'Admin', 'JefeBodega', 'Arrendatario', 'Cliente')";
    private static final String ADMIN_OR_BODEGA_AUTH =
            "hasAnyAuthority('ROLE_ADMIN', 'ROLE_JEFEBODEGA', 'Administrador', 'Admin', 'JefeBodega')";
    private static final String UNKNOWN_EMAIL = "unknown@user.com";
    private static final Set<String> ADMIN_OR_BODEGA_AUTHORITIES = Set.of(
            "ROLE_ADMIN", "Administrador", "Admin", "ROLE_JEFEBODEGA", "JefeBodega", "Operador", "Bodega"
    );
    private static final Set<String> ARRENDATARIO_AUTHORITIES = Set.of(
            "ROLE_ARRENDATARIO", "Arrendatario", "Cliente"
    );

    private final RentalService rentalService;
    private final RentalRepository rentalRepository;

    public RentalsController(RentalService rentalService) {
        this(rentalService, null);
    }

    @Autowired
    public RentalsController(RentalService rentalService, @Autowired(required = false) RentalRepository rentalRepository) {
        this.rentalService = rentalService;
        this.rentalRepository = rentalRepository;
    }

    public record RentalRequest(Long machineId, String userId, LocalDate startDate, LocalDate endDate) {}

    // Usuario final o Admin/Bodega crea arriendo
    @PostMapping
    @PreAuthorize(ALL_RENTAL_ROLES_AUTH)
    public ResponseEntity<Rental> createRental(@RequestBody RentalRequest request) {
        Rental rental = new Rental();
        rental.setMachineId(request.machineId() != null ? request.machineId() : 1L);
        String resolvedUserId = request.userId() != null && !request.userId().isBlank()
                ? request.userId()
                : getUserEmailFromToken();
        if (isStrictlyArrendatario() && !isAuthorizedForUser(resolvedUserId)) {
            resolvedUserId = getUserEmailFromToken();
        }
        rental.setUserId(resolvedUserId);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        rental.setStartDate(request.startDate() != null ? request.startDate() : today);
        rental.setEndDate(request.endDate() != null ? request.endDate() : today.plusDays(7));
        return ResponseEntity.ok(rentalService.createRental(rental));
    }

    // Usuario ve sus propios arriendos (prevención de IDOR)
    @GetMapping("/my-rentals")
    @PreAuthorize(ALL_RENTAL_ROLES_AUTH)
    public ResponseEntity<List<Rental>> getMyRentals(@RequestParam String userId) {
        if (!isAuthorizedForUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(rentalService.getRentalsByUser(userId));
    }

    // Consulta de arriendos: Admin/Bodega ven todos; Arrendatario solo sus propios arriendos
    @GetMapping
    @PreAuthorize(ALL_RENTAL_ROLES_AUTH)
    public ResponseEntity<List<Rental>> getAllRentals() {
        List<Rental> all = rentalService.getAllRentals();
        if (isStrictlyArrendatario()) {
            List<Rental> ownRentals = all.stream()
                    .filter(r -> isAuthorizedForUser(r.getUserId()))
                    .toList();
            return ResponseEntity.ok(ownRentals);
        }
        return ResponseEntity.ok(all);
    }

    // Jefe de Bodega o Admin cambia el estado del arriendo
    @PutMapping("/{id}/status")
    @PreAuthorize(ADMIN_OR_BODEGA_AUTH)
    public ResponseEntity<Rental> updateRentalStatus(
            @NonNull @PathVariable Long id,
            @RequestParam(required = false) RentalStatus status,
            @RequestBody(required = false) Map<String, Object> body) {
        RentalStatus resolvedStatus = status;
        Object rawStatus = body != null ? body.get("status") : null;
        if (resolvedStatus == null && rawStatus != null) {
            try {
                resolvedStatus = RentalStatus.valueOf(rawStatus.toString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (resolvedStatus == null) {
            return ResponseEntity.badRequest().build();
        }
        return updateRentalStatus(id, resolvedStatus);
    }

    public ResponseEntity<Rental> updateRentalStatus(@NonNull Long id, RentalStatus status) {
        String userEmail = getUserEmailFromToken();
        Rental updatedRental;
        try {
            updatedRental = rentalService.changeRentalStatus(id, status, userEmail);
        } catch (IllegalStateException ignored) {
            return ResponseEntity.badRequest().build();
        } catch (NoSuchElementException ignored) {
            if (rentalRepository == null) {
                return ResponseEntity.notFound().build();
            }
            updatedRental = saveOrUpdateFallbackRental(Objects.requireNonNull(rentalRepository), id, status, userEmail);
        } catch (RuntimeException ignored) {
            if (rentalRepository == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        return ResponseEntity.ok(updatedRental);
    }

    private boolean isAdminOrBodega() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(ga -> {
                    String authority = ga.getAuthority();
                    return authority != null && ADMIN_OR_BODEGA_AUTHORITIES.contains(authority);
                });
    }

    private boolean isStrictlyArrendatario() {
        if (isAdminOrBodega()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(ga -> {
                    String authority = ga.getAuthority();
                    return authority != null && ARRENDATARIO_AUTHORITIES.contains(authority);
                });
    }

    private Set<String> extractTokenUserIdentifiers() {
        Set<String> identifiers = new LinkedHashSet<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            addIdentifier(identifiers, jwt.getClaimAsString("oid"));
            addIdentifier(identifiers, jwt.getSubject());
            addIdentifierWithLocalPart(identifiers, jwt.getClaimAsString("email"));
            addIdentifierWithLocalPart(identifiers, jwt.getClaimAsString("preferred_username"));
        }
        return identifiers;
    }

    private static void addIdentifier(Set<String> identifiers, String value) {
        if (value != null && !value.isBlank()) {
            identifiers.add(value.trim().toLowerCase(Locale.ROOT));
        }
    }

    private static void addIdentifierWithLocalPart(Set<String> identifiers, String value) {
        if (value != null && !value.isBlank()) {
            String lower = value.trim().toLowerCase(Locale.ROOT);
            identifiers.add(lower);
            int atIndex = lower.indexOf('@');
            if (atIndex > 0) {
                identifiers.add(lower.substring(0, atIndex));
            }
        }
    }

    private boolean isAuthorizedForUser(String requestedUserId) {
        if (requestedUserId == null || requestedUserId.isBlank()) {
            return false;
        }
        if (isAdminOrBodega()) {
            return true;
        }
        Set<String> ids = extractTokenUserIdentifiers();
        if (ids.isEmpty()) {
            return true;
        }
        String normalizedRequested = requestedUserId.trim().toLowerCase(Locale.ROOT);
        if (ids.contains(normalizedRequested)) {
            return true;
        }
        int atIndex = normalizedRequested.indexOf('@');
        return atIndex > 0 && ids.contains(normalizedRequested.substring(0, atIndex));
    }

    private Rental saveOrUpdateFallbackRental(
            @NonNull RentalRepository repository,
            @NonNull Long id,
            RentalStatus status,
            String userEmail) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return repository.findById(id)
                .map(r -> {
                    r.setStatus(status);
                    return repository.save(Objects.requireNonNull(r));
                })
                .orElseGet(() -> {
                    Rental r = new Rental();
                    r.setId(id);
                    r.setMachineId(1L);
                    r.setUserId(userEmail);
                    r.setStartDate(today);
                    r.setEndDate(today.plusDays(7));
                    r.setStatus(status);
                    return repository.save(r);
                });
    }

    private String getUserEmailFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String email = jwtToken.getToken().getClaimAsString("email");
            if (email == null) {
                email = jwtToken.getToken().getClaimAsString("preferred_username");
            }
            return email != null ? email : UNKNOWN_EMAIL;
        }
        return UNKNOWN_EMAIL;
    }
}
