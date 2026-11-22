package com.examly.springapp.dto.analytics;

public record ApprovalOverviewDTO(
        long pendingSnapshot,
        long stage1PendingSnapshot,
        long stage2PendingSnapshot,
        long overduePendingSnapshot,
        long approvedInPeriod,
        long rejectedInPeriod
) {}

