package cl.duoc.mssmartrentcatalog.services;

import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.repositories.CategoryRepository;
import cl.duoc.mssmartrentcatalog.repositories.MachineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CatalogServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CatalogService catalogService;

    private Machine testMachine;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(machineRepository, categoryRepository);
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Maquinaria Pesada");

        testMachine = new Machine();
        testMachine.setId(1L);
        testMachine.setName("Excavadora");
        testMachine.setSerialNumber("CAT-320");
        testMachine.setDailyPrice(150000.0);
        testMachine.setIsAvailable(true);
        testMachine.setCategory(testCategory);
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
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(machineRepository.findBySerialNumberIgnoreCase("CAT-320")).thenReturn(Optional.empty());
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

    @Test
    void updateMachine_ShouldUpdateExistingMachineFields() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(testMachine));
        when(machineRepository.save(any(Machine.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = Map.of(
                "name", "Excavadora Cat 320D",
                "sku", "CAT-320D",
                "dailyRate", 160000,
                "availableStock", 4,
                "status", "DISPONIBLE"
        );

        Machine updated = catalogService.updateMachine(1L, payload);

        assertEquals("Excavadora Cat 320D", updated.getName());
        assertEquals("CAT-320D", updated.getSerialNumber());
        assertEquals(160000.0, updated.getDailyPrice());
        assertTrue(updated.getIsAvailable());
    }

    @Test
    void deleteMachine_ShouldDeleteWhenExists() {
        when(machineRepository.existsById(1L)).thenReturn(true);

        catalogService.deleteMachine(1L);

        verify(machineRepository, times(1)).deleteById(1L);
    }
}
