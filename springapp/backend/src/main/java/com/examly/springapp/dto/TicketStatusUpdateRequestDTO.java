package com.examly.springapp.dto;

import com.examly.springapp.model.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TicketStatusUpdateRequestDTO {

    @NotNull(message = "Target status is required")
    private TicketStatus status;

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    private Long version;

    public TicketStatusUpdateRequestDTO() {
    }

    public TicketStatusUpdateRequestDTO(TicketStatus status, String reason, Long version) {
        this.status = status;
        this.reason = reason;
        this.version = version;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

