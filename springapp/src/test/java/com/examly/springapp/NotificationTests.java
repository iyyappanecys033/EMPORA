package com.examly.springapp;

import com.examly.springapp.dto.*;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.NotificationEventListener;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.ApprovalService;
import com.examly.springapp.service.TicketCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P8 Notification System Test Suite:
 * Covers unit/integration notification persistence, duplicate suppression,
 * transaction rollback isolation, failure resilience, ownership & IDOR security,
 * CSRF enforcement, pagination bounds, mark-read idempotency, and internal comment privacy.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotificationTests {

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
    @Autowired private NotificationEventListener notificationEventListener;
    @Autowired private PlatformTransactionManager transactionManager;

    private UserAccount alice;
    private UserAccount bobMgr;
    private UserAccount daveIt;
    private UserAccount adminUser;
    private ServiceCatalogItem simpleItem;
    private ServiceCatalogItem approvalItem;

    private String aliceJwt;
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

        bobMgr = getOrCreateUser("p8_notif_bob", "Password123!", rMgr);
        getOrCreateProfile(bobMgr, "Bob", "Manager", "IT", null);

        alice = getOrCreateUser("p8_notif_alice", "Password123!", rEmp);
        getOrCreateProfile(alice, "Alice", "Employee", "Engineering", "p8_notif_bob");

        daveIt = getOrCreateUser("p8_notif_dave", "Password123!", rIt);
        getOrCreateProfile(daveIt, "Dave", "Specialist", "IT", null);

        adminUser = getOrCreateUser("p8_notif_admin", "Password123!", rAdmin);
        getOrCreateProfile(adminUser, "Admin", "User", "Operations", null);

        simpleItem = serviceCatalogRepository.findByItemCode("P8-NOTIF-SIMPLE").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-NOTIF-SIMPLE", "VPN Access Reset", "Access", "IT",
                        "Quick VPN reset", new BigDecimal("0.00"), 8, false, false, true)));

        approvalItem = serviceCatalogRepository.findByItemCode("P8-NOTIF-APPROVAL").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-NOTIF-APPROVAL", "Workstation Upgrade", "Hardware", "IT",
                        "High-spec developer workstation", new BigDecimal("2500.00"), 24, true, false, true)));

        aliceJwt = loginAndGetAccessToken("p8_notif_alice", "Password123!");
        bobJwt = loginAndGetAccessToken("p8_notif_bob", "Password123!");
        daveJwt = loginAndGetAccessToken("p8_notif_dave", "Password123!");
        adminJwt = loginAndGetAccessToken("p8_notif_admin", "Password123!");
    }

    @Test
    @Order(1)
    void test1_UnauthenticatedNotificationRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/notifications/my/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void test2_TicketSubmissionAndAssignmentAndResolutionGenerateTargetedNotifications() throws Exception {
        ServiceTicketDTO ticket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Need VPN reset urgently for remote sprint"),
                alice.getUsername());

        // Alice receives TICKET_SUBMITTED
        mockMvc.perform(get("/api/v1/notifications/my")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("TICKET_SUBMITTED"))
                .andExpect(jsonPath("$.content[0].ticketId").value(ticket.getId()))
                .andExpect(jsonPath("$.content[0].isRead").value(false));

        // Dave receives TICKET_ASSIGNED when assigned
        ticketCommandService.assignTicket(ticket.getId(), daveIt.getId(), daveIt.getUsername());
        mockMvc.perform(get("/api/v1/notifications/my/unread-count")
                        .cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Resolve ticket -> Alice receives TICKET_RESOLVED
        ServiceTicketDTO assigned = ticketCommandService.updateStatus(
                ticket.getId(), new TicketStatusUpdateRequestDTO(TicketStatus.RESOLVED, "VPN profile recreated and verified.", null),
                daveIt.getUsername());

        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("eventType", "TICKET_RESOLVED")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("TICKET_RESOLVED"));

        // Reopen ticket -> Dave receives TICKET_REOPENED
        ticketCommandService.reopenTicket(assigned.getId(), "VPN still disconnects after 5 minutes of inactivity.",
                assigned.getVersion(), alice.getUsername());

        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("eventType", "TICKET_REOPENED")
                        .cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value("TICKET_REOPENED"));
    }

    @Test
    @Order(3)
    void test3_ApprovalFlowNotifiesApproverAndRequesterWithEntityMetadata() throws Exception {
        ServiceTicketDTO ticket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(approvalItem.getId(), "IT", TicketPriority.HIGH, "Need 64GB workstation for local builds"),
                alice.getUsername());

        // Bob (manager) receives APPROVAL_REQUIRED with relatedEntityType = APPROVAL
        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("eventType", "APPROVAL_REQUIRED")
                        .cookie(new Cookie("accessToken", bobJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].relatedEntityType").value("APPROVAL"))
                .andExpect(jsonPath("$.content[0].ticketId").value(ticket.getId()));

        // Bob approves -> Alice receives TICKET_APPROVED
        List<ApprovalTask> tasks = approvalTaskRepository.findByTicketIdOrderByApprovalStageAsc(ticket.getId());
        assertEquals(1, tasks.size());
        approvalService.decideApproval(tasks.get(0).getId(),
                new ApprovalDecisionRequestDTO(ApprovalStatus.APPROVED, "Approved for engineering project", null),
                bobMgr.getUsername());

        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("eventType", "TICKET_APPROVED")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @Order(4)
    void test4_MarkAsReadIsIdempotentAndUpdatesUnreadCount() throws Exception {
        ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.LOW, "Check mark as read idempotency"),
                alice.getUsername());

        List<InAppAlert> aliceAlerts = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(alice.getId());
        assertEquals(1, aliceAlerts.size());
        Long alertId = aliceAlerts.get(0).getId();

        // First PUT -> marks read and sets readAt
        MvcResult firstRes = mockMvc.perform(put("/api/v1/notifications/" + alertId + "/read")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty())
                .andReturn();

        String firstReadAt = objectMapper.readTree(firstRes.getResponse().getContentAsString()).get("readAt").asText();

        // Second PUT -> idempotent, preserves original readAt
        MvcResult secondRes = mockMvc.perform(put("/api/v1/notifications/" + alertId + "/read")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true))
                .andReturn();

        String secondReadAt = objectMapper.readTree(secondRes.getResponse().getContentAsString()).get("readAt").asText();
        assertEquals(firstReadAt, secondReadAt, "Repeated mark-read must preserve initial readAt timestamp");

        mockMvc.perform(get("/api/v1/notifications/my/unread-count")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @Order(5)
    void test5_MarkAllAsReadAndUnreadOnlyFilterWork() throws Exception {
        ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.LOW, "First notification batch ticket"),
                alice.getUsername());
        ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.LOW, "Second notification batch ticket"),
                alice.getUsername());

        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("unreadOnly", "true")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(put("/api/v1/notifications/my/read-all")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("unreadOnly", "true")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(6)
    void test6_OwnershipIsolationAndIdorPreventionEvenForAdmin() throws Exception {
        ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Private notification ownership check"),
                alice.getUsername());

        Long aliceAlertId = inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(alice.getId()).get(0).getId();

        // Dave cannot mark Alice's notification as read (403)
        mockMvc.perform(put("/api/v1/notifications/" + aliceAlertId + "/read")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", daveJwt)))
                .andExpect(status().isForbidden());

        // Admin also cannot mark Alice's private notification as read (403)
        mockMvc.perform(put("/api/v1/notifications/" + aliceAlertId + "/read")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isForbidden());

        // Unknown notification ID returns 404
        mockMvc.perform(put("/api/v1/notifications/999999/read")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void test7_CsrfProtectionRequiredOnNotificationMutations() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/my/read-all")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void test8_PaginationBoundsPreventUnboundedQueries() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my")
                        .param("page", "-5")
                        .param("size", "9999")
                        .cookie(new Cookie("accessToken", aliceJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @Order(9)
    void test9_DuplicateSuppressionPreventsRepeatedNotificationsWithSameDedupKey() {
        TicketNotificationEvent event = new TicketNotificationEvent(
                null, null, alice.getUsername(), "SLA_WARNING",
                "SLA Warning", "Approaching SLA deadline", "TICKET", 42L, "SLA_WARN_42_ALICE"
        );

        boolean first = notificationEventListener.dispatchNotification(event);
        boolean second = notificationEventListener.dispatchNotification(event);

        assertTrue(first, "First dispatch with unique dedupKey must persist");
        assertFalse(second, "Second dispatch with identical dedupKey must be suppressed");
        assertEquals(1, inAppAlertRepository.findByRecipientIdOrderByCreatedAtDesc(alice.getId()).size());
    }

    @Test
    @Order(10)
    void test10_InvalidRecipientOrFailureDoesNotThrowOrBreakCaller() {
        TicketNotificationEvent invalidRecipientEvent = new TicketNotificationEvent(
                null, null, "nonexistent_ghost_user", "TICKET_SUBMITTED", "Ghost", "Should not throw"
        );
        assertDoesNotThrow(() -> {
            boolean persisted = notificationEventListener.dispatchNotification(invalidRecipientEvent);
            assertFalse(persisted);
        });
    }

    @Test
    @Order(11)
    void test11_RolledBackBusinessTransactionDoesNotPersistNotifications() {
        long beforeCount = inAppAlertRepository.count();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () -> txTemplate.execute(status -> {
            ticketCommandService.createTicket(
                    new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.LOW, "Will be rolled back immediately"),
                    alice.getUsername());
            throw new RuntimeException("Simulated downstream failure forcing rollback");
        }));

        assertEquals(beforeCount, inAppAlertRepository.count(),
                "Rolled-back business transaction must NEVER persist post-commit notifications");
    }

    @Test
    @Order(12)
    void test12_InternalCommentsNeverLeakIntoNotifications() {
        ServiceTicketDTO ticket = ticketCommandService.createTicket(
                new CreateTicketRequestDTO(simpleItem.getId(), "IT", TicketPriority.MEDIUM, "Check internal note privacy in alerts"),
                alice.getUsername());
        ticketCommandService.assignTicket(ticket.getId(), daveIt.getId(), daveIt.getUsername());

        String secretNote = "CONFIDENTIAL_INTERNAL_DIAGNOSTIC_TOKEN_XYZ987";
        ticketCommandService.addComment(ticket.getId(),
                new TicketCommentRequestDTO(secretNote, true),
                daveIt.getUsername());

        List<InAppAlert> allAlerts = inAppAlertRepository.findAll();
        boolean leaked = allAlerts.stream()
                .anyMatch(a -> (a.getMessage() != null && a.getMessage().contains(secretNote))
                        || (a.getTitle() != null && a.getTitle().contains(secretNote)));
        assertFalse(leaked, "Internal specialist comment content must NEVER appear in any notification");
    }

    // --- Helper methods ---
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
        if (userRoleMappingRepo.findByUserId(user.getId()).isEmpty()) {
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(user);
            m.setRole(role);
            userRoleMappingRepo.saveAndFlush(m);
        }
        return user;
    }

    private void getOrCreateProfile(UserAccount user, String first, String last, String dept, String manager) {
        if (profileRepo.findByUserId(user.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(user);
            p.setEmployeeId("EMP-" + user.getId());
            p.setFirstName(first);
            p.setLastName(last);
            p.setDepartment(dept);
            p.setDesignation("Staff");
            p.setDateOfJoining(LocalDate.of(2024, 1, 1));
            p.setReportingManager(manager);
            profileRepo.saveAndFlush(p);
        }
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("accessToken");
        assertNotNull(cookie, "accessToken cookie must be returned on login");
        return cookie.getValue();
    }
}
