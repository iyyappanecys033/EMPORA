package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * In-App Alert domain entity representing user-facing notifications.
 * Maps to 'in_app_alert' defined in Flyway V3.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "in_app_alert")
public class InAppAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private ServiceTicket ticket;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "dedup_key", length = 128)
    private String dedupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }

    public InAppAlert() {
    }

    public InAppAlert(UserAccount recipient, ServiceTicket ticket, String alertType, String title, String message) {
        this.recipient = recipient;
        this.ticket = ticket;
        this.alertType = alertType;
        this.title = title;
        this.message = message;
        this.isRead = false;
        if (ticket != null) {
            this.relatedEntityType = "TICKET";
            this.relatedEntityId = ticket.getId();
        }
    }

    public void markAsRead(LocalDateTime readTimestamp) {
        if (!isRead()) {
            this.isRead = true;
            this.readAt = readTimestamp;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public void setRecipient(UserAccount recipient) {
        this.recipient = recipient;
    }

    public ServiceTicket getTicket() {
        return ticket;
    }

    public void setTicket(ServiceTicket ticket) {
        this.ticket = ticket;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
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

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public boolean isRead() {
        return isRead != null && isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
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

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }
}

