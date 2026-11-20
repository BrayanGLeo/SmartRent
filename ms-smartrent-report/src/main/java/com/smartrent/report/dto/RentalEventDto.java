package com.smartrent.report.dto;

public class RentalEventDto {

    private String eventId;
    private String eventType;
    private Long rentalId;
    private Long machineId;
    private String machineName;
    private String userId;
    private String previousStatus;
    private String status;
    private String sourceService;
    private Long timestamp;
    private String occurredAt;

    public RentalEventDto() {
    }

    public RentalEventDto(
            String eventId,
            String eventType,
            Long rentalId,
            Long machineId,
            String machineName,
            String userId,
            String previousStatus,
            String status,
            String sourceService,
            Long timestamp,
            String occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.rentalId = rentalId;
        this.machineId = machineId;
        this.machineName = machineName;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.status = status;
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

    public Long getRentalId() {
        return rentalId;
    }

    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
