package cl.duoc.mssmartrentrentals.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RentalTest {

    @Test
    void testRentalEntity() {
        Rental rental = new Rental();
        rental.setId(1L);
        rental.setMachineId(10L);
        rental.setUserId("user1");
        rental.setStartDate(LocalDate.now());
        rental.setEndDate(LocalDate.now().plusDays(2));
        rental.setStatus(RentalStatus.SOLICITADO);

        assertEquals(1L, rental.getId());
        assertEquals(10L, rental.getMachineId());
        assertEquals("user1", rental.getUserId());
        assertNotNull(rental.getStartDate());
        assertNotNull(rental.getEndDate());
        assertEquals(RentalStatus.SOLICITADO, rental.getStatus());
    }

    @Test
    void testRentalStatusEnum() {
        assertEquals(6, RentalStatus.values().length);
        assertEquals(RentalStatus.SOLICITADO, RentalStatus.valueOf("SOLICITADO"));
    }
}
