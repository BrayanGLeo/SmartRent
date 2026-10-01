package cl.duoc.mssmartrentcart.dto;

import java.util.List;

public class CheckoutEvent {
    private String userId;
    private String type; // "express" or "quote"
    private List<ItemDto> items;

    public CheckoutEvent() {}

    public CheckoutEvent(String userId, String type, List<ItemDto> items) {
        this.userId = userId;
        this.type = type;
        this.items = items;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<ItemDto> getItems() { return items; }
    public void setItems(List<ItemDto> items) { this.items = items; }

    public static class ItemDto {
        private Long machineId;
        private String machineName;
        private Integer dailyPrice;

        public ItemDto() {}

        public ItemDto(Long machineId, String machineName, Integer dailyPrice) {
            this.machineId = machineId;
            this.machineName = machineName;
            this.dailyPrice = dailyPrice;
        }

        public Long getMachineId() { return machineId; }
        public void setMachineId(Long machineId) { this.machineId = machineId; }

        public String getMachineName() { return machineName; }
        public void setMachineName(String machineName) { this.machineName = machineName; }

        public Integer getDailyPrice() { return dailyPrice; }
        public void setDailyPrice(Integer dailyPrice) { this.dailyPrice = dailyPrice; }
    }
}
