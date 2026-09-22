package cl.duoc.mssmartrentrentals.controllers;

import cl.duoc.mssmartrentrentals.controllers.RentalsController.RentalRequest;
import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.services.RentalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(rentalService.changeRentalStatus(eq(1L), eq(RentalStatus.APROBADO), eq("user123@test.com")))
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
}
