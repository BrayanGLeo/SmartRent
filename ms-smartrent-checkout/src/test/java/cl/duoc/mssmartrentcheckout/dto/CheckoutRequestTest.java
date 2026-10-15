package cl.duoc.mssmartrentcheckout.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CheckoutRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        CheckoutRequest request = new CheckoutRequest();
        request.setType("EXPRESS");
        request.setCustomerType("PERSON");
        request.setRut("1-9");
        request.setName("John");
        request.setPhone("123");
        request.setDeliveryType("PICKUP");
        request.setDeliveryAddress("Addr");
        request.setPaymentMethod("CARD");
        
        CheckoutRequest.ItemDto item = new CheckoutRequest.ItemDto();
        item.setMachineId(10L);
        request.setItems(List.of(item));

        assertEquals("EXPRESS", request.getType());
        assertEquals("PERSON", request.getCustomerType());
        assertEquals("1-9", request.getRut());
        assertEquals("John", request.getName());
        assertEquals("123", request.getPhone());
        assertEquals("PICKUP", request.getDeliveryType());
        assertEquals("Addr", request.getDeliveryAddress());
        assertEquals("CARD", request.getPaymentMethod());
        assertEquals(1, request.getItems().size());
    }

    @Test
    void itemDto_settersAndGetters_workCorrectly() {
        CheckoutRequest.ItemDto item = new CheckoutRequest.ItemDto();
        item.setMachineId(100L);
        item.setMachineName("Excavadora");
        item.setDailyPrice(50000);

        assertEquals(100L, item.getMachineId());
        assertEquals("Excavadora", item.getMachineName());
        assertEquals(50000, item.getDailyPrice());
    }
}
