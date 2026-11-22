package com.examly.springapp.dto.analytics;

public record StatusDistributionDTO(
        String status,
        long count,
        double percentage
) {}

