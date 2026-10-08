package cl.duoc.mssmartrentcheckout.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderModelTest {

    @Test
    void defaultConstructor_createsEmptyOrder() {
        Order order = new Order();
        assertNull(order.getId());
        assertNull(order.getUserId());
        assertNotNull(order.getItems());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void parameterizedConstructor_setsFieldsAndStatus() {
        Order expressOrder = new Order(
                "user-1", "EXPRESS", "PERSON", "11.111.111-1", "John", "+56911", 
                "PICKUP", null, "CARD"
        );
        
        assertEquals("user-1", expressOrder.getUserId());
        assertEquals("EXPRESS", expressOrder.getType());
        assertEquals("PERSON", expressOrder.getCustomerType());
        assertEquals("11.111.111-1", expressOrder.getRut());
        assertEquals("John", expressOrder.getName());
        assertEquals("+56911", expressOrder.getPhone());
        assertEquals("PICKUP", expressOrder.getDeliveryType());
        assertNull(expressOrder.getDeliveryAddress());
        assertEquals("CARD", expressOrder.getPaymentMethod());
        assertEquals("PENDING", expressOrder.getStatus());
        assertNotNull(expressOrder.getCreatedAt());

        Order quoteOrder = new Order(
                "user-1", "QUOTE", "COMPANY", "77.777.777-7", "Corp", "+56922", 
                "DISPATCH", "Av. Siempre Viva 123", "TRANSFER"
        );
        assertEquals("QUOTED", quoteOrder.getStatus());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user-123");
        order.setType("EXPRESS");
        order.setCustomerType("PERSON");
        order.setRut("1-9");
        order.setName("Test");
        order.setPhone("123");
        order.setDeliveryType("PICKUP");
        order.setDeliveryAddress("Address");
        order.setPaymentMethod("CASH");
        order.setStatus("COMPLETED");
        
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);

        assertEquals(1L, order.getId());
        assertEquals("user-123", order.getUserId());
        assertEquals("EXPRESS", order.getType());
        assertEquals("PERSON", order.getCustomerType());
        assertEquals("1-9", order.getRut());
        assertEquals("Test", order.getName());
        assertEquals("123", order.getPhone());
        assertEquals("PICKUP", order.getDeliveryType());
        assertEquals("Address", order.getDeliveryAddress());
        assertEquals("CASH", order.getPaymentMethod());
        assertEquals("COMPLETED", order.getStatus());
        assertEquals(now, order.getCreatedAt());
    }

    @Test
    void addItem_addsToCollectionAndSetsParent() {
        Order order = new Order();
        OrderItem item = new OrderItem(10L, "Excavadora", 100);
        
        order.addItem(item);
        
        assertEquals(1, order.getItems().size());
        assertEquals(order, item.getOrder());
    }

    @Test
    void setItems_replacesCollection() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        
        order.setItems(List.of(item));
        
        assertEquals(1, order.getItems().size());
    }
}
