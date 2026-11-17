package cl.duoc.mssmartrentrentals.events;

import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.services.RentalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {

    @Mock
    private RentalService rentalService;

    @InjectMocks
    private OrderCreatedListener listener;

    @Test
    void onOrderCreated_ShouldCreateRentalForEachItem() {
        Map<String, Object> event = Map.of(
                "orderId", 100L,
                "userId", "cliente@smartrent.cl",
                "items", List.of(
                        Map.of("machineId", 2L, "machineName", "Retroexcavadora", "dailyPrice", 120000.0),
                        Map.of("machineId", 7L, "machineName", "Plataforma JLG", "dailyPrice", 95000.0)
                )
        );

        listener.onOrderCreated(event);

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalService, times(2)).createRental(captor.capture());

        List<Rental> created = captor.getAllValues();
        assertEquals(2L, created.get(0).getMachineId());
        assertEquals("cliente@smartrent.cl", created.get(0).getUserId());
        assertEquals(7L, created.get(1).getMachineId());
    }

    @Test
    void onOrderCreated_HandlesNullEmptyItemsStringIdsAndExceptions() {
        listener.onOrderCreated(null);
        verify(rentalService, never()).createRental(any());

        listener.onOrderCreated(Map.of("userId", "u1"));
        listener.onOrderCreated(Map.of("items", List.of(
                Map.of("machineId", "5"),
                Map.of("machineId", "invalid"),
                "not-a-map"
        )));

        when(rentalService.createRental(any())).thenThrow(new RuntimeException("DB fail"));
        listener.onOrderCreated(Map.of("userId", "u2"));

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalService, times(5)).createRental(captor.capture());
        assertEquals(1L, captor.getAllValues().get(0).getMachineId());
        assertEquals(5L, captor.getAllValues().get(1).getMachineId());
        assertEquals(1L, captor.getAllValues().get(2).getMachineId());
        assertEquals(1L, captor.getAllValues().get(3).getMachineId());
    }
}
