package cl.duoc.mssmartrentcheckout.repository;

import cl.duoc.mssmartrentcheckout.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
}
