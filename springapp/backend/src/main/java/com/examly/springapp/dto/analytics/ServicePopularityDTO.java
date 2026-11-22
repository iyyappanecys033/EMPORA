package com.examly.springapp.dto.analytics;

public record ServicePopularityDTO(
        Long catalogItemId,
        String itemCode,
        String title,
        String department,
        String category,
        long requestCount
) {}

