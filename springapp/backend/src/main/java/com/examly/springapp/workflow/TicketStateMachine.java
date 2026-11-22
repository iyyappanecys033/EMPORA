package com.examly.springapp.workflow;

import com.examly.springapp.exception.BusinessRuleViolationException;
import com.examly.springapp.exception.IllegalStateTransitionException;
import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.TicketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Finite State Machine (FSM) enforcing valid state transitions, reason minimum lengths,
 * and dispute window boundaries for EPAMS Service Tickets.
 */
@Component
public class TicketStateMachine {

    private final Clock clock;
    private final BusinessCalendar businessCalendar;

    private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(TicketStatus.DRAFT, Set.of(TicketStatus.SUBMITTED, TicketStatus.CANCELLED)),
            Map.entry(TicketStatus.SUBMITTED, Set.of(TicketStatus.PENDING_APPROVAL, TicketStatus.ASSIGNED, TicketStatus.CANCELLED)),
            Map.entry(TicketStatus.PENDING_APPROVAL, Set.of(TicketStatus.PENDING_SECONDARY_APPROVAL, TicketStatus.ASSIGNED, TicketStatus.REJECTED, TicketStatus.CANCELLED)),
            Map.entry(TicketStatus.PENDING_SECONDARY_APPROVAL, Set.of(TicketStatus.ASSIGNED, TicketStatus.REJECTED, TicketStatus.CANCELLED)),
            Map.entry(TicketStatus.ASSIGNED, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED)),
            Map.entry(TicketStatus.IN_PROGRESS, Set.of(TicketStatus.ON_HOLD, TicketStatus.RESOLVED)),
            Map.entry(TicketStatus.ON_HOLD, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED)),
            Map.entry(TicketStatus.RESOLVED, Set.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS)),
            Map.entry(TicketStatus.CLOSED, Collections.emptySet()),
            Map.entry(TicketStatus.REJECTED, Collections.emptySet()),
            Map.entry(TicketStatus.CANCELLED, Collections.emptySet())
    );

    @Autowired
    public TicketStateMachine(Clock clock, BusinessCalendar businessCalendar) {
        this.clock = clock;
        this.businessCalendar = businessCalendar;
    }

    public TicketStateMachine(Clock clock) {
        this(clock, new BusinessCalendar(clock));
    }

    public TicketStateMachine() {
        this(Clock.system(BusinessCalendar.EPAMS_TIMEZONE));
    }

    public void validateTransition(ServiceTicket ticket, TicketStatus nextStatus, String reason, String actorUsername) {
        TicketStatus current = ticket.getStatus();

        if (current == nextStatus) {
            throw new IllegalStateTransitionException(
                    String.format("Ticket is already in state [%s]", current));
        }

        // 1. Guard against illegal state shifts
        Set<TicketStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
        if (!allowed.contains(nextStatus)) {
            throw new IllegalStateTransitionException(
                    String.format("Illegal transition from state [%s] to [%s]", current, nextStatus));
        }

        // 2. Reason length validation (BR-007: min 15 characters)
        if (requiresReason(current, nextStatus)) {
            if (reason == null || reason.trim().length() < 15) {
                throw new BusinessRuleViolationException(
                        "Transitioning to [" + nextStatus + "] requires a detailed reason of at least 15 characters.");
            }
        }

        // 3. Work-in-progress cancellation restriction (ADR-007)
        if (nextStatus == TicketStatus.CANCELLED && current == TicketStatus.IN_PROGRESS) {
            throw new BusinessRuleViolationException(
                    "Tickets in 'IN_PROGRESS' state cannot be cancelled unilaterally. Please coordinate with the assigned resolver.");
        }

        // 4. Dispute window validation for reopening (BR-003: 48 business hours, inclusive of exact deadline)
        if (current == TicketStatus.RESOLVED && nextStatus == TicketStatus.IN_PROGRESS) {
            LocalDateTime now = businessCalendar.nowInBusinessZone(clock);
            if (ticket.getResolvedAt() == null || !businessCalendar.isWithinReopenWindow(ticket.getResolvedAt(), now)) {
                throw new BusinessRuleViolationException(
                        "The 48-business-hour window to dispute or reopen this ticket has elapsed. Please submit a new service request.");
            }
        }
    }

    public boolean isTransitionAllowed(TicketStatus current, TicketStatus next) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet()).contains(next);
    }

    public Set<TicketStatus> getAllowedNextStates(TicketStatus current) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }

    private boolean requiresReason(TicketStatus current, TicketStatus next) {
        return next == TicketStatus.ON_HOLD ||
               next == TicketStatus.REJECTED ||
               next == TicketStatus.CANCELLED ||
               (current == TicketStatus.RESOLVED && next == TicketStatus.IN_PROGRESS);
    }
}
