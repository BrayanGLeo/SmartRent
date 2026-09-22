package cl.duoc.mssmartrentcatalog.services;

import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.repositories.MachineRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {

    private final MachineRepository machineRepository;

    public CatalogService(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public List<Machine> getAllAvailableMachines() {
        return machineRepository.findByIsAvailableTrue();
    }

    public List<Machine> getAllMachines() {
        return machineRepository.findAll();
    }

    public Machine createMachine(Machine machine) {
        machine.setIsAvailable(true); // By default
        return machineRepository.save(machine);
    }

    public Optional<Machine> getMachineById(@org.springframework.lang.NonNull Long id) {
        return machineRepository.findById(id);
    }

    public Machine updateMachineAvailability(@org.springframework.lang.NonNull Long id, Boolean isAvailable) {
        Optional<Machine> optionalMachine = machineRepository.findById(id);
        if (optionalMachine.isPresent()) {
            Machine machine = optionalMachine.get();
            machine.setIsAvailable(isAvailable);
            return machineRepository.save(machine);
        }
        throw new jakarta.persistence.EntityNotFoundException("Machine not found with id " + id);
    }
}
