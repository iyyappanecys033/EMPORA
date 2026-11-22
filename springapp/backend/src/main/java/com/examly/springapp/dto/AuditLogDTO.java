package com.examly.springapp.dto;

import java.time.LocalDateTime;

public class AuditLogDTO {

    private Long id;
    private Long sequenceNumber;
    private String previousHash;
    private String currentHash;
    private String actorUsername;
    private String action;
    private String targetUsername;
    private String details;
    private LocalDateTime timestamp;

    public AuditLogDTO() {
    }

    public AuditLogDTO(Long id, Long sequenceNumber, String previousHash, String currentHash, 
                       String actorUsername, String action, String targetUsername, String details, 
                       LocalDateTime timestamp) {
        this.id = id;
        this.sequenceNumber = sequenceNumber;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetUsername = targetUsername;
        this.details = details;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

