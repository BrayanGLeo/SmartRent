package cl.duoc.mssmartrentrentals.services;

import cl.duoc.mssmartrentrentals.clients.CatalogClient;
import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.events.RentalEventPublisher;
import cl.duoc.mssmartrentrentals.repositories.RentalRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final CatalogClient catalogClient;
    private final RentalEventPublisher eventPublisher;

    public RentalService(RentalRepository rentalRepository, CatalogClient catalogClient, RentalEventPublisher eventPublisher) {
        this.rentalRepository = rentalRepository;
        this.catalogClient = catalogClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Rental createRental(Rental rental) {
        rental.setStatus(RentalStatus.SOLICITADO);
        Rental savedRental = rentalRepository.save(rental);

        eventPublisher.publishAuditAndReportEvent(
                savedRental.getId(),
                savedRental.getMachineId(),
                savedRental.getUserId(),
                null,
                savedRental.getStatus().name(),
                "RENTAL_CREATED"
        );
        return savedRental;
    }

    public List<Rental> getRentalsByUser(String userId) {
        return rentalRepository.findByUserId(userId);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    @Transactional
    public Rental changeRentalStatus(@org.springframework.lang.NonNull Long rentalId, RentalStatus newStatus, String userEmail) {
        Rental rental = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new NoSuchElementException("Rental not found"));

        RentalStatus currentStatus = rental.getStatus();

        if (currentStatus == newStatus) {
            return rental;
        }

        // Máquina de estados básica
        validateTransition(currentStatus, newStatus);

        // Actualización síncrona de catálogo antes de persistir el cambio de estado para evitar inconsistencias
        switch (newStatus) {
            case APROBADO -> catalogClient.updateAvailability(rental.getMachineId(), false);
            case DEVUELTO -> catalogClient.updateAvailability(rental.getMachineId(), true);
            case CANCELADO -> {
                if (currentStatus == RentalStatus.APROBADO || currentStatus == RentalStatus.EN_PREPARACION) {
                    catalogClient.updateAvailability(rental.getMachineId(), true);
                }
            }
            default -> {
                // Otros estados no modifican disponibilidad en catálogo
            }
        }

        rental.setStatus(newStatus);
        Rental updatedRental = rentalRepository.save(rental);

        // Eventos asíncronos según el nuevo estado
        switch (newStatus) {
            case APROBADO -> eventPublisher.publishEmailEvent(updatedRental.getId(), userEmail, "APROBADO");
            case EN_PREPARACION -> eventPublisher.publishPrepEvent(updatedRental.getId(), updatedRental.getMachineId());
            case DEVUELTO -> eventPublisher.publishEmailEvent(updatedRental.getId(), userEmail, "DEVUELTO");
            case CANCELADO -> eventPublisher.publishEmailEvent(updatedRental.getId(), userEmail, "CANCELADO");
            default -> {
                // EN_TERRENO solo registra el cambio de estado en Kafka
            }
        }

        // Siempre mandamos el evento de auditoría a Kafka
        eventPublisher.publishAuditAndReportEvent(
                updatedRental.getId(),
                updatedRental.getMachineId(),
                updatedRental.getUserId(),
                currentStatus.name(),
                updatedRental.getStatus().name(),
                "RENTAL_STATUS_CHANGED"
        );

        return updatedRental;
    }

    private void validateTransition(RentalStatus current, RentalStatus next) {
        if (current == next) return;

        boolean valid = switch (current) {
            case SOLICITADO -> next == RentalStatus.APROBADO || next == RentalStatus.CANCELADO;
            case APROBADO -> next == RentalStatus.EN_PREPARACION || next == RentalStatus.CANCELADO;
            case EN_PREPARACION -> next == RentalStatus.EN_TERRENO || next == RentalStatus.CANCELADO;
            case EN_TERRENO -> next == RentalStatus.DEVUELTO;
            case DEVUELTO, CANCELADO -> false; // Estados finales
        };

        if (!valid) {
            throw new IllegalStateException("Transición de estado inválida de " + current + " a " + next);
        }
    }
}
