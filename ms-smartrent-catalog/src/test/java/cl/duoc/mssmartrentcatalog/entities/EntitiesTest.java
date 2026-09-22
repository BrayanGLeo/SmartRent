package cl.duoc.mssmartrentcatalog.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntitiesTest {

    @Test
    void testMachineEntity() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Heavy");
        category.setDescription("Heavy machinery");

        Machine machine = new Machine();
        machine.setId(10L);
        machine.setName("Excavator");
        machine.setSerialNumber("12345");
        machine.setCategory(category);
        machine.setDailyPrice(100.0);
        machine.setIsAvailable(true);

        assertEquals(10L, machine.getId());
        assertEquals("Excavator", machine.getName());
        assertEquals("12345", machine.getSerialNumber());
        assertEquals("Heavy", machine.getCategory().getName());
        assertEquals(100.0, machine.getDailyPrice());
        assertEquals("Heavy machinery", machine.getCategory().getDescription());
        assertTrue(machine.getIsAvailable());
    }
}
