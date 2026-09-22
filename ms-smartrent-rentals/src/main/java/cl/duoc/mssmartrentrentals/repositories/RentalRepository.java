package cl.duoc.mssmartrentrentals.repositories;

import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    List<Rental> findByUserId(String userId);
    List<Rental> findByStatus(RentalStatus status);
}
