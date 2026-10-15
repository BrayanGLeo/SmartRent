package cl.duoc.mssmartrentcheckout.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderCreatedEventTest {

    @Test
    void defaultConstructor_createsEmptyEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        assertNull(event.getOrderId());
    }

    @Test
    void parameterizedConstructor_setsFields() {
        CheckoutRequest.ItemDto item = new CheckoutRequest.ItemDto();
        item.setMachineId(100L);
        
        OrderCreatedEvent event = new OrderCreatedEvent(
                1L, "user-1", "1-9", "John", "123", "PERSON", "EXPRESS", 
                "PICKUP", "Addr", "CARD", List.of(item)
        );

        assertEquals(1L, event.getOrderId());
        assertEquals("user-1", event.getUserId());
        assertEquals("1-9", event.getRut());
        assertEquals("John", event.getName());
        assertEquals("123", event.getPhone());
        assertEquals("PERSON", event.getCustomerType());
        assertEquals("EXPRESS", event.getOrderType());
        assertEquals("PICKUP", event.getDeliveryType());
        assertEquals("Addr", event.getDeliveryAddress());
        assertEquals("CARD", event.getPaymentMethod());
        assertEquals(1, event.getItems().size());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(2L);
        event.setUserId("user-2");
        event.setRut("2-7");
        event.setName("Jane");
        event.setPhone("456");
        event.setCustomerType("COMPANY");
        event.setOrderType("QUOTE");
        event.setDeliveryType("DISPATCH");
        event.setDeliveryAddress("Office");
        event.setPaymentMethod("TRANSFER");
        event.setItems(List.of(new CheckoutRequest.ItemDto()));

        assertEquals(2L, event.getOrderId());
        assertEquals("user-2", event.getUserId());
        assertEquals("2-7", event.getRut());
        assertEquals("Jane", event.getName());
        assertEquals("456", event.getPhone());
        assertEquals("COMPANY", event.getCustomerType());
        assertEquals("QUOTE", event.getOrderType());
        assertEquals("DISPATCH", event.getDeliveryType());
        assertEquals("Office", event.getDeliveryAddress());
        assertEquals("TRANSFER", event.getPaymentMethod());
        assertEquals(1, event.getItems().size());
    }
}
