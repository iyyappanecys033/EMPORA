package com.examly.springapp.workflow;

import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.ServiceTicketRepository;
import com.examly.springapp.service.AuditChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled SLA & Automatic Closure Processor (HLD §5.3 Clock 3, FR-021, ADR-016).
 *
 * Guarantees:
 * 1. Enforces BusinessCalendar 48-business-hour auto-closure eligibility strictly AFTER
 *    the inclusive dispute window expires (now > calculateReopenDeadline(resolvedAt)).
 * 2. Validates every state transition through TicketStateMachine (never mutates status blindly).
 * 3. Executes each ticket update in an isolated REQUIRES_NEW transaction with optimistic locking
 *    so a failure or concurrent user reopen on one ticket never affects other tickets.
 * 4. Records mandatory synchronous SHA-256 audit events under SYSTEM_AUTO_CLOSURE.
 * 5. Publishes deduplicated AFTER_COMMIT notifications (idempotent across retries).
 * 6. Excludes ON_HOLD (paused SLA clock), DRAFT, and terminal tickets from SLA breach scanning.
 */
@Service
public class SlaScheduledProcessor {

    public static final String SYSTEM_AUTO_CLOSURE_ACTOR = "SYSTEM_AUTO_CLOSURE";
    private static final long SLA_WARNING_THRESHOLD_HOURS = 2L;

    private static final Set<TicketStatus> ACTIVE_SLA_STATUSES = Set.of(
            TicketStatus.SUBMITTED,
            TicketStatus.PENDING_APPROVAL,
            TicketStatus.PENDING_SECONDARY_APPROVAL,
            TicketStatus.ASSIGNED,
            TicketStatus.IN_PROGRESS
    );

    private static final Logger log = LoggerFactory.getLogger(SlaScheduledProcessor.class);

    private final ServiceTicketRepository serviceTicketRepository;
    private final BusinessCalendar businessCalendar;
    private final TicketStateMachine stateMachine;
    private final AuditChainService auditChainService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate requiresNewTx;
    private final Clock clock;

    private final AtomicBoolean autoClosureRunning = new AtomicBoolean(false);
    private final AtomicBoolean slaAlertRunning = new AtomicBoolean(false);

