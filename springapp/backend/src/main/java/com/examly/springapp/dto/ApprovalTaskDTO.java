package com.examly.springapp.dto;

import com.examly.springapp.model.ApprovalStatus;
import com.examly.springapp.model.TicketStatus;

import java.time.LocalDateTime;

public class ApprovalTaskDTO {

    private Long id;
    private Long ticketId;
    private String ticketNumber;
    private Integer approvalStage;
    private ApprovalStatus status;
    private String decisionReason;
    private String approverUsername;
    private String requesterUsername;
    private TicketStatus newTicketStatus;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private Long version;

    public ApprovalTaskDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getApprovalStage() {
        return approvalStage;
    }

    public void setApprovalStage(Integer approvalStage) {
        this.approvalStage = approvalStage;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getApproverUsername() {
        return approverUsername;
    }

    public void setApproverUsername(String approverUsername) {
        this.approverUsername = approverUsername;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public TicketStatus getNewTicketStatus() {
        return newTicketStatus;
    }

    public void setNewTicketStatus(TicketStatus newTicketStatus) {
        this.newTicketStatus = newTicketStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

