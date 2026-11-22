package com.examly.springapp.dto;

import com.examly.springapp.model.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApprovalDecisionRequestDTO {

    @NotNull(message = "Decision is required")
    private ApprovalStatus decision;

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    private Long version;

    public ApprovalDecisionRequestDTO() {
    }

    public ApprovalDecisionRequestDTO(ApprovalStatus decision, String reason, Long version) {
        this.decision = decision;
        this.reason = reason;
        this.version = version;
    }

    public ApprovalStatus getDecision() {
        return decision;
    }

    public void setDecision(ApprovalStatus decision) {
        this.decision = decision;
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

