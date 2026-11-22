package com.examly.springapp;

import com.examly.springapp.dto.*;
import com.examly.springapp.exception.AntiSelfApprovalException;
import com.examly.springapp.exception.BusinessRuleViolationException;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.NotificationEventListener;
import com.examly.springapp.notification.SmtpEmailDeliveryAdapter;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.*;
import com.examly.springapp.workflow.BusinessCalendar;
import com.examly.springapp.workflow.SlaScheduledProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Consolidated P7 + P8 + P9 Cross-Phase Acceptance Suite (Section 7 Scenarios 1-6)
 * plus deterministic validation of SlaScheduledProcessor (Clock 3 Auto-Closure + SLA Alerting)
 * and SmtpEmailDeliveryAdapter against an in-process Mailpit-compatible TCP SMTP server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class P7P9ConsolidatedAcceptanceTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private ProfileRepo profileRepo;
    @Autowired private ServiceCatalogRepository serviceCatalogRepository;
    @Autowired private ServiceTicketRepository serviceTicketRepository;
    @Autowired private ApprovalTaskRepository approvalTaskRepository;
    @Autowired private InAppAlertRepository inAppAlertRepository;
    @Autowired private TicketCommentRepository ticketCommentRepository;
    @Autowired private AuditLogRepo auditLogRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TicketCommandService ticketCommandService;
    @Autowired private ApprovalService approvalService;
    @Autowired private AuditChainService auditChainService;
    @Autowired private BusinessCalendar businessCalendar;
    @Autowired private SlaScheduledProcessor slaScheduledProcessor;
    @Autowired private NotificationEventListener notificationEventListener;
    @Autowired private SmtpEmailDeliveryAdapter smtpEmailDeliveryAdapter;
    @Autowired private TransactionTemplate transactionTemplate;

    private UserAccount empAlice;
    private UserAccount mgrBob;
    private UserAccount itDave;
    private UserAccount hrGrace;
    private UserAccount auditorEve;
    private ServiceCatalogItem stdItItem;
    private ServiceCatalogItem approvalItItem;
    private ServiceCatalogItem hrItem;

    private String aliceJwt;
    private String bobJwt;
    private String daveJwt;
    private String graceJwt;
    private String eveJwt;

    @BeforeEach
    void setup() throws Exception {
        inAppAlertRepository.deleteAll();
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        auditChainService.resetChainToGenesis();

        Role rEmp = getOrCreateRole("EMPLOYEE");
        Role rMgr = getOrCreateRole("MANAGER");
        Role rIt = getOrCreateRole("IT_SUPPORT");
        Role rHr = getOrCreateRole("HR_SPECIALIST");
        Role rAud = getOrCreateRole("AUDITOR");

        mgrBob = getOrCreateUser("acc_mgr_bob", "Password123!", rMgr);
        getOrCreateProfile(mgrBob, "Bob", "Manager", "Engineering", null);

        empAlice = getOrCreateUser("acc_emp_alice", "Password123!", rEmp);
        getOrCreateProfile(empAlice, "Alice", "Employee", "Engineering", mgrBob.getUsername());

        itDave = getOrCreateUser("acc_it_dave", "Password123!", rIt);
        getOrCreateProfile(itDave, "Dave", "IT", "IT", null);

        hrGrace = getOrCreateUser("acc_hr_grace", "Password123!", rHr);
        getOrCreateProfile(hrGrace, "Grace", "HR", "HR", null);

        auditorEve = getOrCreateUser("acc_auditor_eve", "Password123!", rAud);
        getOrCreateProfile(auditorEve, "Eve", "Auditor", "Audit", null);

        stdItItem = serviceCatalogRepository.findByItemCode("ACC-IT-STD").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "ACC-IT-STD", "Standard VPN Token", "Access", "IT",
                        "VPN token provisioning", BigDecimal.ZERO, 8, false, false, true)));

        approvalItItem = serviceCatalogRepository.findByItemCode("ACC-IT-APR").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "ACC-IT-APR", "High-Spec Workstation", "Hardware", "IT",
                        "Manager-approved developer laptop", new BigDecimal("2200.00"), 16, true, false, true)));

        hrItem = serviceCatalogRepository.findByItemCode("ACC-HR-CONF").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "ACC-HR-CONF", "HR Policy Consultation", "HR", "HR",
                        "Confidential HR request", BigDecimal.ZERO, 24, false, false, true)));

        aliceJwt = login("acc_emp_alice", "Password123!");
        bobJwt = login("acc_mgr_bob", "Password123!");
        daveJwt = login("acc_it_dave", "Password123!");
        graceJwt = login("acc_hr_grace", "Password123!");
        eveJwt = login("acc_auditor_eve", "Password123!");
    }

    @Test
    @Order(1)
    @DisplayName("Scenario 1 & 6: Standard service request end-to-end (Catalog -> Ticket -> Queue -> Resolve -> Notification -> Analytics -> Audit)")
    void scenario1And6_StandardServiceRequestAndAnalyticsAudit() throws Exception {
        // 1. Employee browses catalog
        mockMvc.perform(get("/api/v1/catalog").param("department", "IT").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.title=='Standard VPN Token')]").exists());

        // 2. Employee submits ticket
        MvcResult createRes = mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogItemId\":" + stdItItem.getId() + ",\"department\":\"IT\",\"priority\":\"HIGH\",\"description\":\"Need VPN token for sprint release\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        long ticketId = created.get("id").asLong();

        // 3. IT specialist sees ticket in IT queue, assigns, and resolves
        mockMvc.perform(get("/api/v1/departments/IT/queue").cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ticketId));

        MvcResult assignRes = mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/assign")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", daveJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + itDave.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();
        long ver = objectMapper.readTree(assignRes.getResponse().getContentAsString()).get("version").asLong();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/status")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", daveJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"reason\":\"VPN hardware token provisioned and verified.\",\"version\":" + ver + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 4. Employee receives TICKET_RESOLVED notification
        mockMvc.perform(get("/api/v1/notifications/my").param("eventType", "TICKET_RESOLVED")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // 5. Analytics reflects exact deterministic result & Auditor verifies valid hash chain
        mockMvc.perform(get("/api/v1/analytics/dashboard").cookie(new Cookie("accessToken", eveJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.periodTotalTickets").value(1))
                .andExpect(jsonPath("$.overview.resolvedTicketsInPeriod").value(1));

        mockMvc.perform(get("/api/v1/audit/verify").cookie(new Cookie("accessToken", eveJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @Order(2)
    @DisplayName("Scenario 2: Approval-required workflow, anti-self-approval, rejection reason validation & analytics")
    void scenario2_ApprovalRequiredWorkflowAndAntiSelfApproval() throws Exception {
        ServiceTicketDTO t = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(approvalItItem.getId(), "IT", TicketPriority.URGENT, "Requesting high-spec workstation"),
                empAlice.getUsername());
        assertEquals(TicketStatus.PENDING_APPROVAL, t.getStatus());

        List<ApprovalTaskDTO> bobTasks = approvalService.getPendingApprovalsForUser(mgrBob.getUsername());
        assertEquals(1, bobTasks.size());
        long taskId = bobTasks.get(0).getId();

        // Anti-self-approval: if Bob attempts to approve his own self-submitted approval task -> AntiSelfApprovalException
        ServiceTicketDTO bobOwn = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(approvalItItem.getId(), "IT", TicketPriority.MEDIUM, "Manager own workstation request"),
                mgrBob.getUsername());
        List<ApprovalTask> bobOwnTasks = approvalTaskRepository.findByTicketIdOrderByApprovalStageAsc(bobOwn.getId());
        if (!bobOwnTasks.isEmpty()) {
            assertThrows(AntiSelfApprovalException.class, () -> approvalService.decideApproval(
                    bobOwnTasks.get(0).getId(),
                    new ApprovalDecisionRequestDTO(ApprovalStatus.APPROVED, "Attempting self approval illegally", null),
                    mgrBob.getUsername()));
        }

        // Short rejection reason (< 15 chars) rejected with 400
        mockMvc.perform(post("/api/v1/approvals/" + taskId + "/decide")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", bobJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"reason\":\"Too short\"}"))
                .andExpect(status().isBadRequest());

        // Valid approval transitions ticket to ASSIGNED and notifies Alice
        mockMvc.perform(post("/api/v1/approvals/" + taskId + "/decide")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", bobJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"reason\":\"Approved within quarterly engineering budget.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newTicketStatus").value("ASSIGNED"));

        mockMvc.perform(get("/api/v1/analytics/approvals/overview").cookie(new Cookie("accessToken", eveJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedInPeriod").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("Scenario 3: Cross-department security (IT specialist blocked from HR ticket, HR queue, private notifications, internal notes)")
    void scenario3_CrossDepartmentSecurityBoundaries() throws Exception {
        ServiceTicketDTO hrTicket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(hrItem.getId(), "HR", TicketPriority.HIGH, "Confidential HR compensation inquiry"),
                empAlice.getUsername());

        // HR specialist posts internal note
        ticketCommandService.addComment(hrTicket.getId(),
                new TicketCommentRequestDTO("CONFIDENTIAL_HR_INTERNAL_NOTE", true), hrGrace.getUsername());

        // IT specialist blocked from HR queue, HR ticket, HR ticket comments, and Alice's notifications
        mockMvc.perform(get("/api/v1/departments/HR/queue").cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/" + hrTicket.getId()).cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/" + hrTicket.getId() + "/comments").cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isForbidden());

        List<InAppAlert> aliceAlerts = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(empAlice.getId());
        assertFalse(aliceAlerts.isEmpty());
        mockMvc.perform(put("/api/v1/notifications/" + aliceAlerts.get(0).getId() + "/read")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("Scenario 4: Scheduled Auto-Closure (Clock 3) & SLA Warning/Breach Scanner (Clock 1) with ON_HOLD exclusion and idempotency")
    void scenario4_ScheduledAutoClosureAndSlaAlertScanner() {
        // 1. Ticket A: Resolved Thu 2026-10-01 14:00 (before Oct 2 Gandhi Jayanti holiday + weekend -> deadline Fri 2026-10-09 17:00)
        ServiceTicketDTO tA = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(stdItItem.getId(), "IT", TicketPriority.MEDIUM, "Auto-closure boundary ticket A"),
                empAlice.getUsername());
        ServiceTicketDTO tAInProg = ticketCommandService.assignTicket(tA.getId(), itDave.getId(), itDave.getUsername());
        ticketCommandService.updateStatus(tA.getId(), new TicketStatusUpdateRequestDTO(TicketStatus.RESOLVED, null, tAInProg.getVersion()), itDave.getUsername());

        LocalDateTime resolvedAt = LocalDateTime.of(2026, 10, 1, 14, 0, 0);
        LocalDateTime exactDeadline = LocalDateTime.of(2026, 10, 9, 17, 0, 0);
        transactionTemplate.executeWithoutResult(tx -> {
            ServiceTicket entity = serviceTicketRepository.findById(tA.getId()).orElseThrow();
            entity.setResolvedAt(resolvedAt);
            serviceTicketRepository.saveAndFlush(entity);
        });

        // At exact inclusive deadline (2026-10-09 17:00:00) -> NOT eligible for auto-closure yet!
        SlaScheduledProcessor.SlaJobSummary atDeadline = slaScheduledProcessor.processAutoClosures(exactDeadline);
        assertEquals(0, atDeadline.processed(), "Ticket must NOT auto-close at inclusive reopen deadline");
        assertEquals(TicketStatus.RESOLVED, serviceTicketRepository.findById(tA.getId()).orElseThrow().getStatus());

        // 1 second AFTER deadline (2026-10-09 17:00:01) -> Auto-closed cleanly with audit & deduped notification!
        SlaScheduledProcessor.SlaJobSummary afterDeadline = slaScheduledProcessor.processAutoClosures(exactDeadline.plusSeconds(1));
        assertEquals(1, afterDeadline.processed());
        assertEquals(0, afterDeadline.failed());
        ServiceTicket closedTicket = serviceTicketRepository.findById(tA.getId()).orElseThrow();
        assertEquals(TicketStatus.CLOSED, closedTicket.getStatus());
        assertEquals(exactDeadline.plusSeconds(1), closedTicket.getClosedAt());

        // Second run is 100% idempotent (0 scanned, 0 duplicate alerts)
        SlaScheduledProcessor.SlaJobSummary secondRun = slaScheduledProcessor.processAutoClosures(exactDeadline.plusHours(1));
        assertEquals(0, secondRun.processed());
        long autoCloseAlerts = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(empAlice.getId()).stream()
                .filter(a -> "TICKET_AUTO_CLOSED".equals(a.getAlertType()))
                .count();
        assertEquals(1, autoCloseAlerts, "Exactly one TICKET_AUTO_CLOSED notification must exist");
        assertTrue(auditChainService.verifyChain().isValid(), "Audit chain must remain valid after SYSTEM_AUTO_CLOSURE");

        // 2. SLA Breach & Warning Scanner (Clock 1): ON_HOLD paused ticket MUST be excluded!
        ServiceTicketDTO activeBreached = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(stdItItem.getId(), "IT", TicketPriority.HIGH, "Active overdue ticket"),
                empAlice.getUsername());
        ticketCommandService.assignTicket(activeBreached.getId(), itDave.getId(), itDave.getUsername());

        ServiceTicketDTO pausedOnHold = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(stdItItem.getId(), "IT", TicketPriority.HIGH, "Paused ON_HOLD ticket"),
                empAlice.getUsername());
        ServiceTicketDTO pausedAssigned = ticketCommandService.assignTicket(pausedOnHold.getId(), itDave.getId(), itDave.getUsername());
        ticketCommandService.updateStatus(pausedOnHold.getId(),
                new TicketStatusUpdateRequestDTO(TicketStatus.ON_HOLD, "Waiting on employee hardware serial confirmation.", pausedAssigned.getVersion()),
                itDave.getUsername());

        LocalDateTime scanNow = LocalDateTime.of(2026, 10, 15, 12, 0, 0);
        transactionTemplate.executeWithoutResult(tx -> {
            ServiceTicket b = serviceTicketRepository.findById(activeBreached.getId()).orElseThrow();
            b.setSlaTargetDate(scanNow.minusHours(1)); // Breached
            serviceTicketRepository.saveAndFlush(b);

            ServiceTicket p = serviceTicketRepository.findById(pausedOnHold.getId()).orElseThrow();
            p.setSlaTargetDate(scanNow.minusHours(5)); // Past date, but ON_HOLD clock is paused!
            serviceTicketRepository.saveAndFlush(p);
        });

        // Run SLA scanner twice -> activeBreached gets 1 SLA_BREACHED alert; ON_HOLD gets 0; second run produces 0 duplicates!
        slaScheduledProcessor.processSlaAlerts(scanNow);
        slaScheduledProcessor.processSlaAlerts(scanNow.plusMinutes(30));

        List<InAppAlert> daveBreachAlerts = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(itDave.getId()).stream()
                .filter(a -> "SLA_BREACHED".equals(a.getAlertType()))
                .toList();
        assertEquals(1, daveBreachAlerts.size(), "Only active breached ticket should generate a single deduplicated SLA_BREACHED alert");
        assertEquals(activeBreached.getId(), daveBreachAlerts.get(0).getRelatedEntityId());
    }

    @Test
    @Order(5)
    @DisplayName("Scenario 5: Transaction failure recovery + Mailpit-compatible local SMTP delivery, dedup, and outage isolation")
    void scenario5_FailureRecoveryAndLocalSmtpEmailDelivery() throws Exception {
        // Part A: Start in-process Mailpit-compatible RFC 5321 SMTP server on ephemeral localhost port
        List<String> capturedPayloads = Collections.synchronizedList(new ArrayList<>());
        ExecutorService smtpExec = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0)) {
            int smtpPort = server.getLocalPort();
            smtpExec.submit(() -> {
                while (!server.isClosed()) {
                    try (Socket client = server.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
                        out.write("220 epams-mailpit-stub ESMTP Ready\r\n");
                        out.flush();
                        StringBuilder data = new StringBuilder();
                        boolean inData = false;
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (inData) {
                                if (".".equals(line)) {
                                    inData = false;
                                    capturedPayloads.add(data.toString());
                                    out.write("250 2.0.0 OK queued\r\n");
                                    out.flush();
                                } else {
                                    data.append(line).append("\n");
                                }
                            } else if (line.startsWith("HELO") || line.startsWith("EHLO")
                                    || line.startsWith("MAIL FROM") || line.startsWith("RCPT TO")) {
                                out.write("250 OK\r\n");
                                out.flush();
                            } else if ("DATA".equalsIgnoreCase(line.trim())) {
                                inData = true;
                                out.write("354 End data with <CR><LF>.<CR><LF>\r\n");
                                out.flush();
                            } else if ("QUIT".equalsIgnoreCase(line.trim())) {
                                out.write("221 Bye\r\n");
                                out.flush();
                                break;
                            }
                        }
                    } catch (IOException ignored) {
                        break;
                    }
                }
            });

            // Enable SMTP adapter targeting our local ephemeral server
            ReflectionTestUtils.setField(smtpEmailDeliveryAdapter, "enabled", true);
            ReflectionTestUtils.setField(smtpEmailDeliveryAdapter, "host", "127.0.0.1");
            ReflectionTestUtils.setField(smtpEmailDeliveryAdapter, "port", smtpPort);

            try {
                // 1. Dispatch event twice with same dedupKey -> 1 in-app alert AND 1 email (no duplicate email on retry!)
                TicketNotificationEvent evt = new TicketNotificationEvent(
                        null, "TKT-2026-SMTP1", empAlice.getUsername(), "TICKET_RESOLVED",
                        "Ticket Resolved: TKT-2026-SMTP1", "Your ticket has been resolved.",
                        "TICKET", 9001L, "SMTP_DEDUP_KEY_1");
                assertTrue(notificationEventListener.dispatchNotification(evt));
                assertFalse(notificationEventListener.dispatchNotification(evt), "Duplicate dedupKey must be suppressed");

                assertEquals(1, capturedPayloads.size(), "Exactly 1 email must be delivered across duplicate event retries");
                assertTrue(capturedPayloads.get(0).contains("Subject: [EPAMS] Ticket Resolved: TKT-2026-SMTP1"));
                assertTrue(capturedPayloads.get(0).contains("To: acc_emp_alice@epams.local"));

                // 2. Simulate SMTP outage (closed port) -> in-app notification MUST STILL PERSIST cleanly!
                server.close();
                TicketNotificationEvent outageEvt = new TicketNotificationEvent(
                        null, "TKT-2026-SMTP2", empAlice.getUsername(), "TICKET_APPROVED",
                        "Ticket Approved: TKT-2026-SMTP2", "Approved despite SMTP outage.",
                        "TICKET", 9002L, "SMTP_OUTAGE_KEY_2");
                assertTrue(notificationEventListener.dispatchNotification(outageEvt),
                        "In-app notification must succeed even when external SMTP relay is unreachable");
                assertTrue(inAppAlertRepository.existsByRecipientIdAndDedupKey(empAlice.getId(), "SMTP_OUTAGE_KEY_2"));
            } finally {
                ReflectionTestUtils.setField(smtpEmailDeliveryAdapter, "enabled", false);
                smtpExec.shutdownNow();
            }
        }

        // Part B: Rolled-back transaction leaves 0 partial business tickets & 0 phantom notifications
        long ticketsBefore = serviceTicketRepository.count();
        long alertsBefore = inAppAlertRepository.count();
        assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            ticketCommandService.createTicket(
                    new CreateTicketRequestDTO(stdItItem.getId(), "IT", TicketPriority.LOW, "Simulated mid-tx DB failure"),
                    empAlice.getUsername());
            throw new RuntimeException("Simulated downstream failure forcing rollback");
        }));
        assertEquals(ticketsBefore, serviceTicketRepository.count(), "Rolled-back transaction must not leave partial ticket");
        assertEquals(alertsBefore, inAppAlertRepository.count(), "Rolled-back transaction must not emit phantom notification");
    }

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie c = res.getResponse().getCookie("accessToken");
        assertNotNull(c);
        return c.getValue();
    }

    private Role getOrCreateRole(String name) {
        return roleRepo.findByRoleName(name).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(name);
            return roleRepo.saveAndFlush(r);
        });
    }

    private UserAccount getOrCreateUser(String username, String password, Role role) {
        UserAccount u = userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount acc = new UserAccount();
            acc.setUsername(username);
            acc.setPasswordHash(passwordEncoder.encode(password));
            acc.setEmail(username + "@epams.local");
            acc.setStatus(AccountStatus.ACTIVE);
            return userAccountRepo.saveAndFlush(acc);
        });
        u.setStatus(AccountStatus.ACTIVE);
        u = userAccountRepo.saveAndFlush(u);
        if (userRoleMappingRepo.findByUserId(u.getId()).isEmpty()) {
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(u);
            m.setRole(role);
            userRoleMappingRepo.saveAndFlush(m);
        }
        return u;
    }

    private void getOrCreateProfile(UserAccount user, String first, String last, String dept, String manager) {
        if (profileRepo.findByUserId(user.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(user);
            p.setEmployeeId("EMP-ACC-" + user.getId());
            p.setFirstName(first);
            p.setLastName(last);
            p.setDepartment(dept);
            p.setDesignation("Specialist");
            p.setDateOfJoining(LocalDate.of(2024, 1, 1));
            p.setReportingManager(manager);
            profileRepo.saveAndFlush(p);
        }
    }
}
