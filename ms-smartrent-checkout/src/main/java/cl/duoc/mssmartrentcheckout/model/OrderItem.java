package cl.duoc.mssmartrentcheckout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "checkout_order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long machineId;

    @Column(nullable = false)
    private String machineName;

    @Column(nullable = false)
    private Integer dailyPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    public OrderItem() {}

    public OrderItem(Long machineId, String machineName, Integer dailyPrice) {
        this.machineId = machineId;
        this.machineName = machineName;
        this.dailyPrice = dailyPrice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMachineId() { return machineId; }
    public void setMachineId(Long machineId) { this.machineId = machineId; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public Integer getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(Integer dailyPrice) { this.dailyPrice = dailyPrice; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
}
