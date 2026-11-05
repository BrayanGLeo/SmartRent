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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}
