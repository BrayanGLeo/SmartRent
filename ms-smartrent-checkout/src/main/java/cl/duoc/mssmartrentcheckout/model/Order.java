package cl.duoc.mssmartrentcheckout.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checkout_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String type; // EXPRESS or QUOTE

    @Column(nullable = false)
    private String customerType; // PERSON or COMPANY

    @Column(nullable = false)
    private String rut;

    @Column(nullable = false)
    private String name; // Full name or Business Name

    @Column(nullable = false)
    private String phone;

    private String deliveryType; // PICKUP or DISPATCH
    private String deliveryAddress;

    private String paymentMethod; // TRANSFER, CARD, etc.

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, QUOTED

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    @SuppressWarnings("squid:S00107")
    public Order(String userId, String type, String customerType, String rut, String name, String phone, String deliveryType, String deliveryAddress, String paymentMethod) {
        this.userId = userId;
        this.type = type;
        this.customerType = customerType;
        this.rut = rut;
        this.name = name;
        this.phone = phone;
        this.deliveryType = deliveryType;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.status = type.equalsIgnoreCase("QUOTE") ? "QUOTED" : "PENDING";
        this.createdAt = LocalDateTime.now(java.time.ZoneId.of("America/Santiago"));
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
