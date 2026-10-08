package cl.duoc.mssmartrentcart.service;

import cl.duoc.mssmartrentcart.dto.CheckoutEvent;
import cl.duoc.mssmartrentcart.model.Cart;
import cl.duoc.mssmartrentcart.model.CartItem;
import cl.duoc.mssmartrentcart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private AmqpTemplate rabbitTemplate;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private CartItem expressItem;
    private CartItem quoteItem;

    @BeforeEach
    void setUp() {
        testCart = new Cart("user-abc-123");
        testCart.setId(1L);

        expressItem = new CartItem();
        expressItem.setId(10L);
        expressItem.setMachineId(100L);
        expressItem.setMachineName("Excavadora CAT 320");
        expressItem.setType("express");
        expressItem.setDailyPrice(50000);

        quoteItem = new CartItem();
        quoteItem.setId(11L);
        quoteItem.setMachineId(200L);
        quoteItem.setMachineName("Grúa Torre");
        quoteItem.setType("quote");
        quoteItem.setDailyPrice(120000);
    }

    @Test
    void getCart_existingUser_returnsCart() {
        when(cartRepository.findByUserId("user-abc-123")).thenReturn(Optional.of(testCart));

        Cart result = cartService.getCart("user-abc-123");

        assertNotNull(result);
        assertEquals("user-abc-123", result.getUserId());
        verify(cartRepository).findByUserId("user-abc-123");
    }

    @Test
    void getCart_newUser_createsAndReturnsCart() {
        when(cartRepository.findByUserId("new-user")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.getCart("new-user");

        assertNotNull(result);
        assertEquals("new-user", result.getUserId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void syncCart_replacesItemsAndSaves() {
        when(cartRepository.findByUserId("user-abc-123")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<CartItem> newItems = List.of(expressItem, quoteItem);
        Cart result = cartService.syncCart("user-abc-123", newItems);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        verify(cartRepository).save(testCart);
    }

    @Test
    void syncCart_newUser_createsCartAndAddsItems() {
        when(cartRepository.findByUserId("new-user")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<CartItem> items = List.of(expressItem);
        Cart result = cartService.syncCart("new-user", items);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
    }

    @Test
    void checkout_expressItems_publishesEventAndRemovesItems() {
        testCart.addItem(expressItem);
        testCart.addItem(quoteItem);
        when(cartRepository.findByUserId("user-abc-123")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.checkout("user-abc-123", "express");

        ArgumentCaptor<CheckoutEvent> captor = ArgumentCaptor.forClass(CheckoutEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("smartrent.exchange"), eq("smartrent.routing.checkout"), captor.capture());

        CheckoutEvent event = captor.getValue();
        assertEquals("user-abc-123", event.getUserId());
        assertEquals("express", event.getType());
        assertEquals(1, event.getItems().size());
        assertEquals(100L, event.getItems().get(0).getMachineId());

        // Solo queda el item "quote" en el carrito
        verify(cartRepository).save(testCart);
    }

    @Test
    void checkout_quoteItems_publishesEventAndRemovesItems() {
        testCart.addItem(expressItem);
        testCart.addItem(quoteItem);
        when(cartRepository.findByUserId("user-abc-123")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.checkout("user-abc-123", "quote");

        ArgumentCaptor<CheckoutEvent> captor = ArgumentCaptor.forClass(CheckoutEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("smartrent.exchange"), eq("smartrent.routing.checkout"), captor.capture());

        CheckoutEvent event = captor.getValue();
        assertEquals("quote", event.getType());
        assertEquals(1, event.getItems().size());
        assertEquals(200L, event.getItems().get(0).getMachineId());
    }

    @Test
    void checkout_noItemsForType_throwsException() {
        testCart.addItem(expressItem);
        when(cartRepository.findByUserId("user-abc-123")).thenReturn(Optional.of(testCart));

        assertThrows(IllegalArgumentException.class, () ->
            cartService.checkout("user-abc-123", "quote")
        );

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void checkout_emptyCart_throwsException() {
        when(cartRepository.findByUserId("user-abc-123")).thenReturn(Optional.of(testCart));

        assertThrows(IllegalArgumentException.class, () ->
            cartService.checkout("user-abc-123", "express")
        );
    }
}
