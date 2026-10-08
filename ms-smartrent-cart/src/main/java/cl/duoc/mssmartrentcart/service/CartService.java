package cl.duoc.mssmartrentcart.service;

import cl.duoc.mssmartrentcart.dto.CheckoutEvent;
import cl.duoc.mssmartrentcart.model.Cart;
import cl.duoc.mssmartrentcart.model.CartItem;
import cl.duoc.mssmartrentcart.repository.CartRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final AmqpTemplate rabbitTemplate;
    
    // Exchange and queue names can be defined in a properties file, but we use hardcoded for simplicity
    private static final String EXCHANGE = "smartrent.exchange";
    private static final String ROUTING_KEY_CHECKOUT = "smartrent.routing.checkout";

    public CartService(CartRepository cartRepository, AmqpTemplate rabbitTemplate) {
        this.cartRepository = cartRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Cart getCart(String userId) {
        return findOrCreateCart(userId);
    }

    private Cart findOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart(userId);
            return cartRepository.save(newCart);
        });
    }

    @Transactional
    public Cart syncCart(String userId, List<CartItem> incomingItems) {
        Cart cart = findOrCreateCart(userId);
        
        // Simple sync: clear existing and add new
        cart.getItems().clear();
        for (CartItem item : incomingItems) {
            cart.addItem(item);
        }
        
        return cartRepository.save(cart);
    }

    @Transactional
    public void checkout(String userId, String checkoutType) {
        Cart cart = findOrCreateCart(userId);
        
        // Filter items by type (express vs quote)
        List<CartItem> itemsToProcess = cart.getItems().stream()
                .filter(i -> i.getType().equalsIgnoreCase(checkoutType))
                .toList();

        if (itemsToProcess.isEmpty()) {
            throw new IllegalArgumentException("No items found for checkout type: " + checkoutType);
        }

        // Map to DTO
        List<CheckoutEvent.ItemDto> dtoItems = itemsToProcess.stream()
                .map(i -> new CheckoutEvent.ItemDto(i.getMachineId(), i.getMachineName(), i.getDailyPrice()))
                .toList();

        CheckoutEvent event = new CheckoutEvent(userId, checkoutType, dtoItems);

        // Publish to RabbitMQ
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_CHECKOUT, event);

        // Remove processed items from cart
        itemsToProcess.forEach(cart::removeItem);
        cartRepository.save(cart);
    }
}
