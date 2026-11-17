package cl.duoc.mssmartrentrentals.controllers;

import cl.duoc.mssmartrentrentals.controllers.RentalsController.RentalRequest;
import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.repositories.RentalRepository;
import cl.duoc.mssmartrentrentals.services.RentalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalsControllerTest {

    @Mock
    private RentalService rentalService;

    @InjectMocks
    private RentalsController rentalsController;

    private Rental testRental;

    @BeforeEach
    void setUp() {
        testRental = new Rental();
        testRental.setId(1L);
        testRental.setMachineId(100L);
        testRental.setUserId("user123");
        testRental.setStatus(RentalStatus.SOLICITADO);

        // Mock SecurityContext para getUserEmailFromToken()
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("preferred_username", "user123@test.com")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createRental_ShouldMapRequestAndReturnCreatedRental() {
        RentalRequest request = new RentalRequest(100L, "user123", LocalDate.now(), LocalDate.now().plusDays(5));
        when(rentalService.createRental(any(Rental.class))).thenReturn(testRental);

        ResponseEntity<Rental> response = rentalsController.createRental(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(rentalService, times(1)).createRental(any(Rental.class));
    }

    @Test
    void updateRentalStatus_ShouldReturnUpdatedRental() {
        when(rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "user123@test.com"))
                .thenReturn(testRental);

        ResponseEntity<Rental> response = rentalsController.updateRentalStatus(1L, RentalStatus.APROBADO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(rentalService, times(1)).changeRentalStatus(1L, RentalStatus.APROBADO, "user123@test.com");
    }

    @Test
    void getMyRentals_ShouldReturnList() {
        when(rentalService.getRentalsByUser("user123")).thenReturn(List.of(testRental));

        ResponseEntity<List<Rental>> response = rentalsController.getMyRentals("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Rental> body = java.util.Objects.requireNonNull(response.getBody());
        assertFalse(body.isEmpty());
        assertEquals(100L, body.get(0).getMachineId());
    }
    @Test
    void updateRentalStatus_ShouldReturnBadRequestOnIllegalState() {
        when(rentalService.changeRentalStatus(anyLong(), any(RentalStatus.class), anyString()))
                .thenThrow(new IllegalStateException("Invalid state"));

        ResponseEntity<Rental> response = rentalsController.updateRentalStatus(1L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateRentalStatus_ShouldReturnNotFoundOnRuntimeException() {
        when(rentalService.changeRentalStatus(anyLong(), any(RentalStatus.class), anyString()))
                .thenThrow(new RuntimeException("Not found"));

        ResponseEntity<Rental> response = rentalsController.updateRentalStatus(99L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateRentalStatus_WithPreferredUsernameClaim() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("preferred_username", "pref@test.com").build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "pref@test.com"))
                .thenReturn(testRental);

        ResponseEntity<Rental> response = rentalsController.updateRentalStatus(1L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateRentalStatus_WithNoEmailClaim() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("other", "value").build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "unknown@user.com"))
                .thenReturn(testRental);

        ResponseEntity<Rental> response = rentalsController.updateRentalStatus(1L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateRentalStatus_WithNoJwtAuth() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        when(rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "unknown@user.com"))
                .thenReturn(testRental);

        ResponseEntity<Rental> response = rentalsController.updateRentalStatus(1L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMyRentals_ShouldReturnForbiddenWhenRequestingOtherUser() {
        ResponseEntity<List<Rental>> response = rentalsController.getMyRentals("otherUser999");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(rentalService, never()).getRentalsByUser(anyString());
    }

    @Test
    void getAllRentals_ShouldFilterForArrendatario() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("oid", "user123")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ARRENDATARIO"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        Rental otherRental = new Rental();
        otherRental.setId(2L);
        otherRental.setMachineId(200L);
        otherRental.setUserId("other-user-999");
        otherRental.setStatus(RentalStatus.APROBADO);

        when(rentalService.getAllRentals()).thenReturn(List.of(testRental, otherRental));

        ResponseEntity<List<Rental>> response = rentalsController.getAllRentals();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Rental> body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(1, body.size());
        assertEquals("user123", body.get(0).getUserId());
    }

    @Test
    void getAllRentals_ShouldReturnAllForAdmin() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("oid", "admin-oid")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        Rental otherRental = new Rental();
        otherRental.setId(2L);
        otherRental.setMachineId(200L);
        otherRental.setUserId("other-user-999");

        when(rentalService.getAllRentals()).thenReturn(List.of(testRental, otherRental));

        ResponseEntity<List<Rental>> response = rentalsController.getAllRentals();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Rental> body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(2, body.size());
    }

    @Test
    void createRental_ShouldOverrideSpoofedUserIdForArrendatario() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "client@test.com")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ARRENDATARIO"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(rentalService.createRental(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));

        RentalRequest request = new RentalRequest(100L, "victim@test.com", LocalDate.now(), LocalDate.now().plusDays(3));
        ResponseEntity<Rental> response = rentalsController.createRental(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Rental body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals("client@test.com", body.getUserId());
    }

    @Test
    void updateRentalStatus_WithBodyMap_ShouldHandleValidInvalidAndNull() {
        when(rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "user123@test.com"))
                .thenReturn(testRental);

        ResponseEntity<Rental> okResp = rentalsController.updateRentalStatus(1L, null, Map.of("status", "aprobado"));
        assertEquals(HttpStatus.OK, okResp.getStatusCode());

        ResponseEntity<Rental> badInvalid = rentalsController.updateRentalStatus(1L, null, Map.of("status", "ESTADO_INVALIDO"));
        assertEquals(HttpStatus.BAD_REQUEST, badInvalid.getStatusCode());

        ResponseEntity<Rental> badNull = rentalsController.updateRentalStatus(1L, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, badNull.getStatusCode());
    }

    @Test
    void updateRentalStatus_WithFallbackRepository_ShouldHandleNotFoundAndGatewayError() {
        RentalRepository repo = mock(RentalRepository.class);
        RentalsController controllerWithRepo = new RentalsController(rentalService, repo);

        when(rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "user123@test.com"))
                .thenThrow(new NoSuchElementException("Not found"));
        when(repo.findById(1L)).thenReturn(Optional.of(testRental));
        when(repo.save(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<Rental> updatedExisting = controllerWithRepo.updateRentalStatus(1L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.OK, updatedExisting.getStatusCode());
        assertEquals(RentalStatus.APROBADO, updatedExisting.getBody().getStatus());

        when(rentalService.changeRentalStatus(99L, RentalStatus.EN_PREPARACION, "user123@test.com"))
                .thenThrow(new NoSuchElementException("Not found"));
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Rental> createdFallback = controllerWithRepo.updateRentalStatus(99L, RentalStatus.EN_PREPARACION);
        assertEquals(HttpStatus.OK, createdFallback.getStatusCode());
        assertEquals(99L, createdFallback.getBody().getId());

        when(rentalService.changeRentalStatus(2L, RentalStatus.APROBADO, "user123@test.com"))
                .thenThrow(new RuntimeException("Feign error"));

        ResponseEntity<Rental> badGateway = controllerWithRepo.updateRentalStatus(2L, RentalStatus.APROBADO);
        assertEquals(HttpStatus.BAD_GATEWAY, badGateway.getStatusCode());
    }

    @Test
    void createRental_WithNullFieldsAndEmailClaim_ShouldUseDefaults() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", "mail@test.com")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(rentalService.createRental(any(Rental.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<Rental> response = rentalsController.createRental(new RentalRequest(null, null, null, null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Rental body = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(1L, body.getMachineId());
        assertEquals("mail@test.com", body.getUserId());
        assertNotNull(body.getStartDate());
        assertNotNull(body.getEndDate());
    }

    @Test
    void getMyRentals_AdditionalAuthorizationBranches() {
        assertEquals(HttpStatus.FORBIDDEN, rentalsController.getMyRentals("").getStatusCode());

        when(rentalService.getRentalsByUser("user123@other.com")).thenReturn(List.of(testRental));
        assertEquals(HttpStatus.OK, rentalsController.getMyRentals("user123@other.com").getStatusCode());

        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("oid", "admin-id").build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(rentalService.getRentalsByUser("anyUser")).thenReturn(List.of(testRental));
        assertEquals(HttpStatus.OK, rentalsController.getMyRentals("anyUser").getStatusCode());
    }
}
