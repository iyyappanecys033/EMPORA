package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketCommentRequestDTO {

    @NotBlank(message = "Comment text cannot be blank")
    @Size(min = 1, max = 2000, message = "Comment text must be between 1 and 2000 characters")
    private String commentText;

    private Boolean isInternal;

    public TicketCommentRequestDTO() {
    }

    public TicketCommentRequestDTO(String commentText, Boolean isInternal) {
        this.commentText = commentText;
        this.isInternal = isInternal;
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
}

