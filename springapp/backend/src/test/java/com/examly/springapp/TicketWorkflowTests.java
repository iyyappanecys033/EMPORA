package com.examly.springapp;

import com.examly.springapp.dto.*;
import com.examly.springapp.exception.*;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.ApprovalService;
import com.examly.springapp.service.AuditChainService;
import com.examly.springapp.service.TicketCommandService;
import com.examly.springapp.service.TicketQueryService;
import com.examly.springapp.workflow.BusinessCalendar;
import com.examly.springapp.workflow.TicketStateMachine;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TicketWorkflowTests {

    @Autowired
    private TicketCommandService commandService;

    @Autowired
    private TicketQueryService queryService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private AuditChainService auditChainService;

    @Autowired
    private BusinessCalendar businessCalendar;

    @Autowired
    private TicketStateMachine stateMachine;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private ServiceTicketRepository serviceTicketRepository;

    @Autowired
    private ApprovalTaskRepository approvalTaskRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private InAppAlertRepository inAppAlertRepository;

    @Autowired
    private AuditLogRepo auditLogRepo;

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRoleMappingRepo userRoleMappingRepo;

    @Autowired
    private ProfileRepo profileRepo;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private UserAccount empAlice;
    private UserAccount mgrBob;
    private UserAccount headCarol;
    private UserAccount resDave;
    private UserAccount adminEve;
    private ServiceCatalogItem simpleItem;
    private ServiceCatalogItem approvalItem;
    private ServiceCatalogItem twoStageItem;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // Ensure roles exist
        Role rEmp = getOrCreateRole("ROLE_EMPLOYEE");
        Role rMgr = getOrCreateRole("ROLE_MANAGER");
        Role rHead = getOrCreateRole("ROLE_DEPT_HEAD");
        Role rRes = getOrCreateRole("ROLE_RESOLVER");
        Role rAdmin = getOrCreateRole("ROLE_ADMIN");

        // Create test identities
        empAlice = getOrCreateUser("wf_emp_alice", rEmp);
        mgrBob = getOrCreateUser("wf_mgr_bob", rMgr);
        headCarol = getOrCreateUser("wf_head_carol", rHead);
        resDave = getOrCreateUser("wf_res_dave", rRes);
        adminEve = getOrCreateUser("wf_admin_eve", rAdmin);

        // Profiles linking reporting manager and departments
        getOrCreateProfile(empAlice, "EMP-WF-01", "Alice", "IT", "wf_mgr_bob");
        getOrCreateProfile(mgrBob, "EMP-WF-02", "Bob", "IT", "wf_head_carol");
        getOrCreateProfile(headCarol, "EMP-WF-03", "Carol", "IT", null);
        getOrCreateProfile(resDave, "EMP-WF-04", "Dave", "IT", null);
        getOrCreateProfile(adminEve, "EMP-WF-05", "Eve", "ADMIN", null);

        // Create service catalog items
        simpleItem = serviceCatalogRepository.findByItemCode("WF-PASS-RESET")
                .orElseGet(() -> serviceCatalogRepository.save(new ServiceCatalogItem(
                        "WF-PASS-RESET", "Password Reset", "Security", "IT",
                        "Standard self-service password reset", BigDecimal.ZERO, 4,
                        false, false, true
                )));

        approvalItem = serviceCatalogRepository.findByItemCode("WF-MONITOR")
                .orElseGet(() -> serviceCatalogRepository.save(new ServiceCatalogItem(
                        "WF-MONITOR", "External 4K Monitor", "Hardware", "IT",
                        "27-inch 4K developer display", new BigDecimal("450.00"), 24,
                        true, false, true
                )));

        twoStageItem = serviceCatalogRepository.findByItemCode("WF-SERVER")
                .orElseGet(() -> serviceCatalogRepository.save(new ServiceCatalogItem(
                        "WF-SERVER", "Dedicated GPU Compute Server", "Cloud", "IT",
                        "Dedicated machine learning server instance", new BigDecimal("8500.00"), 48,
                        true, true, true
                )));
    }

    // =========================================================================
    // 1. TICKET LIFECYCLE STATE MACHINE TESTS
    // =========================================================================

    @Test
    @Order(0)
    @DisplayName("Ticket numbers skip values already persisted before the service starts")
    void ticketNumberGenerationSkipsPersistedNumbers() {
        CreateTicketRequestDTO request = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Verify unique ticket numbering after restart."
        );
        ServiceTicketDTO first = commandService.createTicket(request, empAlice.getUsername());
        AtomicLong ticketSequence = (AtomicLong) ReflectionTestUtils.getField(TicketCommandService.class, "TICKET_SEQ");
        long sequenceBeforeSecondCreate = ticketSequence.get();

        try {
            long firstSequence = Long.parseLong(first.getTicketNumber().substring("TKT-2026-".length()));
            ticketSequence.set(firstSequence - 1);

            ServiceTicketDTO second = commandService.createTicket(request, empAlice.getUsername());

            assertNotEquals(first.getTicketNumber(), second.getTicketNumber());
            assertTrue(serviceTicketRepository.existsByTicketNumber(second.getTicketNumber()));
        } finally {
            ticketSequence.set(Math.max(sequenceBeforeSecondCreate, ticketSequence.get()));
        }
    }

    @Test
    @Order(1)
    @DisplayName("Stage B: Valid lifecycle transitions advance correctly through full 11-state machine")
    void test1_ValidLifecycleStateTransitions() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Reset my domain network password please."
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());
        assertNotNull(created.getId());
        assertEquals(TicketStatus.ASSIGNED, created.getStatus(), "Non-approval item should bypass directly to ASSIGNED");

        // 1. ASSIGNED -> IN_PROGRESS via specialist assign
        ServiceTicketDTO inProgress = commandService.assignTicket(created.getId(), resDave.getId(), resDave.getUsername());
        assertEquals(TicketStatus.IN_PROGRESS, inProgress.getStatus());
        assertEquals(resDave.getId(), inProgress.getAssigneeId());

        // 2. IN_PROGRESS -> ON_HOLD with mandatory reason (>= 15 chars)
        TicketStatusUpdateRequestDTO holdReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.ON_HOLD, "Awaiting hardware shipment arrival from logistics vendor", inProgress.getVersion()
        );
        ServiceTicketDTO onHold = commandService.updateStatus(created.getId(), holdReq, resDave.getUsername());
        assertEquals(TicketStatus.ON_HOLD, onHold.getStatus());

        // 3. ON_HOLD -> IN_PROGRESS (resume work)
        TicketStatusUpdateRequestDTO resumeReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.IN_PROGRESS, null, onHold.getVersion()
        );
        ServiceTicketDTO resumed = commandService.updateStatus(created.getId(), resumeReq, resDave.getUsername());
        assertEquals(TicketStatus.IN_PROGRESS, resumed.getStatus());

        // 4. IN_PROGRESS -> RESOLVED
        TicketStatusUpdateRequestDTO resolveReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.RESOLVED, null, resumed.getVersion()
        );
        ServiceTicketDTO resolved = commandService.updateStatus(created.getId(), resolveReq, resDave.getUsername());
        assertEquals(TicketStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());

        // 5. RESOLVED -> CLOSED
        TicketStatusUpdateRequestDTO closeReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.CLOSED, null, resolved.getVersion()
        );
        ServiceTicketDTO closed = commandService.updateStatus(created.getId(), closeReq, resDave.getUsername());
        assertEquals(TicketStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getClosedAt());
    }

    @Test
    @Order(2)
    @DisplayName("Stage B: Illegal state transitions are rejected with IllegalStateTransitionException")
    void test2_IllegalStateTransitionsRejected() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.LOW, "Testing illegal transition guards"
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());

        // Attempt ASSIGNED -> CLOSED directly (bypassing IN_PROGRESS and RESOLVED)
        TicketStatusUpdateRequestDTO illegalReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.CLOSED, null, created.getVersion()
        );
        assertThrows(IllegalStateTransitionException.class, () -> {
            commandService.updateStatus(created.getId(), illegalReq, resDave.getUsername());
        });

        // Attempt same state transition (ASSIGNED -> ASSIGNED)
        TicketStatusUpdateRequestDTO sameReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.ASSIGNED, null, created.getVersion()
        );
        assertThrows(IllegalStateTransitionException.class, () -> {
            commandService.updateStatus(created.getId(), sameReq, resDave.getUsername());
        });
    }

    @Test
    @Order(3)
    @DisplayName("Stage B: Transitions to ON_HOLD require detailed reason of at least 15 characters (BR-007)")
    void test3_MandatoryReasonLengthEnforcement() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Testing reason minimum validation"
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());
        ServiceTicketDTO inProg = commandService.assignTicket(created.getId(), resDave.getId(), resDave.getUsername());

        // Reason < 15 chars (e.g. "Waiting vendor" = 14 chars)
        TicketStatusUpdateRequestDTO shortReasonReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.ON_HOLD, "Waiting vendor", inProg.getVersion()
        );
        assertThrows(BusinessRuleViolationException.class, () -> {
            commandService.updateStatus(created.getId(), shortReasonReq, resDave.getUsername());
        });
    }

    @Test
    @Order(4)
    @DisplayName("Stage B: Requester can cancel before active work, but cannot cancel when IN_PROGRESS (ADR-007)")
    void test4_RequesterCancellationBoundary() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.LOW, "Ticket to test cancellation boundary"
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());

        // When in ASSIGNED, requester can cancel with >= 15 chars reason
        ServiceTicketDTO cancelled = commandService.cancelTicket(
                created.getId(), "No longer needed due to project scope reallocation", created.getVersion(), empAlice.getUsername()
        );
        assertEquals(TicketStatus.CANCELLED, cancelled.getStatus());

        // Create new ticket and move to IN_PROGRESS
        ServiceTicketDTO t2 = commandService.createTicket(createReq, empAlice.getUsername());
        ServiceTicketDTO inProg2 = commandService.assignTicket(t2.getId(), resDave.getId(), resDave.getUsername());

        // In IN_PROGRESS, unilateral requester cancellation is blocked!
        assertThrows(IllegalStateTransitionException.class, () -> {
            commandService.cancelTicket(t2.getId(), "Unilateral cancellation attempt while in progress", inProg2.getVersion(), empAlice.getUsername());
        });
    }

    @Test
    @Order(5)
    @DisplayName("Stage B: Reopening a resolved ticket succeeds within 48 business hours (including >50 wall-clock hours), fails after 48 business hours elapse")
    void test5_ReopenWithinDisputeWindow() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.HIGH, "Ticket for dispute window test"
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());
        ServiceTicketDTO inProg = commandService.assignTicket(created.getId(), resDave.getId(), resDave.getUsername());

        TicketStatusUpdateRequestDTO resolveReq = new TicketStatusUpdateRequestDTO(TicketStatus.RESOLVED, null, inProg.getVersion());
        ServiceTicketDTO resolved = commandService.updateStatus(created.getId(), resolveReq, resDave.getUsername());

        // Reopen with valid reason immediately within 48 business hours
        ServiceTicketDTO reopened = commandService.reopenTicket(
                resolved.getId(), "Issue recurred immediately after software reboot check", resolved.getVersion(), empAlice.getUsername()
        );
        assertEquals(TicketStatus.IN_PROGRESS, reopened.getStatus());

        // 50 wall-clock hours ago is still WELL within 48 business hours (<= 27 business hours) -> MUST SUCCEED
        final Long verWithinBizWindow = transactionTemplate.execute(status -> {
            ServiceTicket t = serviceTicketRepository.findById(resolved.getId()).orElseThrow();
            t.setStatus(TicketStatus.RESOLVED);
            t.setResolvedAt(businessCalendar.nowInBusinessZone().minusHours(50));
            ServiceTicket saved = serviceTicketRepository.saveAndFlush(t);
            return saved.getVersion();
        });

        ServiceTicketDTO reopenedAfter50WallHours = commandService.reopenTicket(
                resolved.getId(), "Reopening after 50 wall-clock hours is valid within 48 business hours",
                verWithinBizWindow, empAlice.getUsername()
        );
        assertEquals(TicketStatus.IN_PROGRESS, reopenedAfter50WallHours.getStatus());

        // Simulate genuinely expired ticket: 14 calendar days ago (>= 72 business hours > 48 business hours) -> MUST FAIL
        final Long currentVer = transactionTemplate.execute(status -> {
            ServiceTicket t = serviceTicketRepository.findById(resolved.getId()).orElseThrow();
            t.setStatus(TicketStatus.RESOLVED);
            t.setResolvedAt(businessCalendar.nowInBusinessZone().minusDays(14));
            ServiceTicket saved = serviceTicketRepository.saveAndFlush(t);
            return saved.getVersion();
        });

        assertThrows(BusinessRuleViolationException.class, () -> {
            commandService.reopenTicket(resolved.getId(), "Trying to reopen after 48 business hours have elapsed", currentVer, empAlice.getUsername());
        });
    }

    // =========================================================================
    // 2. APPROVAL WORKFLOW & ANTI-SELF-APPROVAL TESTS
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Stage C: Single-level approval creates pending task assigned to manager and approves to ASSIGNED")
    void test6_SingleLevelApprovalWorkflow() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                approvalItem.getId(), "IT", TicketPriority.HIGH, "Requesting 4K external display"
        );
        ServiceTicketDTO ticket = commandService.createTicket(createReq, empAlice.getUsername());
        assertEquals(TicketStatus.PENDING_APPROVAL, ticket.getStatus());

        List<ApprovalTaskDTO> pending = approvalService.getPendingApprovalsForUser(mgrBob.getUsername());
        assertTrue(pending.stream().anyMatch(a -> a.getTicketId().equals(ticket.getId())));

        ApprovalTaskDTO task = pending.stream().filter(a -> a.getTicketId().equals(ticket.getId())).findFirst().orElseThrow();
        assertEquals(1, task.getApprovalStage());

        // Manager Bob approves
        ApprovalDecisionRequestDTO approveReq = new ApprovalDecisionRequestDTO(
                ApprovalStatus.APPROVED, "Approved hardware request for project needs", task.getVersion()
        );
        ApprovalTaskDTO decided = approvalService.decideApproval(task.getId(), approveReq, mgrBob.getUsername());
        assertEquals(ApprovalStatus.APPROVED, decided.getStatus());

        ServiceTicketDTO refreshedTicket = queryService.getTicketById(ticket.getId(), empAlice.getUsername());
        assertEquals(TicketStatus.ASSIGNED, refreshedTicket.getStatus(), "Approved ticket moves to ASSIGNED queue");
    }

    @Test
    @Order(7)
    @DisplayName("Stage C: Anti-Self-Approval strictly blocks requester from approving their own ticket (BR-010)")
    void test7_AntiSelfApprovalEnforced() {
        // Manager Bob submits ticket requiring approval
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                approvalItem.getId(), "IT", TicketPriority.HIGH, "Manager Bob submitting personal hardware request"
        );
        ServiceTicketDTO ticket = commandService.createTicket(createReq, mgrBob.getUsername());

        // Approval task is escalated to Dept Head Carol because Bob cannot approve own ticket
        List<ApprovalTaskDTO> carolPending = approvalService.getPendingApprovalsForUser(headCarol.getUsername());
        ApprovalTaskDTO task = carolPending.stream().filter(a -> a.getTicketId().equals(ticket.getId())).findFirst().orElseThrow();

        // Even if Bob attempts to call decideApproval, AntiSelfApprovalException is thrown!
        ApprovalDecisionRequestDTO selfApproveReq = new ApprovalDecisionRequestDTO(
                ApprovalStatus.APPROVED, "I approve my own request immediately", task.getVersion()
        );
        assertThrows(AntiSelfApprovalException.class, () -> {
            approvalService.decideApproval(task.getId(), selfApproveReq, mgrBob.getUsername());
        });
    }

    @Test
    @Order(8)
    @DisplayName("Stage C: Two-stage approval escalates through Stage 1 (Manager) and Stage 2 (Dept Head)")
    void test8_TwoStageApprovalWorkflow() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                twoStageItem.getId(), "IT", TicketPriority.CRITICAL, "Requesting high value GPU cloud compute node"
        );
        ServiceTicketDTO ticket = commandService.createTicket(createReq, empAlice.getUsername());
        assertEquals(TicketStatus.PENDING_APPROVAL, ticket.getStatus());

        // Stage 1: Manager Bob approves
        List<ApprovalTaskDTO> bobTasks = approvalService.getPendingApprovalsForUser(mgrBob.getUsername());
        ApprovalTaskDTO stage1 = bobTasks.stream().filter(a -> a.getTicketId().equals(ticket.getId())).findFirst().orElseThrow();

        approvalService.decideApproval(stage1.getId(), new ApprovalDecisionRequestDTO(
                ApprovalStatus.APPROVED, "Stage 1 approved: project needs justified", stage1.getVersion()
        ), mgrBob.getUsername());

        // Verify ticket shifted to PENDING_SECONDARY_APPROVAL
        ServiceTicketDTO stage2Ticket = queryService.getTicketById(ticket.getId(), empAlice.getUsername());
        assertEquals(TicketStatus.PENDING_SECONDARY_APPROVAL, stage2Ticket.getStatus());

        // Stage 2: Dept Head Carol approves
        List<ApprovalTaskDTO> carolTasks = approvalService.getPendingApprovalsForUser(headCarol.getUsername());
        ApprovalTaskDTO stage2 = carolTasks.stream().filter(a -> a.getTicketId().equals(ticket.getId())).findFirst().orElseThrow();
        assertEquals(2, stage2.getApprovalStage());

        approvalService.decideApproval(stage2.getId(), new ApprovalDecisionRequestDTO(
                ApprovalStatus.APPROVED, "Stage 2 approved by Dept Head after budget review", stage2.getVersion()
        ), headCarol.getUsername());

        ServiceTicketDTO finalTicket = queryService.getTicketById(ticket.getId(), empAlice.getUsername());
        assertEquals(TicketStatus.ASSIGNED, finalTicket.getStatus());
    }

    @Test
    @Order(9)
    @DisplayName("Stage C: Rejecting an approval requires >= 15 chars reason and transitions ticket to REJECTED")
    void test9_ApprovalRejectionWorkflow() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                approvalItem.getId(), "IT", TicketPriority.LOW, "Requesting extra monitor"
        );
        ServiceTicketDTO ticket = commandService.createTicket(createReq, empAlice.getUsername());

        ApprovalTaskDTO task = approvalService.getPendingApprovalsForUser(mgrBob.getUsername()).stream()
                .filter(a -> a.getTicketId().equals(ticket.getId())).findFirst().orElseThrow();

        // Short reason rejected
        assertThrows(BusinessRuleViolationException.class, () -> {
            approvalService.decideApproval(task.getId(), new ApprovalDecisionRequestDTO(
                    ApprovalStatus.REJECTED, "Budget denied", task.getVersion()
            ), mgrBob.getUsername());
        });

        // Valid rejection reason
        approvalService.decideApproval(task.getId(), new ApprovalDecisionRequestDTO(
                ApprovalStatus.REJECTED, "Hardware budget exceeded for current fiscal quarter", task.getVersion()
        ), mgrBob.getUsername());

        ServiceTicketDTO rejectedTicket = queryService.getTicketById(ticket.getId(), empAlice.getUsername());
        assertEquals(TicketStatus.REJECTED, rejectedTicket.getStatus());
    }

    // =========================================================================
    // 3. ORGANIZATIONAL AUTHORIZATION & RESOURCE SCOPE TESTS
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Stage D: Requester can access own ticket, other employees are blocked (403)")
    void test10_ResourceOwnershipAuthorization() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.LOW, "Confidential employee request"
        );
        ServiceTicketDTO ticket = commandService.createTicket(createReq, empAlice.getUsername());

        // Alice can view own ticket
        assertDoesNotThrow(() -> queryService.getTicketById(ticket.getId(), empAlice.getUsername()));

        // Another employee (e.g. user without role or from another team) is blocked
        UserAccount stranger = getOrCreateUser("wf_stranger_user", roleRepo.findByRoleName("ROLE_EMPLOYEE").orElseThrow());
        getOrCreateProfile(stranger, "EMP-STRANGER", "Stranger", "HR", null);

        assertThrows(UnauthorizedScopeException.class, () -> {
            queryService.getTicketById(ticket.getId(), stranger.getUsername());
        });
    }

    @Test
    @Order(11)
    @DisplayName("Stage D: Resolver can access own department queue, cross-department access is blocked")
    void test11_DepartmentResolverScopeBoundary() {
        // Resolver Dave belongs to IT
        assertDoesNotThrow(() -> {
            queryService.getDepartmentQueue("IT", TicketStatus.ASSIGNED, PageRequest.of(0, 10), resDave.getUsername());
        });

        // Dave attempting to access "HR" department queue is blocked!
        assertThrows(UnauthorizedScopeException.class, () -> {
            queryService.getDepartmentQueue("HR", TicketStatus.ASSIGNED, PageRequest.of(0, 10), resDave.getUsername());
        });
    }

    // =========================================================================
    // 4. SLA CLOCK & BUSINESS CALENDAR TESTS
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Stage E: SLA target date excludes weekends and holidays in Asia/Kolkata timezone")
    void test12_BusinessCalendarSlaCalculations() {
        // Friday at 16:00 (2 hours before 18:00 close)
        LocalDateTime fridayEvening = LocalDateTime.of(2026, 3, 13, 16, 0); // Friday
        // 5 business hours SLA:
        // Friday 16:00-18:00 (2h remaining) -> Sat/Sun skipped -> Monday 09:00-12:00 (3h remaining)
        LocalDateTime expectedDeadline = LocalDateTime.of(2026, 3, 16, 12, 0); // Monday noon

        LocalDateTime actualDeadline = businessCalendar.calculateTargetDate(fridayEvening, 5);
        assertEquals(expectedDeadline, actualDeadline, "SLA must skip weekend and land on Monday 12:00");
    }

    @Test
    @Order(13)
    @DisplayName("Stage E: Placing ticket ON_HOLD pauses SLA clock and resumes with accurate accumulated minutes")
    void test13_SlaPauseAndResumeTracking() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.MEDIUM, "SLA pause tracking test ticket"
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());
        ServiceTicketDTO inProg = commandService.assignTicket(created.getId(), resDave.getId(), resDave.getUsername());

        // Pause
        ServiceTicketDTO onHold = commandService.updateStatus(created.getId(), new TicketStatusUpdateRequestDTO(
                TicketStatus.ON_HOLD, "Awaiting customer diagnostic logs from client", inProg.getVersion()
        ), resDave.getUsername());

        ServiceTicket onHoldTicket = serviceTicketRepository.findById(created.getId()).orElseThrow();
        assertEquals(TicketStatus.ON_HOLD, onHoldTicket.getStatus());
        assertNotNull(onHoldTicket.getLastPausedAt());

        // Resume after simulated pause
        onHoldTicket.setLastPausedAt(LocalDateTime.now().minusMinutes(35));
        ServiceTicket savedOnHold = serviceTicketRepository.saveAndFlush(onHoldTicket);

        commandService.updateStatus(created.getId(), new TicketStatusUpdateRequestDTO(
                TicketStatus.IN_PROGRESS, null, savedOnHold.getVersion()
        ), resDave.getUsername());

        ServiceTicket resumedTicket = serviceTicketRepository.findById(created.getId()).orElseThrow();
        assertEquals(TicketStatus.IN_PROGRESS, resumedTicket.getStatus());
        assertNull(resumedTicket.getLastPausedAt());
        assertTrue(resumedTicket.getTotalPausedMinutes() >= 34 && resumedTicket.getTotalPausedMinutes() <= 36);
    }

    // =========================================================================
    // 5. TRANSACTIONAL CRYPTOGRAPHIC AUDIT CHAIN TESTS
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("Stage F: Audit chain allocates gapless monotonic sequence with SHA-256 hash linkage")
    void test14_AuditChainIntegrityAndVerification() {
        // Record 3 events
        AuditLog e1 = auditChainService.recordChainedEvent("wf_test_actor", "ACTION_A", "target1", "Details for event 1");
        AuditLog e2 = auditChainService.recordChainedEvent("wf_test_actor", "ACTION_B", "target2", "Details for event 2");
        AuditLog e3 = auditChainService.recordChainedEvent("wf_test_actor", "ACTION_C", "target3", "Details for event 3");

        assertNotNull(e1.getSequenceNumber());
        assertEquals(e1.getSequenceNumber() + 1, e2.getSequenceNumber());
        assertEquals(e2.getSequenceNumber() + 1, e3.getSequenceNumber());

        assertEquals(e1.getCurrentHash(), e2.getPreviousHash(), "e2.previousHash must match e1.currentHash");
        assertEquals(e2.getCurrentHash(), e3.getPreviousHash(), "e3.previousHash must match e2.currentHash");

        // Verify full chain
        AuditVerificationReport report = auditChainService.verifyChain();
        if (!report.isValid()) {
            System.err.println("TEST14 VIOLATIONS: " + report.getViolations());
        }
        assertTrue(report.isValid(), "Cryptographic audit chain must verify cleanly with 0 violations: " + report.getViolations());
        assertTrue(report.getViolations().isEmpty());
    }

    @Test
    @Order(15)
    @DisplayName("Stage F: Audit chain tampering is detected immediately by verifyChain()")
    void test15_AuditChainTamperDetection() {
        AuditLog e = auditChainService.recordChainedEvent("wf_test_actor", "MUTATE_EVENT", "target", "Legitimate payload");

        // Simulate unauthorized direct database alteration of event details
        transactionTemplate.execute(status -> {
            AuditLog log = auditLogRepo.findById(e.getId()).orElseThrow();
            log.setDetails("MALICIOUSLY ALTERED PAYLOAD");
            auditLogRepo.save(log);
            return null;
        });

        AuditVerificationReport report = auditChainService.verifyChain();
        assertFalse(report.isValid(), "Chain must detect tampering");
        assertFalse(report.getViolations().isEmpty());
        assertTrue(report.getViolations().stream().anyMatch(v -> v.contains("Tamper detected")));

        // Restore payload so subsequent tests pass cleanly
        transactionTemplate.execute(status -> {
            AuditLog log = auditLogRepo.findById(e.getId()).orElseThrow();
            log.setDetails("Legitimate payload");
            auditLogRepo.save(log);
            return null;
        });
    }

    // =========================================================================
    // 6. POST-COMMIT NOTIFICATION ORCHESTRATION TESTS
    // =========================================================================

    @Test
    @Order(16)
    @DisplayName("Stage G: Notifications execute post-commit and persist in_app_alert records")
    void test16_PostCommitNotificationPersistence() {
        long initialAlertCount = inAppAlertRepository.count();

        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.LOW, "Notification dispatch verification ticket"
        );
        ServiceTicketDTO created = commandService.createTicket(createReq, empAlice.getUsername());

        // Verify in-app alert was recorded for Alice
        List<InAppAlert> alerts = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(empAlice.getId());
        assertFalse(alerts.isEmpty(), "Alice must receive an alert upon ticket submission");
        assertTrue(alerts.stream().anyMatch(a -> a.getTicket() != null && a.getTicket().getId().equals(created.getId())));
    }

    // =========================================================================
    // 7. CONTEXTUAL COMMENTS TESTS
    // =========================================================================

    @Test
    @Order(17)
    @DisplayName("Stage D & E: Comments enforce internal visibility filtering based on viewer role")
    void test17_TicketCommentsVisibility() {
        CreateTicketRequestDTO createReq = new CreateTicketRequestDTO(
                simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Ticket for comment testing"
        );
        ServiceTicketDTO ticket = commandService.createTicket(createReq, empAlice.getUsername());
        commandService.assignTicket(ticket.getId(), resDave.getId(), resDave.getUsername());

        // 1 Public comment by requester
        commandService.addComment(ticket.getId(), new TicketCommentRequestDTO(
                "Public comment: Attached error logs for hardware diagnostic", false
        ), empAlice.getUsername());

        // 1 Internal comment by resolver
        commandService.addComment(ticket.getId(), new TicketCommentRequestDTO(
                "Internal specialist note: Hardware stock verified on shelf 3", true
        ), resDave.getUsername());

        // Requester Alice views comments: only public comment returned (1)
        List<TicketCommentDTO> aliceView = queryService.getCommentsForTicket(ticket.getId(), empAlice.getUsername());
        assertEquals(1, aliceView.size(), "Requester must only see public comments");
        assertFalse(aliceView.get(0).getIsInternal());

        // Resolver Dave views comments: both public and internal comments returned (2)
        List<TicketCommentDTO> daveView = queryService.getCommentsForTicket(ticket.getId(), resDave.getUsername());
        assertEquals(2, daveView.size(), "Specialist resolver must see all comments including internal");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Role getOrCreateRole(String roleName) {
        return roleRepo.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(roleName);
                    return roleRepo.save(r);
                });
    }

    private UserAccount getOrCreateUser(String username, Role role) {
        return userAccountRepo.findByUsername(username)
                .orElseGet(() -> {
                    UserAccount u = new UserAccount();
                    u.setUsername(username);
                    u.setPasswordHash("$2a$10$wO7vI13Lg1J13O0t7Vz7euZ8yG3bU0wUvO7vI13Lg1J13O0t7Vz7e");
                    u.setEmail(username + "@epams.com");
                    u.setStatus(AccountStatus.ACTIVE);
                    UserAccount saved = userAccountRepo.save(u);

                    UserRoleMapping mapping = new UserRoleMapping();
                    mapping.setUser(saved);
                    mapping.setRole(role);
                    userRoleMappingRepo.save(mapping);

                    return saved;
                });
    }

    private Profile getOrCreateProfile(UserAccount user, String employeeId, String firstName, String department, String managerUsername) {
        return profileRepo.findByUserUsername(user.getUsername())
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setUser(user);
                    p.setEmployeeId(employeeId);
                    p.setFirstName(firstName);
                    p.setLastName("Workflow");
                    p.setDepartment(department);
                    p.setReportingManager(managerUsername);
                    return profileRepo.save(p);
                });
    }

    @AfterAll
    static void tearDownAll(@Autowired com.examly.springapp.service.AuditChainService auditChainService,
                            @Autowired ApprovalTaskRepository approvalTaskRepository,
                            @Autowired TicketCommentRepository ticketCommentRepository,
                            @Autowired ServiceTicketRepository serviceTicketRepository) {
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        auditChainService.resetChainToGenesis();
    }
}
