package cl.duoc.mssmartrentcart.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutEventTest {

    @Test
    void defaultConstructor_createsEmptyEvent() {
        CheckoutEvent event = new CheckoutEvent();
        assertNull(event.getUserId());
        assertNull(event.getType());
        assertNull(event.getItems());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        CheckoutEvent.ItemDto item = new CheckoutEvent.ItemDto(100L, "Excavadora", 50000);
        CheckoutEvent event = new CheckoutEvent("user-123", "express", List.of(item));

        assertEquals("user-123", event.getUserId());
        assertEquals("express", event.getType());
        assertEquals(1, event.getItems().size());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        CheckoutEvent event = new CheckoutEvent();
        event.setUserId("user-456");
        event.setType("quote");

        CheckoutEvent.ItemDto item = new CheckoutEvent.ItemDto();
        item.setMachineId(200L);
        item.setMachineName("Grúa Torre");
        item.setDailyPrice(120000);
        event.setItems(List.of(item));

        assertEquals("user-456", event.getUserId());
        assertEquals("quote", event.getType());
        assertEquals(1, event.getItems().size());
    }

    @Test
    void itemDto_defaultConstructor() {
        CheckoutEvent.ItemDto item = new CheckoutEvent.ItemDto();
        assertNull(item.getMachineId());
        assertNull(item.getMachineName());
        assertNull(item.getDailyPrice());
    }

    @Test
    void itemDto_parameterizedConstructor() {
        CheckoutEvent.ItemDto item = new CheckoutEvent.ItemDto(100L, "Excavadora", 50000);
        assertEquals(100L, item.getMachineId());
        assertEquals("Excavadora", item.getMachineName());
        assertEquals(50000, item.getDailyPrice());
    }

    @Test
    void itemDto_settersAndGetters() {
        CheckoutEvent.ItemDto item = new CheckoutEvent.ItemDto();
        item.setMachineId(300L);
        item.setMachineName("Rodillo");
        item.setDailyPrice(75000);

        assertEquals(300L, item.getMachineId());
        assertEquals("Rodillo", item.getMachineName());
        assertEquals(75000, item.getDailyPrice());
    }
}
