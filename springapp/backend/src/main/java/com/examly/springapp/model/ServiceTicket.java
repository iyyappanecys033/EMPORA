package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service Ticket domain entity representing an employee service request.
 * Maps to the 'service_ticket' database table as defined in Flyway V2.
 * Supports optimistic concurrency control via JPA @Version.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "service_ticket")
public class ServiceTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 32)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private ServiceCatalogItem catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserAccount requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserAccount assignee;

    @Column(name = "department", nullable = false, length = 50)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status = TicketStatus.SUBMITTED;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "sla_target_date", nullable = false)
    private LocalDateTime slaTargetDate;

    @Column(name = "total_paused_minutes", nullable = false)
    private Integer totalPausedMinutes = 0;

    @Column(name = "last_paused_at")
    private LocalDateTime lastPausedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.totalPausedMinutes == null) {
            this.totalPausedMinutes = 0;
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    public ServiceTicket() {
    }

    public ServiceTicket(String ticketNumber, ServiceCatalogItem catalogItem, UserAccount requester,
                         String department, TicketPriority priority, String description, LocalDateTime slaTargetDate) {
        this.ticketNumber = ticketNumber;
        this.catalogItem = catalogItem;
        this.requester = requester;
        this.department = department;
        this.priority = priority != null ? priority : TicketPriority.MEDIUM;
        this.status = TicketStatus.SUBMITTED;
        this.description = description;
        this.slaTargetDate = slaTargetDate;
        this.totalPausedMinutes = 0;
        this.version = 0L;
    }

    // Domain business methods

    public void pauseSlaClock(LocalDateTime now) {
        if (this.lastPausedAt == null) {
            this.lastPausedAt = now != null ? now : LocalDateTime.now();
            this.status = TicketStatus.ON_HOLD;
        }
    }

    public void resumeSlaClock(LocalDateTime now) {
        if (this.lastPausedAt != null) {
            LocalDateTime resumeTime = now != null ? now : LocalDateTime.now();
            long pausedMinutes = Duration.between(this.lastPausedAt, resumeTime).toMinutes();
            this.totalPausedMinutes = (this.totalPausedMinutes != null ? this.totalPausedMinutes : 0) + (int) Math.max(0, pausedMinutes);
            this.lastPausedAt = null;
        }
    }

    public boolean canBeCancelledByRequester() {
        return this.status == TicketStatus.DRAFT ||
               this.status == TicketStatus.SUBMITTED ||
               this.status == TicketStatus.PENDING_APPROVAL ||
               this.status == TicketStatus.PENDING_SECONDARY_APPROVAL;
    }

    // Getters and Setters

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

    public ServiceCatalogItem getCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(ServiceCatalogItem catalogItem) {
        this.catalogItem = catalogItem;
    }

    public UserAccount getRequester() {
        return requester;
    }

    public void setRequester(UserAccount requester) {
        this.requester = requester;
    }

    public UserAccount getAssignee() {
        return assignee;
    }

    public void setAssignee(UserAccount assignee) {
        this.assignee = assignee;
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

    public LocalDateTime getLastPausedAt() {
        return lastPausedAt;
    }

    public void setLastPausedAt(LocalDateTime lastPausedAt) {
        this.lastPausedAt = lastPausedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

