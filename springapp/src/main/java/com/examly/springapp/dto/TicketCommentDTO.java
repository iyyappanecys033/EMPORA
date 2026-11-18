package com.examly.springapp.dto;

import java.time.LocalDateTime;

public class TicketCommentDTO {

    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorUsername;
    private String commentText;
    private Boolean isInternal;
    private LocalDateTime createdAt;

    public TicketCommentDTO() {
    }

    public TicketCommentDTO(Long id, Long ticketId, Long authorId, String authorUsername, 
                            String commentText, Boolean isInternal, LocalDateTime createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.commentText = commentText;
        this.isInternal = isInternal;
        this.createdAt = createdAt;
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

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

