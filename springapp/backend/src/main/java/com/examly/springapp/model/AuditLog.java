package com.examly.springapp.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actorUsername;
    private String action;
    private String targetUsername;
    
    @Column(updatable = false)
    private LocalDateTime timestamp;
    
    @Column(length = 1000)
    private String details;

    @Column(name = "sequence_number", unique = true)
    private Long sequenceNumber;

    @Column(name = "previous_hash", length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String previousHash;

    @Column(name = "current_hash", length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currentHash;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public AuditLog() {
    }

    public AuditLog(String actorUsername, String action, String targetUsername, String details) {
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetUsername = targetUsername;
        this.details = details;
    }

    public AuditLog(String actorUsername, String action, String targetUsername, String details,
                    Long sequenceNumber, String previousHash, String currentHash, LocalDateTime timestamp) {
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetUsername = targetUsername;
        this.details = details;
        this.sequenceNumber = sequenceNumber;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }
}
