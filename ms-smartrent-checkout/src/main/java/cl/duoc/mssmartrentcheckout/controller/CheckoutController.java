package cl.duoc.mssmartrentcheckout.controller;

import cl.duoc.mssmartrentcheckout.dto.CheckoutRequest;
import cl.duoc.mssmartrentcheckout.model.Order;
import cl.duoc.mssmartrentcheckout.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processCheckout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CheckoutRequest request) {
        
        String userId = jwt.getClaimAsString("oid");
        if (userId == null) {
            userId = jwt.getSubject();
        }

        try {
            Order order = checkoutService.processCheckout(userId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", order.getId());
            response.put("message", request.getType().equalsIgnoreCase("QUOTE") ? 
                "Cotización enviada exitosamente" : "Arriendo procesado exitosamente");
            response.put("status", order.getStatus());

            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