    @Autowired
    public SlaScheduledProcessor(ServiceTicketRepository serviceTicketRepository,
                                 BusinessCalendar businessCalendar,
                                 TicketStateMachine stateMachine,
                                 AuditChainService auditChainService,
                                 ApplicationEventPublisher eventPublisher,
                                 PlatformTransactionManager transactionManager,
                                 Clock clock) {
        this.serviceTicketRepository = serviceTicketRepository;
        this.businessCalendar = businessCalendar;
        this.stateMachine = stateMachine;
        this.auditChainService = auditChainService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public record SlaJobSummary(int scanned, int processed, int skipped, int failed) {
    }

    /**
     * Hourly scheduled automatic closure of RESOLVED tickets past the 48-business-hour dispute window.
     */
    @Scheduled(cron = "${epams.sla.auto-closure-cron:0 0 * * * *}", zone = "Asia/Kolkata")
    public SlaJobSummary processAutoClosures() {
        return processAutoClosures(businessCalendar.nowInBusinessZone(clock));
    }

    /**
     * Deterministic entry point accepting an explicit Asia/Kolkata evaluation timestamp.
     */
    public SlaJobSummary processAutoClosures(LocalDateTime evaluationTime) {
        if (!autoClosureRunning.compareAndSet(false, true)) {
            log.warn("Auto-closure job skipped: previous execution is still active");
            return new SlaJobSummary(0, 0, 0, 0);
        }

        int scanned = 0;
        int closed = 0;
        int skipped = 0;
        int failed = 0;

        try {
            List<ServiceTicket> candidates = serviceTicketRepository.findByStatus(TicketStatus.RESOLVED);
            scanned = candidates.size();

            for (ServiceTicket candidate : candidates) {
                if (candidate.getResolvedAt() == null
                        || !businessCalendar.isAutoClosureEligible(candidate.getResolvedAt(), evaluationTime)) {
                    skipped++;
                    continue;
                }

                try {
                    boolean didClose = Boolean.TRUE.equals(requiresNewTx.execute(tx -> {
                        ServiceTicket fresh = serviceTicketRepository.findById(candidate.getId()).orElse(null);
                        if (fresh == null || fresh.getStatus() != TicketStatus.RESOLVED || fresh.getResolvedAt() == null) {
                            return false;
                        }
                        if (!businessCalendar.isAutoClosureEligible(fresh.getResolvedAt(), evaluationTime)) {
                            return false;
                        }

                        // Enforce state machine transition rule explicitly
                        stateMachine.validateTransition(fresh, TicketStatus.CLOSED,
                                "Automatic closure after 48 business hours elapsed without dispute.",
                                SYSTEM_AUTO_CLOSURE_ACTOR);

                        fresh.setStatus(TicketStatus.CLOSED);
                        fresh.setClosedAt(evaluationTime);
                        serviceTicketRepository.saveAndFlush(fresh);

                        auditChainService.recordChainedEvent(
                                SYSTEM_AUTO_CLOSURE_ACTOR,
                                "TICKET_AUTO_CLOSED",
                                fresh.getRequester() != null ? fresh.getRequester().getUsername() : "UNKNOWN",
                                "Ticket " + fresh.getTicketNumber()
                                        + " automatically closed after 48-business-hour dispute window expired."
                        );

                        if (fresh.getRequester() != null) {
                            eventPublisher.publishEvent(new TicketNotificationEvent(
                                    fresh.getId(),
                                    fresh.getTicketNumber(),
                                    fresh.getRequester().getUsername(),
                                    "TICKET_AUTO_CLOSED",
                                    "Ticket Automatically Closed: " + fresh.getTicketNumber(),
                                    "Your resolved ticket " + fresh.getTicketNumber()
                                            + " has been closed after the 48-business-hour dispute window elapsed.",
                                    "TICKET",
                                    fresh.getId(),
                                    "AUTO_CLOSE_" + fresh.getId()
                            ));
                        }
                        return true;
                    }));

                    if (didClose) {
                        closed++;
                    } else {
                        skipped++;
                    }
                } catch (ObjectOptimisticLockingFailureException conflict) {
                    log.info("Auto-closure skipped due to concurrent ticket modification: ticketId={}", candidate.getId());
                    skipped++;
                } catch (Exception ex) {
                    failed++;
                    log.error("Auto-closure failed safely for ticketId={}: {}", candidate.getId(), ex.getMessage());
                }
            }

            log.info("sla_auto_closure_run scanned={} closed={} skipped={} failed={}", scanned, closed, skipped, failed);
            return new SlaJobSummary(scanned, closed, skipped, failed);
        } finally {
            autoClosureRunning.set(false);
        }
    }

    /**
     * Periodic active SLA breach & approaching-warning scanner (FR-021).
     * Strictly excludes ON_HOLD (paused SLA clock), DRAFT, and terminal tickets.
     */
    @Scheduled(cron = "${epams.sla.warning-cron:0 */30 * * * *}", zone = "Asia/Kolkata")
    public SlaJobSummary processSlaAlerts() {
        return processSlaAlerts(businessCalendar.nowInBusinessZone(clock));
    }

    /**
     * Deterministic entry point accepting an explicit Asia/Kolkata evaluation timestamp.
     */
    public SlaJobSummary processSlaAlerts(LocalDateTime evaluationTime) {
        if (!slaAlertRunning.compareAndSet(false, true)) {
            log.warn("SLA alert scanner skipped: previous execution is still active");
            return new SlaJobSummary(0, 0, 0, 0);
        }

        int scanned = 0;
        int alertsPublished = 0;
        int skipped = 0;
        int failed = 0;

        try {
            List<ServiceTicket> activeTickets = serviceTicketRepository.findByStatusIn(ACTIVE_SLA_STATUSES);
            scanned = activeTickets.size();

            for (ServiceTicket ticket : activeTickets) {
                if (ticket.getSlaTargetDate() == null || ticket.getAssignee() == null) {
                    skipped++;
                    continue;
                }

                boolean isBreached = evaluationTime.isAfter(ticket.getSlaTargetDate());
                boolean isApproaching = !isBreached
                        && !evaluationTime.plusHours(SLA_WARNING_THRESHOLD_HOURS).isBefore(ticket.getSlaTargetDate());

                if (!isBreached && !isApproaching) {
                    skipped++;
                    continue;
                }

                try {
                    String eventType = isBreached ? "SLA_BREACHED" : "SLA_WARNING";
                    String dedupKey = (isBreached ? "SLA_BREACH_" : "SLA_WARN_") + ticket.getId();
                    String title = (isBreached ? "SLA Breached: " : "SLA Warning: ") + ticket.getTicketNumber();
                    String message = isBreached
                            ? "Ticket " + ticket.getTicketNumber() + " has breached its SLA target deadline."
                            : "Ticket " + ticket.getTicketNumber() + " is approaching its SLA target deadline.";
                    String recipientUsername = ticket.getAssignee().getUsername();

                    requiresNewTx.executeWithoutResult(tx -> eventPublisher.publishEvent(new TicketNotificationEvent(
                            ticket.getId(),
                            ticket.getTicketNumber(),
                            recipientUsername,
                            eventType,
                            title,
                            message,
                            "TICKET",
                            ticket.getId(),
                            dedupKey
                    )));
                    alertsPublished++;
                } catch (Exception ex) {
                    failed++;
                    log.error("SLA alert dispatch failed safely for ticketId={}: {}", ticket.getId(), ex.getMessage());
                }
            }

            log.info("sla_alert_scan scanned={} alerted={} skipped={} failed={}", scanned, alertsPublished, skipped, failed);
            return new SlaJobSummary(scanned, alertsPublished, skipped, failed);
        } finally {
            slaAlertRunning.set(false);
        }
    }
}
