package cl.duoc.mssmartrentcheckout.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemModelTest {

    @Test
    void defaultConstructor_createsEmptyItem() {
        OrderItem item = new OrderItem();
        assertNull(item.getId());
        assertNull(item.getMachineId());
        assertNull(item.getMachineName());
        assertNull(item.getDailyPrice());
        assertNull(item.getOrder());
    }

    @Test
    void parameterizedConstructor_setsFields() {
        OrderItem item = new OrderItem(100L, "Excavadora", 50000);
        assertEquals(100L, item.getMachineId());
        assertEquals("Excavadora", item.getMachineName());
        assertEquals(50000, item.getDailyPrice());
        assertNull(item.getOrder());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setMachineId(200L);
        item.setMachineName("Grúa");
        item.setDailyPrice(100000);
        
        Order order = new Order();
        item.setOrder(order);

        assertEquals(1L, item.getId());
        assertEquals(200L, item.getMachineId());
        assertEquals("Grúa", item.getMachineName());
        assertEquals(100000, item.getDailyPrice());
        assertEquals(order, item.getOrder());
    }
}
