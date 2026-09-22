package cl.duoc.mssmartrentrentals.services;

import cl.duoc.mssmartrentrentals.clients.CatalogClient;
import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.events.RentalEventPublisher;
import cl.duoc.mssmartrentrentals.repositories.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private RentalEventPublisher eventPublisher;

    @InjectMocks
    private RentalService rentalService;

    private Rental testRental;

    @BeforeEach
    void setUp() {
        testRental = new Rental();
        testRental.setId(1L);
        testRental.setMachineId(100L);
        testRental.setUserId("user123");
        testRental.setStartDate(LocalDate.now());
        testRental.setEndDate(LocalDate.now().plusDays(5));
        testRental.setStatus(RentalStatus.SOLICITADO);
    }

    @Test
    void createRental_ShouldSetStatusAndPublishEvent() {
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        Rental result = rentalService.createRental(testRental);

        assertNotNull(result);
        assertEquals(RentalStatus.SOLICITADO, result.getStatus());
        verify(rentalRepository, times(1)).save(testRental);
        verify(eventPublisher, times(1)).publishAuditAndReportEvent(testRental.getId(), testRental.getUserId(), testRental.getStatus().name());
    }

    @Test
    void changeRentalStatus_ToAprobado_ShouldCallCatalogAndPublishEvents() {
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        Rental result = rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "admin@test.com");

        assertEquals(RentalStatus.APROBADO, result.getStatus());
        verify(catalogClient, times(1)).updateAvailability(100L, false);
        verify(eventPublisher, times(1)).publishEmailEvent(1L, "admin@test.com", RentalStatus.APROBADO.name());
        verify(eventPublisher, times(1)).publishAuditAndReportEvent(1L, "user123", RentalStatus.APROBADO.name());
    }

    @Test
    void changeRentalStatus_ToEnPreparacion_ShouldPublishPrepEvent() {
        testRental.setStatus(RentalStatus.APROBADO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        Rental result = rentalService.changeRentalStatus(1L, RentalStatus.EN_PREPARACION, "admin@test.com");

        assertEquals(RentalStatus.EN_PREPARACION, result.getStatus());
        verify(eventPublisher, times(1)).publishPrepEvent(1L, 100L);
        verify(eventPublisher, times(1)).publishAuditAndReportEvent(1L, "user123", RentalStatus.EN_PREPARACION.name());
    }

    @Test
    void changeRentalStatus_InvalidTransition_ShouldThrowException() {
        // testRental is SOLICITADO. Attempting to jump directly to EN_TERRENO is invalid
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            rentalService.changeRentalStatus(1L, RentalStatus.EN_TERRENO, "admin@test.com");
        });

        assertTrue(exception.getMessage().contains("Transición de estado inválida"));
    }

    @Test
    void changeRentalStatus_ShouldThrowExceptionWhenNotFound() {
        when(rentalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "test@test.com");
        });
    }

    @Test
    void changeRentalStatus_ToEnTerreno_ShouldOnlyPublishAudit() {
        testRental.setStatus(RentalStatus.EN_PREPARACION);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        rentalService.changeRentalStatus(1L, RentalStatus.EN_TERRENO, "test@test.com");

        verify(eventPublisher).publishAuditAndReportEvent(eq(1L), any(), eq("EN_TERRENO"));
        verifyNoInteractions(catalogClient);
    }

    @Test
    void changeRentalStatus_ToDevuelto_ShouldUpdateAvailabilityAndEmail() {
        testRental.setStatus(RentalStatus.EN_TERRENO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        rentalService.changeRentalStatus(1L, RentalStatus.DEVUELTO, "test@test.com");

        verify(catalogClient).updateAvailability(100L, true);
        verify(eventPublisher).publishEmailEvent(1L, "test@test.com", "DEVUELTO");
    }

    @Test
    void changeRentalStatus_ToCancelado_FromAprobado_ShouldUpdateAvailabilityAndEmail() {
        testRental.setStatus(RentalStatus.APROBADO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        rentalService.changeRentalStatus(1L, RentalStatus.CANCELADO, "test@test.com");

        verify(catalogClient).updateAvailability(100L, true);
        verify(eventPublisher).publishEmailEvent(1L, "test@test.com", "CANCELADO");
    }

    @Test
    void changeRentalStatus_ToCancelado_FromSolicitado_ShouldNotUpdateAvailability() {
        testRental.setStatus(RentalStatus.SOLICITADO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        rentalService.changeRentalStatus(1L, RentalStatus.CANCELADO, "test@test.com");

        verify(catalogClient, never()).updateAvailability(anyLong(), anyBoolean());
        verify(eventPublisher).publishEmailEvent(1L, "test@test.com", "CANCELADO");
    }
    @Test
    void changeRentalStatus_ToCancelado_FromEnPreparacion_ShouldUpdateAvailability() {
        testRental.setStatus(RentalStatus.EN_PREPARACION);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);

        rentalService.changeRentalStatus(1L, RentalStatus.CANCELADO, "test@test.com");

        verify(catalogClient).updateAvailability(100L, true);
    }



    @Test
    void changeRentalStatus_SameStatus_ShouldDoNothing() {
        testRental.setStatus(RentalStatus.APROBADO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));

        Rental result = rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "test@test.com");
        assertEquals(RentalStatus.APROBADO, result.getStatus());
        verify(catalogClient, never()).updateAvailability(anyLong(), anyBoolean());
    }

    @Test
    void validateTransition_FromDevuelto_ShouldThrowException() {
        testRental.setStatus(RentalStatus.DEVUELTO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));

        assertThrows(IllegalStateException.class, () -> {
            rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "test@test.com");
        });
    }

    @Test
    void validateTransition_FromCancelado_ShouldThrowException() {
        testRental.setStatus(RentalStatus.CANCELADO);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(testRental));

        assertThrows(IllegalStateException.class, () -> {
            rentalService.changeRentalStatus(1L, RentalStatus.APROBADO, "test@test.com");
        });
    }
}
