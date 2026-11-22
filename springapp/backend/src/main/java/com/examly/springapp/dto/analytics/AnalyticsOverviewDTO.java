package com.examly.springapp.dto.analytics;

import java.time.LocalDate;

public record AnalyticsOverviewDTO(
        long periodTotalTickets,
        long openTicketsSnapshot,
        long resolvedTicketsInPeriod,
        long closedTicketsInPeriod,
        long cancelledOrRejectedInPeriod,
        long overdueOpenTicketsSnapshot,
        Double slaComplianceRate,
        Double averageResolutionHours,
        long pendingApprovalsSnapshot,
        String scopedDepartment,
        LocalDate startDate,
        LocalDate endDate,
        String timezone
) {}

