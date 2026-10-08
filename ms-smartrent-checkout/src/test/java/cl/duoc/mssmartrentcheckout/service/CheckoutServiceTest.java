package cl.duoc.mssmartrentcheckout.service;

import cl.duoc.mssmartrentcheckout.dto.CheckoutRequest;
import cl.duoc.mssmartrentcheckout.dto.OrderCreatedEvent;
import cl.duoc.mssmartrentcheckout.model.Order;
import cl.duoc.mssmartrentcheckout.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CheckoutServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AmqpTemplate rabbitTemplate;

    @InjectMocks
    private CheckoutService checkoutService;

    private CheckoutRequest validRequest;
    private CheckoutRequest.ItemDto itemDto;

    @BeforeEach
    void setUp() {
        itemDto = new CheckoutRequest.ItemDto();
        itemDto.setMachineId(100L);
        itemDto.setMachineName("Excavadora CAT 320");
        itemDto.setDailyPrice(50000);

        validRequest = new CheckoutRequest();
        validRequest.setType("EXPRESS");
        validRequest.setCustomerType("PERSON");
        validRequest.setRut("11.111.111-1");
        validRequest.setName("Juan Perez");
        validRequest.setPhone("+56912345678");
        validRequest.setDeliveryType("DISPATCH");
        validRequest.setDeliveryAddress("Av. Siempre Viva 123");
        validRequest.setPaymentMethod("CARD");
        validRequest.setItems(List.of(itemDto));
    }

    @Test
    void processCheckout_validExpressRequest_createsOrderAndPublishesEvent() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(1L);
            return savedOrder;
        });

        Order result = checkoutService.processCheckout("user-123", validRequest);

        assertNotNull(result);
        assertEquals("user-123", result.getUserId());
        assertEquals("EXPRESS", result.getType());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1, result.getItems().size());
        
        verify(orderRepository).save(any(Order.class));

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("smartrent.exchange"), eq("order.created"), captor.capture());
        
        OrderCreatedEvent event = captor.getValue();
        assertEquals(1L, event.getOrderId());
        assertEquals("user-123", event.getUserId());
        assertEquals("EXPRESS", event.getOrderType());
        assertEquals(1, event.getItems().size());
    }

    @Test
    void processCheckout_validQuoteRequest_createsOrderWithQuotedStatus() {
        validRequest.setType("QUOTE");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(2L);
            return savedOrder;
        });

        Order result = checkoutService.processCheckout("user-456", validRequest);

        assertNotNull(result);
        assertEquals("QUOTE", result.getType());
        assertEquals("QUOTED", result.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void processCheckout_nullItems_throwsException() {
        validRequest.setItems(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            checkoutService.processCheckout("user-123", validRequest)
        );
        
        assertEquals("La orden debe contener al menos un item", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void processCheckout_emptyItems_throwsException() {
        validRequest.setItems(new ArrayList<>());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            checkoutService.processCheckout("user-123", validRequest)
        );
        
        assertEquals("La orden debe contener al menos un item", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
}
