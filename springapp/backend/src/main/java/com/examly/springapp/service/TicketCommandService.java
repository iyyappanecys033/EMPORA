package com.examly.springapp.service;

import com.examly.springapp.dto.*;
import com.examly.springapp.exception.*;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.*;
import com.examly.springapp.security.TicketSecurityEvaluator;
import com.examly.springapp.workflow.BusinessCalendar;
import com.examly.springapp.workflow.TicketStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application Command Service coordinating ticket mutations, state machine transitions,
 * SLA clock management, and synchronous cryptographic audit logging.
 */
@Service
public class TicketCommandService {

    private static final AtomicLong TICKET_SEQ = new AtomicLong(100);

    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserAccountRepo userAccountRepo;
    private final TicketStateMachine stateMachine;
    private final ApprovalService approvalService;
    private final AuditChainService auditChainService;
    private final BusinessCalendar businessCalendar;
    private final TicketSecurityEvaluator ticketSecurity;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public TicketCommandService(ServiceTicketRepository serviceTicketRepository,
                                ServiceCatalogRepository serviceCatalogRepository,
                                TicketCommentRepository ticketCommentRepository,
                                UserAccountRepo userAccountRepo,
                                TicketStateMachine stateMachine,
                                ApprovalService approvalService,
                                AuditChainService auditChainService,
                                BusinessCalendar businessCalendar,
                                TicketSecurityEvaluator ticketSecurity,
                                ApplicationEventPublisher eventPublisher,
                                Clock clock) {
        this.serviceTicketRepository = serviceTicketRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.userAccountRepo = userAccountRepo;
        this.stateMachine = stateMachine;
        this.approvalService = approvalService;
        this.auditChainService = auditChainService;
        this.businessCalendar = businessCalendar;
        this.ticketSecurity = ticketSecurity;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Submits a new service ticket.
     */
    @Transactional
    public ServiceTicketDTO createTicket(CreateTicketRequestDTO request, String requesterUsername) {
        if (request == null || request.getCatalogItemId() == null) {
            throw new BusinessRuleViolationException("Catalog item identifier is required.");
        }
        if (request.getDescription() == null || request.getDescription().trim().isBlank()) {
            throw new BusinessRuleViolationException("Ticket description cannot be blank.");
        }

        UserAccount requester = userAccountRepo.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterUsername));

