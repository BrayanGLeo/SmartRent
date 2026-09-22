package cl.duoc.mssmartrentcatalog.repositories;

import cl.duoc.mssmartrentcatalog.entities.Machine;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByIsAvailableTrue();
}
