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
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        when(machineRepository.save(Objects.requireNonNull(testMachine))).thenReturn(testMachine);

        Machine result = catalogService.createMachine(testMachine);

        assertNotNull(result);
        assertEquals("Excavadora", result.getName());
        verify(machineRepository, times(1)).save(Objects.requireNonNull(testMachine));
    }

    @Test
    void updateMachineAvailability_ShouldUpdateWhenExists() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(testMachine));
        when(machineRepository.save(Objects.requireNonNull(testMachine))).thenReturn(testMachine);

        Machine result = catalogService.updateMachineAvailability(1L, false);

        assertNotNull(result);
        assertFalse(result.getIsAvailable());
        verify(machineRepository, times(1)).findById(1L);
        verify(machineRepository, times(1)).save(Objects.requireNonNull(testMachine));
    }

    @Test
    void updateMachineAvailability_ShouldThrowExceptionWhenNotFound() {
        when(machineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> catalogService.updateMachineAvailability(99L, false));

        verify(machineRepository, times(1)).findById(99L);
        verify(machineRepository, never()).save(Objects.requireNonNull(testMachine));
    }

    @Test
    void updateMachine_ShouldUpdateExistingMachineFields() {
        when(machineRepository.findById(1L)).thenReturn(Optional.of(testMachine));
        when(machineRepository.save(Objects.requireNonNull(testMachine))).thenAnswer(inv -> inv.getArgument(0));

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

    @Test
    void getAllMachinesAndDeleteNonExistent_ShouldBehaveCorrectly() {
        when(machineRepository.findAll()).thenReturn(List.of(testMachine));
        assertEquals(1, catalogService.getAllMachines().size());

        when(machineRepository.existsById(99L)).thenReturn(false);
        catalogService.deleteMachine(99L);
        verify(machineRepository, never()).deleteById(99L);
    }

    @Test
    void oneArgConstructor_ShouldHandleNullCategoryRepoAndNullAvailability() {
        CatalogService simpleService = new CatalogService(machineRepository);
        Machine m = new Machine();
        m.setName("Taladro");
        when(machineRepository.save(any(Machine.class))).thenAnswer(inv -> inv.getArgument(0));

        Machine created = simpleService.createMachine(m);
        assertTrue(created.getIsAvailable());

        when(machineRepository.findById(99L)).thenReturn(Optional.empty());
        Machine updated = simpleService.updateMachine(99L, Map.of("name", "Nueva", "isAvailable", "false"));
        assertEquals("Nueva", updated.getName());
        assertFalse(updated.getIsAvailable());
    }

    @Test
    void createMachine_ShouldUpdateExistingBySerialAndResolveCategoryBranches() {
        Machine existing = new Machine();
        existing.setId(5L);
        existing.setSerialNumber("CAT-320");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(machineRepository.findBySerialNumberIgnoreCase("CAT-320")).thenReturn(Optional.of(existing));
        when(machineRepository.save(any(Machine.class))).thenAnswer(inv -> inv.getArgument(0));

        Machine updatedExisting = catalogService.createMachine(testMachine);
        assertEquals("Excavadora", updatedExisting.getName());

        // Resolve by existing category name
        Machine byCatName = new Machine();
        byCatName.setName("Compactador");
        byCatName.setSerialNumber("SN-NEW-1");
        Category catByName = new Category();
        catByName.setName("Compactación");
        byCatName.setCategory(catByName);
        when(categoryRepository.findByNameIgnoreCase("Compactación")).thenReturn(Optional.of(testCategory));
        when(machineRepository.findBySerialNumberIgnoreCase("SN-NEW-1")).thenReturn(Optional.empty());
        assertNotNull(catalogService.createMachine(byCatName));

        // Resolve by creating new category name
        Machine byNewCatName = new Machine();
        byNewCatName.setName("Sierra");
        Category newCat = new Category();
        newCat.setName("Herramientas");
        byNewCatName.setCategory(newCat);
        when(categoryRepository.findByNameIgnoreCase("Herramientas")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("Herramientas", catalogService.createMachine(byNewCatName).getCategory().getName());

        // Resolve using first existing category, then fallback when empty
        Machine noCat = new Machine();
        noCat.setName("Generador");
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory)).thenReturn(List.of());
        assertEquals("Maquinaria Pesada", catalogService.createMachine(noCat).getCategory().getName());

        Machine fallbackCat = new Machine();
        fallbackCat.setName("Torre");
        assertEquals("Maquinaria Pesada", catalogService.createMachine(fallbackCat).getCategory().getName());
    }

    @Test
    void updateMachine_ShouldHandleSerialLookupFallbackAndPayloadVariants() {
        when(machineRepository.findById(10L)).thenReturn(Optional.empty());
        when(machineRepository.findBySerialNumberIgnoreCase("CAT-320")).thenReturn(Optional.of(testMachine));
        when(machineRepository.save(any(Machine.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByNameIgnoreCase("Elevación")).thenReturn(Optional.of(testCategory));

        Machine bySerial = catalogService.updateMachine(10L, Map.of(
                "serialNumber", "CAT-320",
                "status", "SIN_STOCK",
                "category", Map.of("name", "Elevación")
        ));
        assertFalse(bySerial.getIsAvailable());

        when(machineRepository.findById(20L)).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));
        Machine fallbackBuilt = catalogService.updateMachine(20L, Map.of(
                "status", "EN_MANTENCION",
                "categoryId", "not-a-number",
                "category", "Elevación"
        ));
        assertEquals("Equipo Industrial #20", fallbackBuilt.getName());
        assertFalse(fallbackBuilt.getIsAvailable());

        Machine rented = catalogService.updateMachine(20L, Map.of(
                "status", "ARRENDADO",
                "availableStock", 3
        ));
        assertFalse(rented.getIsAvailable());

        Machine otherStatus = catalogService.updateMachine(20L, Map.of(
                "status", "OTRO",
                "availableStock", "2",
                "categoryId", 1L
        ));
        assertTrue(otherStatus.getIsAvailable());

        Machine stockOnly = catalogService.updateMachine(20L, Map.of(
                "availableStock", 0,
                "dailyPrice", "99000"
        ));
        assertFalse(stockOnly.getIsAvailable());
        assertEquals(99000.0, stockOnly.getDailyPrice());
    }
}
