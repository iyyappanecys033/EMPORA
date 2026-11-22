package com.examly.springapp.dto.analytics;

import java.util.List;

/**
 * Cohesive single-roundtrip dashboard payload preventing N-request waterfalls
 * when rendering the Operational Analytics workspace.
 */
public record AnalyticsDashboardBundleDTO(
        AnalyticsOverviewDTO overview,
        List<StatusDistributionDTO> statusDistribution,
        List<PriorityDistributionDTO> priorityDistribution,
        List<TicketTrendPointDTO> trends,
        List<DepartmentPerformanceDTO> departmentPerformance,
        List<ServicePopularityDTO> servicePopularity,
        SlaComplianceDTO slaCompliance,
        ApprovalOverviewDTO approvalsOverview
) {}

