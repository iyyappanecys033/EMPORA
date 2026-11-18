package com.examly.springapp.dto.analytics;

import java.time.LocalDate;

public record TicketTrendPointDTO(
        LocalDate date,
        long submittedCount,
        long resolvedCount
) {}

