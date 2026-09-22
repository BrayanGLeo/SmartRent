package cl.duoc.mssmartrentcatalog.repositories;

import cl.duoc.mssmartrentcatalog.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CategoryRepository extends JpaRepository<Category, Long> {
}
