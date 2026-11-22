package com.examly.springapp.dto.analytics;

public record SlaComplianceDTO(
        long evaluatedTickets,
        long metSlaCount,
        long breachedSlaCount,
        long activeOverdueCount,
        Double compliancePercentage,
        Double averageResolutionHours
) {}

