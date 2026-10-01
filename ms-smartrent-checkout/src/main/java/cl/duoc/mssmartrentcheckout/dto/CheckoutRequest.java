package cl.duoc.mssmartrentcheckout.dto;

import java.util.List;

public class CheckoutRequest {
    private String type; // EXPRESS or QUOTE
    private String customerType; // PERSON or COMPANY
    private String rut;
    private String name;
    private String phone;
    private String deliveryType; // PICKUP or DISPATCH
    private String deliveryAddress;
    private String paymentMethod; // TRANSFER or CARD
    private List<ItemDto> items;

    public static class ItemDto implements java.io.Serializable {
        private Long machineId;
        private String machineName;
        private Integer dailyPrice;

        public Long getMachineId() { return machineId; }
        public void setMachineId(Long machineId) { this.machineId = machineId; }
        public String getMachineName() { return machineName; }
        public void setMachineName(String machineName) { this.machineName = machineName; }
        public Integer getDailyPrice() { return dailyPrice; }
        public void setDailyPrice(Integer dailyPrice) { this.dailyPrice = dailyPrice; }
    }

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
    public List<ItemDto> getItems() { return items; }
    public void setItems(List<ItemDto> items) { this.items = items; }
}
