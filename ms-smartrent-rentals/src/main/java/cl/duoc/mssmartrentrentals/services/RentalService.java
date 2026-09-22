package cl.duoc.mssmartrentrentals.services;

import cl.duoc.mssmartrentrentals.clients.CatalogClient;
import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.entities.RentalStatus;
import cl.duoc.mssmartrentrentals.events.RentalEventPublisher;
import cl.duoc.mssmartrentrentals.repositories.RentalRepository;

import org.springframework.stereotype.Service;

import java.util.List;

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

    public Rental createRental(Rental rental) {
        rental.setStatus(RentalStatus.SOLICITADO);
        Rental savedRental = rentalRepository.save(rental);
        
        eventPublisher.publishAuditAndReportEvent(savedRental.getId(), savedRental.getUserId(), savedRental.getStatus().name());
        return savedRental;
    }

    public List<Rental> getRentalsByUser(String userId) {
        return rentalRepository.findByUserId(userId);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    public Rental changeRentalStatus(@org.springframework.lang.NonNull Long rentalId, RentalStatus newStatus, String userEmail) {
        Rental rental = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new RuntimeException("Rental not found"));

        RentalStatus currentStatus = rental.getStatus();

        if (currentStatus == newStatus) {
            return rental;
        }

        // Máquina de estados básica
        validateTransition(currentStatus, newStatus);

        rental.setStatus(newStatus);
        Rental updatedRental = rentalRepository.save(rental);

        // Lógica síncrona y asíncrona según el nuevo estado
        switch (newStatus) {
            case APROBADO:
                catalogClient.updateAvailability(rental.getMachineId(), false);
                eventPublisher.publishEmailEvent(rental.getId(), userEmail, "APROBADO");
                break;
            case EN_PREPARACION:
                eventPublisher.publishPrepEvent(rental.getId(), rental.getMachineId());
                break;
            case EN_TERRENO:
                // Solo registramos el cambio de estado en Kafka
                break;
            case DEVUELTO:
                catalogClient.updateAvailability(rental.getMachineId(), true);
                eventPublisher.publishEmailEvent(rental.getId(), userEmail, "DEVUELTO");
                break;
            case CANCELADO:
                if (currentStatus == RentalStatus.APROBADO || currentStatus == RentalStatus.EN_PREPARACION) {
                    catalogClient.updateAvailability(rental.getMachineId(), true);
                }
                eventPublisher.publishEmailEvent(rental.getId(), userEmail, "CANCELADO");
                break;
            default:
                break;
        }

        // Siempre mandamos el evento de auditoría a Kafka
        eventPublisher.publishAuditAndReportEvent(updatedRental.getId(), updatedRental.getUserId(), updatedRental.getStatus().name());

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
