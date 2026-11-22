package com.examly.springapp.dto;

import jakarta.validation.constraints.Size;

public class TicketActionRequestDTO {

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    private Long version;

    public TicketActionRequestDTO() {
    }

    public TicketActionRequestDTO(String reason, Long version) {
        this.reason = reason;
        this.version = version;
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

