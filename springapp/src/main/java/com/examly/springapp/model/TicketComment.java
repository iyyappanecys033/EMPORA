package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Ticket Comment domain entity representing public or internal notes on a service ticket.
 * Maps to the 'ticket_comment' database table as defined in Flyway V2.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "ticket_comment")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private ServiceTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isInternal == null) {
            this.isInternal = false;
        }
    }

    public TicketComment() {
    }

    public TicketComment(ServiceTicket ticket, UserAccount author, String commentText, Boolean isInternal) {
        this.ticket = ticket;
        this.author = author;
        this.commentText = commentText;
        this.isInternal = isInternal != null ? isInternal : false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceTicket getTicket() {
        return ticket;
    }

    public void setTicket(ServiceTicket ticket) {
        this.ticket = ticket;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public void setAuthor(UserAccount author) {
        this.author = author;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Boolean getIsInternal() {
        return isInternal;
    }

    public void setIsInternal(Boolean isInternal) {
        this.isInternal = isInternal;
    }

    public boolean isInternal() {
        return isInternal != null && isInternal;
    }

    public void setInternal(boolean internal) {
        this.isInternal = internal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

