package com.examly.springapp.dto;

import jakarta.validation.constraints.NotNull;

public class TicketAssignRequestDTO {

    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;

    public TicketAssignRequestDTO() {
    }

    public TicketAssignRequestDTO(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}

