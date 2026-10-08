package cl.duoc.mssmartrentcart.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartItemModelTest {

    @Test
    void defaultConstructor_createsEmptyItem() {
        CartItem item = new CartItem();
        assertNull(item.getId());
        assertNull(item.getMachineId());
        assertNull(item.getMachineName());
        assertNull(item.getType());
        assertNull(item.getDailyPrice());
        assertNull(item.getCart());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setMachineId(100L);
        item.setMachineName("Excavadora CAT 320");
        item.setType("express");
        item.setDailyPrice(50000);

        Cart cart = new Cart("user-123");
        item.setCart(cart);

        assertEquals(1L, item.getId());
        assertEquals(100L, item.getMachineId());
        assertEquals("Excavadora CAT 320", item.getMachineName());
        assertEquals("express", item.getType());
        assertEquals(50000, item.getDailyPrice());
        assertEquals(cart, item.getCart());
    }

    @Test
    void setType_quote() {
        CartItem item = new CartItem();
        item.setType("quote");
        assertEquals("quote", item.getType());
    }

    @Test
    void setDailyPrice_zero() {
        CartItem item = new CartItem();
        item.setDailyPrice(0);
        assertEquals(0, item.getDailyPrice());
    }
}
