package cl.duoc.mssmartrentcatalog.services;

import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.repositories.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @InjectMocks
    private CatalogService catalogService;

    private Machine testMachine;

    @BeforeEach
    void setUp() {
        testMachine = new Machine();
        testMachine.setId(1L);
        testMachine.setName("Excavadora");
        testMachine.setIsAvailable(true);
    }

    @Test
    void getAllAvailableMachines_ShouldReturnOnlyAvailableMachines() {
        when(machineRepository.findByIsAvailableTrue()).thenReturn(List.of(testMachine));

        List<Machine> result = catalogService.getAllAvailableMachines();

        assertFalse(result.isEmpty());
        assertTrue(result.get(0).getIsAvailable());
        verify(machineRepository, times(1)).findByIsAvailableTrue();
    }

    @Test
    void createMachine_ShouldSaveAndReturnMachine() {
        when(machineRepository.save(any(Machine.class))).thenReturn(testMachine);

        Machine result = catalogService.createMachine(testMachine);

        assertNotNull(result);
        assertEquals("Excavadora", result.getName());
        verify(machineRepository, times(1)).save(testMachine);
    }

    @Test
    void updateMachineAvailability_ShouldUpdateWhenExists() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(testMachine));
        when(machineRepository.save(any(Machine.class))).thenReturn(testMachine);

        Machine result = catalogService.updateMachineAvailability(1L, false);

        assertNotNull(result);
        assertFalse(result.getIsAvailable());
        verify(machineRepository, times(1)).findById(1L);
        verify(machineRepository, times(1)).save(testMachine);
    }

    @Test
    void updateMachineAvailability_ShouldThrowExceptionWhenNotFound() {
        when(machineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> catalogService.updateMachineAvailability(99L, false));

        verify(machineRepository, times(1)).findById(99L);
        verify(machineRepository, never()).save(any(Machine.class));
    }
}