        ServiceCatalogItem catalogItem = serviceCatalogRepository.findById(request.getCatalogItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + request.getCatalogItemId()));

        if (!Boolean.TRUE.equals(catalogItem.getIsActive())) {
            throw new BusinessRuleViolationException("The selected catalog item is inactive and cannot accept requests.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String ticketNumber = generateTicketNumber();
        String targetDept = (request.getDepartment() != null && !request.getDepartment().isBlank()) 
                ? request.getDepartment().trim() 
                : catalogItem.getDepartment();

        LocalDateTime slaTargetDate = businessCalendar.calculateTargetDate(now, catalogItem.getSlaTargetHours());

        ServiceTicket ticket = new ServiceTicket(
                ticketNumber,
                catalogItem,
                requester,
                targetDept,
                request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM,
                request.getDescription(),
                slaTargetDate
        );
        ticket.setStatus(TicketStatus.SUBMITTED);
        ticket.setCreatedAt(now);

        ServiceTicket savedTicket = serviceTicketRepository.saveAndFlush(ticket);

        // Synchronous transactional audit
        auditChainService.recordChainedEvent(requesterUsername, "TICKET_CREATED", requesterUsername,
                "Submitted ticket " + ticketNumber + " for catalog item [" + catalogItem.getItemCode() + "]");

        // Trigger approval workflow or queue directly
        approvalService.createApprovalTasksForTicket(savedTicket);

        // Notify requester
        eventPublisher.publishEvent(new TicketNotificationEvent(
                savedTicket.getId(),
                savedTicket.getTicketNumber(),
                requesterUsername,
                "TICKET_SUBMITTED",
                "Ticket Submitted: " + savedTicket.getTicketNumber(),
                "Your request has been successfully registered."
        ));

        return toDto(savedTicket);
    }

    /**
     * Assigns or self-claims a ticket within the specialist department queue.
     */
    @Transactional
    public ServiceTicketDTO assignTicket(Long ticketId, Long assigneeId, String actorUsername) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (!ticketSecurity.canAssignTicket(ticket, actorUsername)) {
            throw new UnauthorizedScopeException("User [" + actorUsername + "] is not authorized to assign tickets for department [" + ticket.getDepartment() + "]");
        }

        UserAccount assignee;
        if (assigneeId != null) {
            assignee = userAccountRepo.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found with ID: " + assigneeId));
        } else {
            // Self-assignment
            assignee = userAccountRepo.findByUsername(actorUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorUsername));
        }

        ticket.setAssignee(assignee);

        // If ticket is in ASSIGNED state, advance to IN_PROGRESS upon active pickup
        if (ticket.getStatus() == TicketStatus.ASSIGNED) {
            stateMachine.validateTransition(ticket, TicketStatus.IN_PROGRESS, null, actorUsername);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ServiceTicket updated = serviceTicketRepository.saveAndFlush(ticket);

        auditChainService.recordChainedEvent(actorUsername, "TICKET_ASSIGNED", assignee.getUsername(),
                "Ticket " + ticket.getTicketNumber() + " assigned to " + assignee.getUsername());

        eventPublisher.publishEvent(new TicketNotificationEvent(
                updated.getId(),
                updated.getTicketNumber(),
                assignee.getUsername(),
                "TICKET_ASSIGNED",
                "Ticket Assigned: " + updated.getTicketNumber(),
                "You have been assigned to ticket " + updated.getTicketNumber()
        ));

        return toDto(updated);
    }

    /**
     * Mutates ticket status according to the 11-State Machine and updates SLA clock.
     */
    @Transactional
    public ServiceTicketDTO updateStatus(Long ticketId, TicketStatusUpdateRequestDTO request, String actorUsername) {
        if (request == null || request.getStatus() == null) {
            throw new BusinessRuleViolationException("Target status is required.");
        }

        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        // Optimistic locking verification
        if (request.getVersion() != null && !ticket.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockingConflictException("The ticket was updated by another user. Please refresh and retry.");
        }

        if (!ticketSecurity.canChangeStatus(ticket, actorUsername)) {
            throw new UnauthorizedScopeException("User [" + actorUsername + "] is not authorized to alter the state of ticket [" + ticket.getTicketNumber() + "]");
        }

        TicketStatus target = request.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);

        stateMachine.validateTransition(ticket, target, request.getReason(), actorUsername);

        // SLA clock management
        if (target == TicketStatus.ON_HOLD) {
            ticket.pauseSlaClock(now);
        } else if (ticket.getStatus() == TicketStatus.ON_HOLD && target == TicketStatus.IN_PROGRESS) {
            ticket.resumeSlaClock(now);
        } else if (target == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        } else if (target == TicketStatus.CLOSED) {
            ticket.setClosedAt(now);
        }

        ticket.setStatus(target);
        ServiceTicket updated = serviceTicketRepository.saveAndFlush(ticket);

        auditChainService.recordChainedEvent(actorUsername, "TICKET_STATUS_CHANGED", ticket.getRequester().getUsername(),
                "Ticket " + ticket.getTicketNumber() + " shifted to " + target + (request.getReason() != null ? " Reason: " + request.getReason() : ""));

        String eventType = (target == TicketStatus.RESOLVED) ? "TICKET_RESOLVED" : "STATUS_CHANGED";
        String notifTitle = (target == TicketStatus.RESOLVED)
                ? "Ticket Resolved: " + updated.getTicketNumber()
                : "Ticket " + updated.getTicketNumber() + " Status: " + target;
        String notifMessage = (target == TicketStatus.RESOLVED)
                ? "Your service ticket " + updated.getTicketNumber() + " has been marked as RESOLVED."
                : "Ticket status has been updated to " + target;

        eventPublisher.publishEvent(new TicketNotificationEvent(
                updated.getId(),
                updated.getTicketNumber(),
                ticket.getRequester().getUsername(),
                eventType,
                notifTitle,
                notifMessage
        ));

        return toDto(updated);
    }

    /**
     * Allows requester to cancel their own ticket prior to active specialist work.
     */
    @Transactional
    public ServiceTicketDTO cancelTicket(Long ticketId, String reason, Long version, String requesterUsername) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (version != null && !ticket.getVersion().equals(version)) {
            throw new OptimisticLockingConflictException("Ticket concurrency collision. Please refresh.");
        }

        if (!ticketSecurity.canCancelTicket(ticket, requesterUsername)) {
            throw new UnauthorizedScopeException("Only the ticket requester or administrator can cancel this ticket.");
        }

        stateMachine.validateTransition(ticket, TicketStatus.CANCELLED, reason, requesterUsername);
        ticket.setStatus(TicketStatus.CANCELLED);

        ServiceTicket updated = serviceTicketRepository.saveAndFlush(ticket);

        auditChainService.recordChainedEvent(requesterUsername, "TICKET_CANCELLED", requesterUsername,
                "Ticket " + ticket.getTicketNumber() + " cancelled by requester. Reason: " + reason);

        if (updated.getAssignee() != null
                && !updated.getAssignee().getUsername().equalsIgnoreCase(requesterUsername)) {
            eventPublisher.publishEvent(new TicketNotificationEvent(
                    updated.getId(),
                    updated.getTicketNumber(),
                    updated.getAssignee().getUsername(),
                    "TICKET_CANCELLED",
                    "Ticket Cancelled: " + updated.getTicketNumber(),
                    "Ticket " + updated.getTicketNumber() + " was cancelled by the requester."
            ));
        }

        return toDto(updated);
    }

    /**
     * Reopens a resolved ticket within the 48-business-hour dispute window.
     */
    @Transactional
    public ServiceTicketDTO reopenTicket(Long ticketId, String reason, Long version, String requesterUsername) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (version != null && !ticket.getVersion().equals(version)) {
            throw new OptimisticLockingConflictException("Ticket concurrency collision. Please refresh.");
        }

        if (!ticketSecurity.canCancelTicket(ticket, requesterUsername)) {
            throw new UnauthorizedScopeException("Only the ticket requester can reopen this ticket.");
        }

        stateMachine.validateTransition(ticket, TicketStatus.IN_PROGRESS, reason, requesterUsername);
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        ServiceTicket updated = serviceTicketRepository.saveAndFlush(ticket);

        auditChainService.recordChainedEvent(requesterUsername, "TICKET_REOPENED", requesterUsername,
                "Ticket " + ticket.getTicketNumber() + " reopened within dispute window. Reason: " + reason);

        if (updated.getAssignee() != null
                && !updated.getAssignee().getUsername().equalsIgnoreCase(requesterUsername)) {
            eventPublisher.publishEvent(new TicketNotificationEvent(
                    updated.getId(),
                    updated.getTicketNumber(),
                    updated.getAssignee().getUsername(),
                    "TICKET_REOPENED",
                    "Ticket Reopened: " + updated.getTicketNumber(),
                    "Ticket " + updated.getTicketNumber() + " was reopened by the requester and returned to IN_PROGRESS."
            ));
        }

        return toDto(updated);
    }

    /**
     * Appends a comment to a ticket thread.
     */
    @Transactional
    public TicketCommentDTO addComment(Long ticketId, TicketCommentRequestDTO request, String authorUsername) {
        if (request == null || request.getCommentText() == null || request.getCommentText().trim().isBlank()) {
            throw new BusinessRuleViolationException("Comment text cannot be empty.");
        }

        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        UserAccount author = userAccountRepo.findByUsername(authorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authorUsername));

        if (!ticketSecurity.canViewTicket(ticket, authorUsername)) {
            throw new UnauthorizedScopeException("User is not authorized to comment on this ticket.");
        }

        boolean isInternal = Boolean.TRUE.equals(request.getIsInternal());
        if (isInternal && !(ticketSecurity.isAdmin(authorUsername) || ticketSecurity.isDepartmentResolver(ticket, authorUsername))) {
            throw new UnauthorizedScopeException("Only authorized department specialists or administrators may post internal notes.");
        }

        TicketComment comment = new TicketComment(
                ticket,
                author,
                request.getCommentText().trim(),
                isInternal
        );
        comment.setCreatedAt(LocalDateTime.now(clock));

        TicketComment saved = ticketCommentRepository.save(comment);

        auditChainService.recordChainedEvent(authorUsername, "COMMENT_ADDED", ticket.getTicketNumber(),
                "Added " + (saved.isInternal() ? "internal " : "public ") + "comment to ticket " + ticket.getTicketNumber());

        return toDto(saved);
    }

    private String generateTicketNumber() {
        String ticketNumber;
        do {
            ticketNumber = "TKT-2026-" + String.format("%06d", TICKET_SEQ.incrementAndGet());
        } while (serviceTicketRepository.existsByTicketNumber(ticketNumber));
        return ticketNumber;
    }

    public ServiceTicketDTO toDto(ServiceTicket ticket) {
        ServiceTicketDTO dto = new ServiceTicketDTO();
        dto.setId(ticket.getId());
        dto.setTicketNumber(ticket.getTicketNumber());
        if (ticket.getCatalogItem() != null) {
            dto.setCatalogItemId(ticket.getCatalogItem().getId());
            dto.setCatalogItemTitle(ticket.getCatalogItem().getTitle());
        }
        if (ticket.getRequester() != null) {
            dto.setRequesterId(ticket.getRequester().getId());
            dto.setRequesterUsername(ticket.getRequester().getUsername());
        }
        if (ticket.getAssignee() != null) {
            dto.setAssigneeId(ticket.getAssignee().getId());
            dto.setAssigneeUsername(ticket.getAssignee().getUsername());
        }
        dto.setDepartment(ticket.getDepartment());
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setDescription(ticket.getDescription());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setResolvedAt(ticket.getResolvedAt());
        dto.setClosedAt(ticket.getClosedAt());
        dto.setSlaTargetDate(ticket.getSlaTargetDate());
        dto.setTotalPausedMinutes(ticket.getTotalPausedMinutes());
        dto.setVersion(ticket.getVersion());

        // SLA Status calculation
        LocalDateTime now = LocalDateTime.now(clock);
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            dto.setSlaStatus("SLA_OK");
        } else if (ticket.getSlaTargetDate() != null) {
            if (now.isAfter(ticket.getSlaTargetDate())) {
                dto.setSlaStatus("SLA_BREACHED");
            } else {
                dto.setSlaStatus("SLA_OK");
            }
        }
        return dto;
    }

    public TicketCommentDTO toDto(TicketComment comment) {
        TicketCommentDTO dto = new TicketCommentDTO();
        dto.setId(comment.getId());
        if (comment.getTicket() != null) {
            dto.setTicketId(comment.getTicket().getId());
        }
        if (comment.getAuthor() != null) {
            dto.setAuthorId(comment.getAuthor().getId());
            dto.setAuthorUsername(comment.getAuthor().getUsername());
        }
        dto.setCommentText(comment.getCommentText());
        dto.setIsInternal(comment.getIsInternal());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}

