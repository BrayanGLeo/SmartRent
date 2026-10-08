package cl.duoc.mssmartrentcart.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartModelTest {

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart("user-abc-123");
    }

    @Test
    void constructor_setsUserId() {
        assertEquals("user-abc-123", cart.getUserId());
        assertNotNull(cart.getItems());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void defaultConstructor_createsEmptyCart() {
        Cart emptyCart = new Cart();
        assertNull(emptyCart.getUserId());
        assertNotNull(emptyCart.getItems());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        cart.setId(10L);
        cart.setUserId("new-user");

        assertEquals(10L, cart.getId());
        assertEquals("new-user", cart.getUserId());
    }

    @Test
    void addItem_addsItemToCartAndSetsParent() {
        CartItem item = new CartItem();
        item.setMachineId(100L);
        item.setType("express");

        cart.addItem(item);

        assertEquals(1, cart.getItems().size());
        assertEquals(cart, item.getCart());
    }

    @Test
    void addItem_multipleItems() {
        CartItem item1 = new CartItem();
        item1.setMachineId(100L);
        item1.setType("express");

        CartItem item2 = new CartItem();
        item2.setMachineId(200L);
        item2.setType("quote");

        cart.addItem(item1);
        cart.addItem(item2);

        assertEquals(2, cart.getItems().size());
    }

    @Test
    void removeItem_removesItemAndNullifiesParent() {
        CartItem item = new CartItem();
        item.setMachineId(100L);
        item.setType("express");

        cart.addItem(item);
        assertEquals(1, cart.getItems().size());

        cart.removeItem(item);
        assertEquals(0, cart.getItems().size());
        assertNull(item.getCart());
    }

    @Test
    void setItems_replacesItemsList() {
        CartItem item = new CartItem();
        item.setMachineId(100L);
        item.setType("express");

        cart.setItems(List.of(item));
        assertEquals(1, cart.getItems().size());
    }
}
