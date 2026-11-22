package com.examly.springapp.service;

import com.examly.springapp.dto.ApprovalDecisionRequestDTO;
import com.examly.springapp.dto.ApprovalTaskDTO;
import com.examly.springapp.exception.*;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.ApprovalTaskRepository;
import com.examly.springapp.repository.ProfileRepo;
import com.examly.springapp.repository.ServiceTicketRepository;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.security.TicketSecurityEvaluator;
import com.examly.springapp.workflow.TicketStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing ticket approval lifecycles, multi-level escalation,
 * and server-side anti-self-approval enforcement.
 */
@Service
public class ApprovalService {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final UserAccountRepo userAccountRepo;
    private final ProfileRepo profileRepo;
    private final TicketStateMachine stateMachine;
    private final AuditChainService auditChainService;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketSecurityEvaluator ticketSecurity;
    private final Clock clock;

    @Autowired
    public ApprovalService(ApprovalTaskRepository approvalTaskRepository,
                           ServiceTicketRepository serviceTicketRepository,
                           UserAccountRepo userAccountRepo,
                           ProfileRepo profileRepo,
                           TicketStateMachine stateMachine,
                           AuditChainService auditChainService,
                           ApplicationEventPublisher eventPublisher,
                           TicketSecurityEvaluator ticketSecurity,
                           Clock clock) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.serviceTicketRepository = serviceTicketRepository;
        this.userAccountRepo = userAccountRepo;
        this.profileRepo = profileRepo;
        this.stateMachine = stateMachine;
        this.auditChainService = auditChainService;
        this.eventPublisher = eventPublisher;
        this.ticketSecurity = ticketSecurity;
        this.clock = clock;
    }

    /**
     * Creates initial approval task for a newly submitted ticket based on service catalog requirements.
     */
    @Transactional
    public void createApprovalTasksForTicket(ServiceTicket ticket) {
        if (!Boolean.TRUE.equals(ticket.getCatalogItem().getRequiresManagerApproval())) {
            // No approval required: ticket can immediately be queued for assignment
            stateMachine.validateTransition(ticket, TicketStatus.ASSIGNED, null, "system");
            ticket.setStatus(TicketStatus.ASSIGNED);
            serviceTicketRepository.saveAndFlush(ticket);
            auditChainService.recordChainedEvent("system", "TICKET_QUEUED", ticket.getRequester().getUsername(),
                    "Ticket " + ticket.getTicketNumber() + " queued for assignment without approval requirement.");
            return;
        }

        // 1. Resolve manager identity
        UserAccount approver = resolveApproverForRequester(ticket.getRequester(), ticket.getDepartment(), 1);

        // 2. Anti-self-approval check: Requester cannot be assigned as their own approver
        if (approver.getId().equals(ticket.getRequester().getId()) ||
            approver.getUsername().equalsIgnoreCase(ticket.getRequester().getUsername())) {
            approver = resolveEscalationApprover(ticket.getDepartment(), approver.getUsername());
        }

        // 3. Create Stage 1 Approval Task
        ApprovalTask task = new ApprovalTask(ticket, approver, 1);
        approvalTaskRepository.saveAndFlush(task);

        // 4. Update ticket state to PENDING_APPROVAL
        stateMachine.validateTransition(ticket, TicketStatus.PENDING_APPROVAL, null, "system");
        ticket.setStatus(TicketStatus.PENDING_APPROVAL);
        serviceTicketRepository.saveAndFlush(ticket);

        // 5. Synchronous audit entry
        auditChainService.recordChainedEvent("system", "APPROVAL_TASK_CREATED", approver.getUsername(),
                "Stage 1 approval task assigned to " + approver.getUsername() + " for ticket " + ticket.getTicketNumber());

        // 6. Post-commit notification
        eventPublisher.publishEvent(new TicketNotificationEvent(
                ticket.getId(),
                ticket.getTicketNumber(),
                approver.getUsername(),
                "APPROVAL_REQUIRED",
                "Approval Required for Ticket " + ticket.getTicketNumber(),
                "Ticket " + ticket.getTicketNumber() + " submitted by " + ticket.getRequester().getUsername() + " requires your approval.",
                "APPROVAL",
                task.getId(),
                "APPROVAL_STAGE1_" + ticket.getId()
        ));
    }

    /**
     * Records an approval or rejection decision.
     */
    @Transactional
    public ApprovalTaskDTO decideApproval(Long taskId, ApprovalDecisionRequestDTO request, String actorUsername) {
        if (request == null || request.getDecision() == null) {
            throw new BusinessRuleViolationException("Decision (APPROVE or REJECT) is required.");
        }

        ApprovalTask task = approvalTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval task not found with ID: " + taskId));

        ServiceTicket ticket = task.getTicket();

        // 1. Concurrency Check via JPA @Version
        if (request.getVersion() != null && !task.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockingConflictException("The approval task was modified by another concurrent user.");
        }

        // 2. Reject duplicate decision
        if (task.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleViolationException("Approval task has already been decided: " + task.getStatus());
        }

        // 3. Anti-Self-Approval Enforcement (BR-010)
        if (ticket.getRequester().getUsername().equalsIgnoreCase(actorUsername)) {
            throw new AntiSelfApprovalException(
                    "Anti-Self-Approval Violation: Requesters are strictly prohibited from approving their own service tickets.");
        }

        // 4. Authorization Check: Actor must be the assigned approver or System Admin
        if (!task.getApprover().getUsername().equalsIgnoreCase(actorUsername) && !ticketSecurity.isAdmin(actorUsername)) {
            throw new UnauthorizedScopeException("User [" + actorUsername + "] is not the designated approver for task ID: " + taskId);
        }

        // 5. Reason Validation (BR-007)
        if (request.getDecision() == ApprovalStatus.REJECTED) {
            if (request.getReason() == null || request.getReason().trim().length() < 15) {
                throw new BusinessRuleViolationException("Rejecting an approval requires a detailed reason of at least 15 characters.");
            }
        }

        LocalDateTime now = LocalDateTime.now(clock);
        task.decide(request.getDecision(), request.getReason(), now);
        approvalTaskRepository.saveAndFlush(task);

        if (request.getDecision() == ApprovalStatus.REJECTED) {
            stateMachine.validateTransition(ticket, TicketStatus.REJECTED, request.getReason(), actorUsername);
            ticket.setStatus(TicketStatus.REJECTED);
            serviceTicketRepository.saveAndFlush(ticket);

            auditChainService.recordChainedEvent(actorUsername, "APPROVAL_REJECTED", ticket.getRequester().getUsername(),
                    "Approval Stage " + task.getApprovalStage() + " rejected with reason: " + request.getReason());

            eventPublisher.publishEvent(new TicketNotificationEvent(
                    ticket.getId(),
                    ticket.getTicketNumber(),
                    ticket.getRequester().getUsername(),
                    "APPROVAL_REJECTED",
                    "Ticket " + ticket.getTicketNumber() + " Rejected",
                    "Your ticket was rejected at Stage " + task.getApprovalStage() + ". Reason: " + request.getReason()
            ));
        } else {
            // APPROVED
            boolean requiresSecondary = (task.getApprovalStage() == 1) &&
                    Boolean.TRUE.equals(ticket.getCatalogItem().getRequiresSecondaryApproval());

            if (requiresSecondary) {
                // Advance to Stage 2: PENDING_SECONDARY_APPROVAL
                stateMachine.validateTransition(ticket, TicketStatus.PENDING_SECONDARY_APPROVAL, null, actorUsername);
                ticket.setStatus(TicketStatus.PENDING_SECONDARY_APPROVAL);
                serviceTicketRepository.saveAndFlush(ticket);

                UserAccount stage2Approver = resolveEscalationApprover(ticket.getDepartment(), actorUsername);
                ApprovalTask stage2Task = new ApprovalTask(ticket, stage2Approver, 2);
                approvalTaskRepository.saveAndFlush(stage2Task);

                auditChainService.recordChainedEvent(actorUsername, "APPROVAL_STAGE1_APPROVED", ticket.getRequester().getUsername(),
                        "Stage 1 approved: " + request.getReason() + ". Escalated to Stage 2 approver " + stage2Approver.getUsername());

                eventPublisher.publishEvent(new TicketNotificationEvent(
                        ticket.getId(),
                        ticket.getTicketNumber(),
                        stage2Approver.getUsername(),
                        "APPROVAL_REQUIRED",
                        "Secondary Approval Required for Ticket " + ticket.getTicketNumber(),
                        "Ticket " + ticket.getTicketNumber() + " requires secondary approval.",
                        "APPROVAL",
                        stage2Task.getId(),
                        "APPROVAL_STAGE2_" + ticket.getId()
                ));
            } else {
                // Final Approval Granted: Advance to ASSIGNED
                stateMachine.validateTransition(ticket, TicketStatus.ASSIGNED, null, actorUsername);
                ticket.setStatus(TicketStatus.ASSIGNED);
                serviceTicketRepository.saveAndFlush(ticket);

                auditChainService.recordChainedEvent(actorUsername, "APPROVAL_FINAL_APPROVED", ticket.getRequester().getUsername(),
                        "Final approval granted at stage " + task.getApprovalStage() + " by " + actorUsername);

                eventPublisher.publishEvent(new TicketNotificationEvent(
                        ticket.getId(),
                        ticket.getTicketNumber(),
                        ticket.getRequester().getUsername(),
                        "TICKET_APPROVED",
                        "Ticket " + ticket.getTicketNumber() + " Approved",
                        "Your ticket has received final approval and is queued for specialist assignment."
                ));
            }
        }

        return toDto(task);
    }

    @Transactional(readOnly = true)
    public List<ApprovalTaskDTO> getPendingApprovalsForUser(String username) {
        return approvalTaskRepository.findPendingByApproverUsername(username, ApprovalStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApprovalTaskDTO> getApprovalHistoryForTicket(Long ticketId, String actorUsername) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        List<ApprovalTask> tasks = approvalTaskRepository.findByTicketIdOrderByApprovalStageAsc(ticketId);
        boolean isAssignedApprover = tasks.stream()
                .anyMatch(t -> t.getApprover() != null && t.getApprover().getUsername().equalsIgnoreCase(actorUsername));

        if (!ticketSecurity.canViewTicket(ticket, actorUsername) && !isAssignedApprover) {
            throw new UnauthorizedScopeException("You are not authorized to view approval history for this ticket.");
        }

        return tasks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private UserAccount resolveApproverForRequester(UserAccount requester, String department, int stage) {
        Optional<Profile> profileOpt = profileRepo.findByUserId(requester.getId());
        if (profileOpt.isPresent() && profileOpt.get().getReportingManager() != null) {
            String managerUsername = profileOpt.get().getReportingManager().trim();
            Optional<UserAccount> managerOpt = userAccountRepo.findByUsername(managerUsername);
            if (managerOpt.isPresent()) {
                return managerOpt.get();
            }
        }
        return resolveEscalationApprover(department, requester.getUsername());
    }

    private UserAccount resolveEscalationApprover(String department, String excludeUsername) {
        // Look up Department Head first
        List<Profile> deptProfiles = profileRepo.findByDepartment(department);
        for (Profile p : deptProfiles) {
            if (p.getUser() != null && !p.getUser().getUsername().equalsIgnoreCase(excludeUsername)) {
                if (ticketSecurity.hasRole(p.getUser().getUsername(), "ROLE_DEPT_HEAD")) {
                    return p.getUser();
                }
            }
        }
        // Secondary fallback to another department Manager
        for (Profile p : deptProfiles) {
            if (p.getUser() != null && !p.getUser().getUsername().equalsIgnoreCase(excludeUsername)) {
                if (ticketSecurity.hasRole(p.getUser().getUsername(), "ROLE_MANAGER")) {
                    return p.getUser();
                }
            }
        }
        // Fallback to any active administrator
        return userAccountRepo.findAll().stream()
                .filter(u -> !u.getUsername().equalsIgnoreCase(excludeUsername) && ticketSecurity.isAdmin(u.getUsername()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("No eligible approver or department head found for approval routing."));
    }

    public ApprovalTaskDTO toDto(ApprovalTask task) {
        ApprovalTaskDTO dto = new ApprovalTaskDTO();
        dto.setId(task.getId());
        if (task.getTicket() != null) {
            dto.setTicketId(task.getTicket().getId());
            dto.setTicketNumber(task.getTicket().getTicketNumber());
            dto.setNewTicketStatus(task.getTicket().getStatus());
            if (task.getTicket().getRequester() != null) {
                dto.setRequesterUsername(task.getTicket().getRequester().getUsername());
            }
        }
        if (task.getApprover() != null) {
            dto.setApproverUsername(task.getApprover().getUsername());
        }
        dto.setApprovalStage(task.getApprovalStage());
        dto.setStatus(task.getStatus());
        dto.setDecisionReason(task.getDecisionReason());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setDecidedAt(task.getDecidedAt());
        dto.setVersion(task.getVersion());
        return dto;
    }
}

