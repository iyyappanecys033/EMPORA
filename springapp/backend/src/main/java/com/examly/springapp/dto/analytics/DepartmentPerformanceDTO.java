package com.examly.springapp.dto.analytics;

public record DepartmentPerformanceDTO(
        String department,
        long totalSubmittedInPeriod,
        long openBacklogSnapshot,
        long resolvedInPeriod,
        long overdueOpenSnapshot,
        Double slaComplianceRate
) {}

