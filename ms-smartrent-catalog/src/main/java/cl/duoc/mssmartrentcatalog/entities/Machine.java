package cl.duoc.mssmartrentcatalog.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Double dailyPrice;

    @Column(nullable = false)
    private Boolean isAvailable;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Double getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(Double dailyPrice) { this.dailyPrice = dailyPrice; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean available) { isAvailable = available; }
}
