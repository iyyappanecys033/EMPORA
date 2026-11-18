package com.examly.springapp.dto;

import com.examly.springapp.model.TicketPriority;
import com.examly.springapp.model.TicketStatus;

import java.time.LocalDateTime;

public class ServiceTicketDTO {

    private Long id;
    private String ticketNumber;
    private Long catalogItemId;
    private String catalogItemTitle;
    private Long requesterId;
    private String requesterUsername;
    private Long assigneeId;
    private String assigneeUsername;
    private String department;
    private TicketPriority priority;
    private TicketStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime slaTargetDate;
    private Integer totalPausedMinutes;
    private Long version;
    private String slaStatus; // SLA_OK, SLA_WARNING, SLA_BREACHED

    public ServiceTicketDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getCatalogItemTitle() {
        return catalogItemTitle;
    }

    public void setCatalogItemTitle(String catalogItemTitle) {
        this.catalogItemTitle = catalogItemTitle;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    public void setAssigneeUsername(String assigneeUsername) {
        this.assigneeUsername = assigneeUsername;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getSlaTargetDate() {
        return slaTargetDate;
    }

    public void setSlaTargetDate(LocalDateTime slaTargetDate) {
        this.slaTargetDate = slaTargetDate;
    }

    public Integer getTotalPausedMinutes() {
        return totalPausedMinutes;
    }

    public void setTotalPausedMinutes(Integer totalPausedMinutes) {
        this.totalPausedMinutes = totalPausedMinutes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }
}

