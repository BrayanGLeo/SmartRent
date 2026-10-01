package cl.duoc.mssmartrentcart.controller;

import cl.duoc.mssmartrentcart.model.Cart;
import cl.duoc.mssmartrentcart.model.CartItem;
import cl.duoc.mssmartrentcart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/sync")
    public ResponseEntity<Cart> syncCart(@AuthenticationPrincipal Jwt jwt, @RequestBody List<CartItem> items) {
        String userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(cartService.syncCart(userId, items));
    }

    @PostMapping("/checkout/{type}")
    public ResponseEntity<Map<String, String>> checkout(@AuthenticationPrincipal Jwt jwt, @PathVariable String type) {
        String userId = getUserIdFromJwt(jwt);
        cartService.checkout(userId, type);
        
        String message = type.equals("express") ? "Pago en proceso" : "Cotización solicitada";
        return ResponseEntity.ok(Map.of("message", message, "status", "PROCESSING"));
    }

    private String getUserIdFromJwt(Jwt jwt) {
        // En Azure AD, el claim "oid" es el Object ID del usuario
        return jwt.getClaimAsString("oid");
    }
}
