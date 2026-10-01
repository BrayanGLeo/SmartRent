package cl.duoc.mssmartrentcart.repository;

import cl.duoc.mssmartrentcart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
