package cl.duoc.mssmartrentrentals.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rentals")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long machineId;

    @Column(nullable = false)
    private String userId; // User identifier from token

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMachineId() { return machineId; }
    public void setMachineId(Long machineId) { this.machineId = machineId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }
}
