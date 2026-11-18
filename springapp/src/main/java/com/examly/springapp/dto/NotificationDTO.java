package com.examly.springapp.dto;

import com.examly.springapp.model.InAppAlert;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private String eventType;
    private String title;
    private String message;
    private Long ticketId;
    private String ticketNumber;
    private String relatedEntityType;
    private Long relatedEntityId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public NotificationDTO() {
    }

    public static NotificationDTO fromEntity(InAppAlert alert) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(alert.getId());
        dto.setEventType(alert.getAlertType());
        dto.setTitle(alert.getTitle());
        dto.setMessage(alert.getMessage());
        if (alert.getTicket() != null) {
            dto.setTicketId(alert.getTicket().getId());
            dto.setTicketNumber(alert.getTicket().getTicketNumber());
        }
        dto.setRelatedEntityType(alert.getRelatedEntityType());
        dto.setRelatedEntityId(alert.getRelatedEntityId());
        dto.setRead(alert.isRead());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setReadAt(alert.getReadAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    @JsonProperty("isRead")
    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
