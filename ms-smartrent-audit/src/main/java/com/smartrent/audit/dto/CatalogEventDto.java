package com.smartrent.audit.dto;

public class CatalogEventDto {

    private String eventId;
    private String eventType;
    private Long machineId;
    private String machineName;
    private String serialNumber;
    private String categoryName;
    private Double dailyPrice;
    private Boolean available;
    private String sourceService;
    private Long timestamp;
    private String occurredAt;

    public CatalogEventDto() {
    }

    public CatalogEventDto(
            String eventId,
            String eventType,
            Long machineId,
            String machineName,
            String serialNumber,
            String categoryName,
            Double dailyPrice,
            Boolean available,
            String sourceService,
            Long timestamp,
            String occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.machineId = machineId;
        this.machineName = machineName;
        this.serialNumber = serialNumber;
        this.categoryName = categoryName;
        this.dailyPrice = dailyPrice;
        this.available = available;
        this.sourceService = sourceService;
        this.timestamp = timestamp;
        this.occurredAt = occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(Double dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }
}
