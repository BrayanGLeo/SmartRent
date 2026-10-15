package cl.duoc.mssmartrentbff.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RentalsControllerTest {

    @InjectMocks
    private RentalsController rentalsController;

    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        mockJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "user-123")
                .claim("roles", List.of("Admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void listar_returnsListOfRentals() {
        List<Map<String, String>> result = rentalsController.listar(mockJwt);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("RNT-1042", result.get(0).get("id"));
        assertEquals("EN_TERRENO", result.get(0).get("estado"));
    }

    @Test
    void cambiarEstado_returnsUpdatedStatusMessage() {
        Map<String, String> body = Map.of("estado", "APROBADO");
        
        Map<String, String> result = rentalsController.cambiarEstado("RNT-1042", body);

        assertNotNull(result);
        assertEquals("RNT-1042", result.get("id"));
        assertEquals("APROBADO", result.get("estado"));
        assertEquals("Estado actualizado correctamente", result.get("mensaje"));
    }

    @Test
    void kpis_returnsStaticMetrics() {
        Map<String, Object> result = rentalsController.kpis();

        assertNotNull(result);
        assertEquals(12, result.get("arriendosActivos"));
        assertEquals(5, result.get("equiposEnTerreno"));
        assertEquals(3.4, result.get("tiempoCicloPromedioDias"));
    }
}
