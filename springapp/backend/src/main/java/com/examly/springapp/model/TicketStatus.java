package com.examly.springapp.model;

/**
 * The 11 canonical states of the EPAMS Service Ticket finite-state machine (FSM).
 */
public enum TicketStatus {
    DRAFT,
    SUBMITTED,
    PENDING_APPROVAL,
    PENDING_SECONDARY_APPROVAL,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    RESOLVED,
    CLOSED,
    REJECTED,
    CANCELLED
}

