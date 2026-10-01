package cl.duoc.mssmartrentcheckout.dto;

import java.io.Serializable;
import java.util.List;

public class OrderCreatedEvent implements Serializable {
    private Long orderId;
    private String userId;
    private String rut;
    private String name;
    private String phone;
    private String customerType;
    private String orderType;
    private String deliveryType;
    private String deliveryAddress;
    private String paymentMethod;
    private List<CheckoutRequest.ItemDto> items;

    public OrderCreatedEvent() {}

    @SuppressWarnings("squid:S00107")
    public OrderCreatedEvent(Long orderId, String userId, String rut, String name, String phone, String customerType, String orderType, String deliveryType, String deliveryAddress, String paymentMethod, List<CheckoutRequest.ItemDto> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.rut = rut;
        this.name = name;
        this.phone = phone;
        this.customerType = customerType;
        this.orderType = orderType;
        this.deliveryType = deliveryType;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<CheckoutRequest.ItemDto> getItems() { return items; }
    public void setItems(List<CheckoutRequest.ItemDto> items) { this.items = items; }
}
