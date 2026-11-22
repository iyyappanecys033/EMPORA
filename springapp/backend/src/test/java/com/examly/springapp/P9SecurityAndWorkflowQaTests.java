package com.examly.springapp;

import com.examly.springapp.dto.*;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.NotificationEventListener;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.ApprovalService;
import com.examly.springapp.service.AuditChainService;
import com.examly.springapp.service.TicketCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P9 Comprehensive QA, Security, Boundary, Concurrency & Reliability Verification Suite.
 * Validates remediations SEC-P9-01 through SEC-P9-05, reason length boundaries,
 * optimistic locking concurrency, audit-chain concurrent append integrity,
 * atomic notification deduplication, executor saturation, and suspended session revocation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class P9SecurityAndWorkflowQaTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private ProfileRepo profileRepo;
    @Autowired private ServiceCatalogRepository serviceCatalogRepository;
    @Autowired private ServiceTicketRepository serviceTicketRepository;
    @Autowired private TicketCommentRepository ticketCommentRepository;
    @Autowired private ApprovalTaskRepository approvalTaskRepository;
    @Autowired private InAppAlertRepository inAppAlertRepository;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TicketCommandService ticketCommandService;
    @Autowired private ApprovalService approvalService;
    @Autowired private AuditChainService auditChainService;
    @Autowired private NotificationEventListener notificationEventListener;
    @Autowired @Qualifier("notificationTaskExecutor") private Executor notificationTaskExecutor;

    private UserAccount empAlice;
    private UserAccount empCharlie;
    private UserAccount mgrBob;
    private UserAccount itDave;
    private UserAccount adminUser;
    private ServiceCatalogItem simpleItem;
    private ServiceCatalogItem approvalItem;

    private String aliceJwt;
    private String charlieJwt;
    private String bobJwt;
    private String daveJwt;
    private String adminJwt;

    @BeforeEach
    void setup() throws Exception {
        inAppAlertRepository.deleteAll();
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();

        Role rEmp = getOrCreateRole("ROLE_EMPLOYEE");
        Role rMgr = getOrCreateRole("ROLE_MANAGER");
        Role rIt = getOrCreateRole("ROLE_IT_SUPPORT");
        Role rAdmin = getOrCreateRole("ROLE_ADMIN");

        mgrBob = getOrCreateUser("p9_qa_bob", "Password123!", rMgr);
        getOrCreateProfile(mgrBob, "Bob", "Manager", "IT", null, 110000.0);

        empAlice = getOrCreateUser("p9_qa_alice", "Password123!", rEmp);
        getOrCreateProfile(empAlice, "Alice", "Engineer", "Engineering", "p9_qa_bob", 95000.0);

        empCharlie = getOrCreateUser("p9_qa_charlie", "Password123!", rEmp);
        getOrCreateProfile(empCharlie, "Charlie", "Analyst", "Finance", null, 85000.0);

        itDave = getOrCreateUser("p9_qa_dave", "Password123!", rIt);
        getOrCreateProfile(itDave, "Dave", "Resolver", "IT", null, 90000.0);

        adminUser = getOrCreateUser("p9_qa_admin", "Password123!", rAdmin);
        getOrCreateProfile(adminUser, "System", "Admin", "Operations", null, 150000.0);

        simpleItem = serviceCatalogRepository.findByItemCode("P9-QA-SIMPLE").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P9-QA-SIMPLE", "QA VPN Reset", "Access", "IT",
                        "Simple IT request", new BigDecimal("0.00"), 8, false, false, true)));

        approvalItem = serviceCatalogRepository.findByItemCode("P9-QA-APPROVAL").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P9-QA-APPROVAL", "QA High-Spec Workstation", "Hardware", "IT",
                        "Requires approval", new BigDecimal("2500.00"), 24, true, false, true)));

        aliceJwt = login("p9_qa_alice", "Password123!");
        charlieJwt = login("p9_qa_charlie", "Password123!");
        bobJwt = login("p9_qa_bob", "Password123!");
        daveJwt = login("p9_qa_dave", "Password123!");
        adminJwt = login("p9_qa_admin", "Password123!");
    }

    @Test
    @Order(1)
    @DisplayName("SEC-P9-01: Production runtime strictly prohibits AuditChainService.resetChainToGenesis()")
    void test1_ProductionBlocksDestructiveAuditChainReset() {
        auditChainService.recordChainedEvent("p9_qa_admin", "PROD_AUDIT_EVENT", "system", "Immutable production entry");
        AuditVerificationReport before = auditChainService.verifyChain();
        long recordsBefore = before.getChainedRecordsCount();
        assertTrue(recordsBefore >= 1);

        // Simulate production flag (allowChainReset = false)
        ReflectionTestUtils.setField(auditChainService, "allowChainReset", false);
        try {
            assertThrows(UnsupportedOperationException.class, () -> auditChainService.resetChainToGenesis(),
                    "Production configuration MUST reject destructive audit chain reset");
            AuditVerificationReport after = auditChainService.verifyChain();
            assertEquals(recordsBefore, after.getChainedRecordsCount(), "Audit history must remain intact");
            assertTrue(after.isValid());
        } finally {
            ReflectionTestUtils.setField(auditChainService, "allowChainReset", true);
        }
    }

    @Test
    @Order(2)
    @DisplayName("SEC-P9-02: Non-specialist employee cannot spoof internal specialist comments (403)")
    void test2_EmployeeCannotPostInternalComment() throws Exception {
        ServiceTicketDTO ticket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Test internal comment privilege check"),
                empAlice.getUsername());

        // Employee attempts to post internal comment -> 403 Forbidden
        TicketCommentRequestDTO spoofReq = new TicketCommentRequestDTO("Attempting to post internal specialist note", true);
        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/comments")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(spoofReq)))
                .andExpect(status().isForbidden());

        // Authorized IT specialist posts internal comment -> 201/200 OK
        TicketCommentRequestDTO validInternal = new TicketCommentRequestDTO("Verified server log internally", true);
        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/comments")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", daveJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInternal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isInternal").value(true));
    }

    @Test
    @Order(3)
    @DisplayName("SEC-P9-03 & SEC-P9-05: Legacy IAM endpoints enforce RBAC, redact directory PII/salary, and bound pagination")
    void test3_LegacyIamEndpointsEnforceRbacAndRedactSalary() throws Exception {
        // Ordinary employee blocked from raw IAM enumeration endpoints (403)
        mockMvc.perform(get("/api/users").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/" + empCharlie.getId()).cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profiles").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profiles/" + empCharlie.getId() + "/details").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/roles").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/userRoleMappings").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/accountStatusLogs").cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());

        // Directory accessible to employee, but redacts salary/dob/address/emergencyContact AND clamps page size to 100
        mockMvc.perform(get("/api/profiles/directory")
                        .param("size", "9999")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content[0].salary").value(nullValue()))
                .andExpect(jsonPath("$.content[0].dob").value(nullValue()))
                .andExpect(jsonPath("$.content[0].address").value(nullValue()))
                .andExpect(jsonPath("$.content[0].emergencyContact").value(nullValue()));

        // Admin still accesses full directory with salary intact
        mockMvc.perform(get("/api/profiles/directory")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username=='p9_qa_alice')].salary", hasItem(95000.0)));
    }

    @Test
    @Order(4)
    @DisplayName("SEC-P9-04: Approval history endpoint enforces BOLA/IDOR authorization")
    void test4_ApprovalHistoryEnforcesOwnershipAndScope() throws Exception {
        ServiceTicketDTO ticket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(approvalItem.getId(), "IT", TicketPriority.HIGH, "Approval history BOLA check"),
                empAlice.getUsername());

        // Unrelated employee Charlie -> 403 Forbidden
        mockMvc.perform(get("/api/v1/approvals/tickets/" + ticket.getId())
                        .cookie(new Cookie("accessToken", charlieJwt)))
                .andExpect(status().isForbidden());

        // Requester Alice -> 200 OK
        mockMvc.perform(get("/api/v1/approvals/tickets/" + ticket.getId())
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Assigned Approver Bob -> 200 OK
        mockMvc.perform(get("/api/v1/approvals/tickets/" + ticket.getId())
                        .cookie(new Cookie("accessToken", bobJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Order(5)
    @DisplayName("Phase E: Mandatory reason boundary checks (14 chars, whitespace-only, exact 15 chars)")
    void test5_MandatoryReasonBoundaryValidation() throws Exception {
        ServiceTicketDTO ticket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.LOW, "Boundary test ticket for cancel reasons"),
                empAlice.getUsername());

        // 14 characters -> 400 Bad Request
        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/cancel")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketActionRequestDTO("12345678901234", ticket.getVersion()))))
                .andExpect(status().isBadRequest());

        // 20 spaces (whitespace-only) -> 400 Bad Request
        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/cancel")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketActionRequestDTO("                    ", ticket.getVersion()))))
                .andExpect(status().isBadRequest());

        // Exactly 15 characters -> 200 OK
        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/cancel")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketActionRequestDTO("123456789012345", ticket.getVersion()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Repeated cancel on already-cancelled ticket -> 400 Bad Request
        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/cancel")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketActionRequestDTO("Attempting duplicate cancellation", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("Phase E: Concurrent ticket status updates trigger HTTP 409 optimistic locking with zero silent overwrite")
    void test6_ConcurrentOptimisticLockingReturns409() throws Exception {
        ServiceTicketDTO created = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.HIGH, "Concurrent optimistic lock verification"),
                empAlice.getUsername());
        ServiceTicketDTO inProgress = ticketCommandService.assignTicket(created.getId(), itDave.getId(), itDave.getUsername());
        Long staleVersion = inProgress.getVersion();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);

        Callable<Integer> worker1 = () -> {
            gate.await();
            MvcResult res = mockMvc.perform(post("/api/v1/tickets/" + created.getId() + "/status")
                            .with(csrf())
                            .cookie(new Cookie("accessToken", daveJwt))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TicketStatusUpdateRequestDTO(TicketStatus.ON_HOLD, "Waiting for hardware vendor confirmation", staleVersion))))
                    .andReturn();
            return res.getResponse().getStatus();
        };

        Callable<Integer> worker2 = () -> {
            gate.await();
            MvcResult res = mockMvc.perform(post("/api/v1/tickets/" + created.getId() + "/status")
                            .with(csrf())
                            .cookie(new Cookie("accessToken", daveJwt))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new TicketStatusUpdateRequestDTO(TicketStatus.RESOLVED, "Resolved issue immediately", staleVersion))))
                    .andReturn();
            return res.getResponse().getStatus();
        };

        Future<Integer> f1 = pool.submit(worker1);
        Future<Integer> f2 = pool.submit(worker2);
        gate.countDown();

        List<Integer> statuses = Arrays.asList(f1.get(10, TimeUnit.SECONDS), f2.get(10, TimeUnit.SECONDS));
        pool.shutdown();

        long okCount = statuses.stream().filter(s -> s == 200).count();
        long conflictOrRejectCount = statuses.stream().filter(s -> s == 409 || s == 400).count();
        assertEquals(1, okCount, "Exactly ONE concurrent update must succeed; statuses=" + statuses);
        assertEquals(1, conflictOrRejectCount, "The competing stale update must be rejected (409/400); statuses=" + statuses);
    }

    @Test
    @Order(7)
    @DisplayName("Phase F: Concurrent audit-chain appends preserve monotonic sequence and 0 hash violations")
    void test7_ConcurrentAuditChainAppendsHaveZeroForks() throws Exception {
        int threads = 6;
        int eventsPerThread = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            futures.add(pool.submit(() -> {
                try {
                    gate.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        auditChainService.recordChainedEvent("worker_" + id, "CONCURRENT_AUDIT", "target_" + i,
                                "Concurrent hash chain entry " + id + "-" + i);
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            }));
        }
        gate.countDown();
        for (Future<?> f : futures) {
            f.get(20, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(0, failures.get(), "Concurrent audit chain appends must not fail under row lock");
        AuditVerificationReport report = auditChainService.verifyChain();
        assertTrue(report.isValid(), "Audit chain must remain 100% valid after concurrent appends: " + report.getViolations());
        assertTrue(report.getViolations().isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("Phase G: Concurrent duplicate notification dispatch guarantees exactly 1 persisted row via DB unique constraint")
    void test8_ConcurrentNotificationDeduplicationAtomic() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        String dedupKey = "CONCURRENT_DEDUP_KEY_999";

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                gate.await();
                TicketNotificationEvent ev = new TicketNotificationEvent(
                        null, null, empAlice.getUsername(), "SLA_WARNING",
                        "Concurrent SLA Warning", "Testing atomic dedup constraint",
                        "TICKET", 999L, dedupKey
                );
                return notificationEventListener.dispatchNotification(ev);
            }));
        }
        gate.countDown();

        int persistedTrueCount = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get(15, TimeUnit.SECONDS))) {
                persistedTrueCount++;
            }
        }
        pool.shutdown();

        long dbCount = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(empAlice.getId()).stream()
                .filter(a -> dedupKey.equals(a.getDedupKey()))
                .count();
        assertEquals(1, dbCount, "DB unique constraint uk_alert_recipient_dedup must guarantee exactly 1 alert");
        assertEquals(1, persistedTrueCount, "Exactly 1 concurrent dispatch call should report persisted=true");
    }

    @Test
    @Order(9)
    @DisplayName("Phase G: Bounded notificationTaskExecutor handles saturation via CallerRunsPolicy without dropping tasks")
    void test9_NotificationExecutorHandlesSaturationGracefully() throws Exception {
        int totalTasks = 130; // Exceeds queueCapacity=100 + maxPoolSize=4
        CountDownLatch completed = new CountDownLatch(totalTasks);
        AtomicInteger executed = new AtomicInteger(0);

        for (int i = 0; i < totalTasks; i++) {
            notificationTaskExecutor.execute(() -> {
                executed.incrementAndGet();
                completed.countDown();
            });
        }

        assertTrue(completed.await(15, TimeUnit.SECONDS), "All tasks must finish without RejectedExecutionException");
        assertEquals(totalTasks, executed.get());
    }

    @Test
    @Order(10)
    @DisplayName("Phase K: Suspended account's existing JWT is rejected immediately and malformed JSON returns safe RFC 7807")
    void test10_SuspendedAccountRejectedAndMalformedJsonSafe() throws Exception {
        // Suspend Charlie while Charlie holds a valid JWT
        empCharlie.setStatus(AccountStatus.SUSPENDED);
        userAccountRepo.saveAndFlush(empCharlie);

        mockMvc.perform(get("/api/users/me")
                        .cookie(new Cookie("accessToken", charlieJwt)))
                .andExpect(status().isUnauthorized());

        // Restore Charlie to ACTIVE
        empCharlie.setStatus(AccountStatus.ACTIVE);
        userAccountRepo.saveAndFlush(empCharlie);

        // Malformed JSON payload -> strict 400 Bad Request RFC 7807 without stack trace leak
        MvcResult malformed = mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andReturn();
        String body = malformed.getResponse().getContentAsString();
        assertFalse(body.contains("java.lang."), "Error response must not expose internal Java stack traces");
    }

    private Role getOrCreateRole(String name) {
        return roleRepo.findByRoleName(name).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(name);
            return roleRepo.saveAndFlush(r);
        });
    }

    private UserAccount getOrCreateUser(String username, String password, Role role) {
        UserAccount user = userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setEmail(username + "@epams.local");
            u.setStatus(AccountStatus.ACTIVE);
            return userAccountRepo.saveAndFlush(u);
        });
        user.setStatus(AccountStatus.ACTIVE);
        user = userAccountRepo.saveAndFlush(user);
        if (userRoleMappingRepo.findByUserId(user.getId()).isEmpty()) {
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(user);
            m.setRole(role);
            userRoleMappingRepo.saveAndFlush(m);
        }
        return user;
    }

    private void getOrCreateProfile(UserAccount user, String first, String last, String dept, String manager, double salary) {
        if (profileRepo.findByUserId(user.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(user);
            p.setEmployeeId("EMP-P9-" + user.getId());
            p.setFirstName(first);
            p.setLastName(last);
            p.setDepartment(dept);
            p.setDesignation("Staff");
            p.setSalary(salary);
            p.setDob(LocalDate.of(1992, 5, 15));
            p.setAddress("123 Confidential St");
            p.setEmergencyContact("9999999999");
            p.setDateOfJoining(LocalDate.of(2024, 1, 1));
            p.setReportingManager(manager);
            profileRepo.saveAndFlush(p);
        }
    }

    private String login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return res.getResponse().getCookie("accessToken").getValue();
    }
}
