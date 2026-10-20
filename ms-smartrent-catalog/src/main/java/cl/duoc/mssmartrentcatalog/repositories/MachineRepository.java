package cl.duoc.mssmartrentcatalog.repositories;

import cl.duoc.mssmartrentcatalog.entities.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByIsAvailableTrue();
    Optional<Machine> findBySerialNumberIgnoreCase(String serialNumber);
}
