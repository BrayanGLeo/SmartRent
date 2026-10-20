package cl.duoc.mssmartrentcatalog.repositories;

import cl.duoc.mssmartrentcatalog.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
}
