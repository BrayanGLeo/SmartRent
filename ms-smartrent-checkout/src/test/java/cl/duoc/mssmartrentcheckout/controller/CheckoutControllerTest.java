package cl.duoc.mssmartrentcheckout.controller;

import cl.duoc.mssmartrentcheckout.dto.CheckoutRequest;
import cl.duoc.mssmartrentcheckout.model.Order;
import cl.duoc.mssmartrentcheckout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CheckoutControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private CheckoutController checkoutController;

    private Jwt mockJwtOid;
    private Jwt mockJwtSub;
    private CheckoutRequest request;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockJwtOid = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("oid", "user-oid-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
                
        mockJwtSub = Jwt.withTokenValue("mock-token2")
                .header("alg", "none")
                .subject("user-sub-456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        request = new CheckoutRequest();
        request.setType("EXPRESS");

        mockOrder = new Order();
        mockOrder.setId(10L);
        mockOrder.setStatus("PENDING");
    }

    @Test
    void processCheckout_withOid_success() {
        when(checkoutService.processCheckout("user-oid-123", request)).thenReturn(mockOrder);

        ResponseEntity<Map<String, Object>> response = checkoutController.processCheckout(mockJwtOid, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(10L, response.getBody().get("orderId"));
        assertEquals("Arriendo procesado exitosamente", response.getBody().get("message"));
        assertEquals("PENDING", response.getBody().get("status"));
        
        verify(checkoutService).processCheckout("user-oid-123", request);
    }
    
    @Test
    void processCheckout_withSubFallback_success() {
        when(checkoutService.processCheckout("user-sub-456", request)).thenReturn(mockOrder);

        ResponseEntity<Map<String, Object>> response = checkoutController.processCheckout(mockJwtSub, request);

        assertEquals(200, response.getStatusCode().value());
        verify(checkoutService).processCheckout("user-sub-456", request);
    }

    @Test
    void processCheckout_quote_returnsQuoteMessage() {
        request.setType("QUOTE");
        mockOrder.setStatus("QUOTED");
        when(checkoutService.processCheckout("user-oid-123", request)).thenReturn(mockOrder);

        ResponseEntity<Map<String, Object>> response = checkoutController.processCheckout(mockJwtOid, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Cotización enviada exitosamente", response.getBody().get("message"));
        assertEquals("QUOTED", response.getBody().get("status"));
    }

    @Test
    void processCheckout_invalidRequest_returnsBadRequest() {
        when(checkoutService.processCheckout(eq("user-oid-123"), any(CheckoutRequest.class)))
            .thenThrow(new IllegalArgumentException("La orden debe contener al menos un item"));

        ResponseEntity<Map<String, Object>> response = checkoutController.processCheckout(mockJwtOid, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("La orden debe contener al menos un item", response.getBody().get("message"));
    }
}
