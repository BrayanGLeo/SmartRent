package cl.duoc.mssmartrentcart.controller;

import cl.duoc.mssmartrentcart.model.Cart;
import cl.duoc.mssmartrentcart.model.CartItem;
import cl.duoc.mssmartrentcart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private Jwt mockJwt;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        mockJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("oid", "user-abc-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        testCart = new Cart("user-abc-123");
        testCart.setId(1L);
    }

    @Test
    void getCart_returnsCartForAuthenticatedUser() {
        when(cartService.getCart("user-abc-123")).thenReturn(testCart);

        ResponseEntity<Cart> response = cartController.getCart(mockJwt);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("user-abc-123", response.getBody().getUserId());
        verify(cartService).getCart("user-abc-123");
    }

    @Test
    void syncCart_syncsItemsAndReturnsCart() {
        CartItem item = new CartItem();
        item.setMachineId(100L);
        item.setMachineName("Excavadora");
        item.setType("express");
        item.setDailyPrice(50000);

        List<CartItem> items = List.of(item);
        when(cartService.syncCart("user-abc-123", items)).thenReturn(testCart);

        ResponseEntity<Cart> response = cartController.syncCart(mockJwt, items);

        assertEquals(200, response.getStatusCode().value());
        verify(cartService).syncCart("user-abc-123", items);
    }

    @Test
    void checkout_express_returnsProcessingMessage() {
        doNothing().when(cartService).checkout("user-abc-123", "express");

        ResponseEntity<Map<String, String>> response = cartController.checkout(mockJwt, "express");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Pago en proceso", response.getBody().get("message"));
        assertEquals("PROCESSING", response.getBody().get("status"));
    }

    @Test
    void checkout_quote_returnsQuoteMessage() {
        doNothing().when(cartService).checkout("user-abc-123", "quote");

        ResponseEntity<Map<String, String>> response = cartController.checkout(mockJwt, "quote");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Cotización solicitada", response.getBody().get("message"));
    }
}
