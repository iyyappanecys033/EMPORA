package com.examly.springapp.dto.analytics;

public record PriorityDistributionDTO(
        String priority,
        long count,
        double percentage
) {}

