package com.examly.springapp;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P8 Business Analytics Test Suite:
 * Verifies metric formulas, SLA calculation accuracy, ON_HOLD pause exclusion,
 * reopened/cancelled/rejected handling, date validation, Asia/Kolkata timezone,
 * empty datasets, and strict role/department authorization boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsTests {

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
    @Autowired private Clock clock;

    private UserAccount adminUser;
    private UserAccount auditorUser;
    private UserAccount itSpecialist;
    private UserAccount hrManager;
    private UserAccount plainEmployee;

    private ServiceCatalogItem itLaptop;
    private ServiceCatalogItem hrLetter;

    private String adminJwt;
    private String auditorJwt;
    private String itJwt;
    private String hrMgrJwt;
    private String empJwt;

    @BeforeEach
    void setup() throws Exception {
        inAppAlertRepository.deleteAll();
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();

        Role rAdmin = getOrCreateRole("ROLE_ADMIN");
        Role rAuditor = getOrCreateRole("ROLE_AUDITOR");
        Role rIt = getOrCreateRole("ROLE_IT_SUPPORT");
        Role rMgr = getOrCreateRole("ROLE_MANAGER");
        Role rEmp = getOrCreateRole("ROLE_EMPLOYEE");

        adminUser = getOrCreateUser("p8_ana_admin", "Password123!", rAdmin);
        getOrCreateProfile(adminUser, "Admin", "Ops", "Operations");

        auditorUser = getOrCreateUser("p8_ana_auditor", "Password123!", rAuditor);
        getOrCreateProfile(auditorUser, "Audit", "Lead", "Audit");

        itSpecialist = getOrCreateUser("p8_ana_it", "Password123!", rIt);
        getOrCreateProfile(itSpecialist, "Ian", "Tech", "IT");

        hrManager = getOrCreateUser("p8_ana_hrmgr", "Password123!", rMgr);
        getOrCreateProfile(hrManager, "Helen", "HR", "HR");

        plainEmployee = getOrCreateUser("p8_ana_emp", "Password123!", rEmp);
        getOrCreateProfile(plainEmployee, "Evan", "Staff", "Engineering");

        itLaptop = serviceCatalogRepository.findByItemCode("P8-ANA-LAPTOP").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-ANA-LAPTOP", "Developer Laptop", "Hardware", "IT",
                        "Enterprise laptop", new BigDecimal("1500.00"), 16, true, false, true)));

        hrLetter = serviceCatalogRepository.findByItemCode("P8-ANA-HRLETTER").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-ANA-HRLETTER", "Employment Verification Letter", "Documents", "HR",
                        "Official HR letter", new BigDecimal("0.00"), 8, false, false, true)));

        adminJwt = login("p8_ana_admin", "Password123!");
        auditorJwt = login("p8_ana_auditor", "Password123!");
        itJwt = login("p8_ana_it", "Password123!");
        hrMgrJwt = login("p8_ana_hrmgr", "Password123!");
        empJwt = login("p8_ana_emp", "Password123!");
    }

    @Test
    @Order(1)
    void test1_AuthorizationEnforcedForEmployeeAndCrossDepartmentScope() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isUnauthorized());

        // Plain employee -> 403 Forbidden
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .cookie(new Cookie("accessToken", empJwt)))
                .andExpect(status().isForbidden());

        // IT specialist requesting HR analytics -> 403 Forbidden
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .param("department", "HR")
                        .cookie(new Cookie("accessToken", itJwt)))
                .andExpect(status().isForbidden());

        // IT specialist omitting department -> auto-scoped to IT (200 OK)
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .cookie(new Cookie("accessToken", itJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopedDepartment").value("IT"));

        // Auditor -> org-wide read-only access (200 OK)
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .cookie(new Cookie("accessToken", auditorJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopedDepartment").value("ALL"));
    }

    @Test
    @Order(2)
    void test2_EmptyDatasetReturnsZeroCountsAndNullRatesWithoutFabrication() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodTotalTickets").value(0))
                .andExpect(jsonPath("$.openTicketsSnapshot").value(0))
                .andExpect(jsonPath("$.slaComplianceRate").value(nullValue()))
                .andExpect(jsonPath("$.averageResolutionHours").value(nullValue()))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));
    }

    @Test
    @Order(3)
    void test3_InvalidDateRangesRejectedWith400BadRequest() throws Exception {
        LocalDate today = LocalDate.now(clock);

        // startDate > endDate
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .param("startDate", today.toString())
                        .param("endDate", today.minusDays(5).toString())
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isBadRequest());

        // Range > 366 days
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .param("startDate", today.minusDays(400).toString())
                        .param("endDate", today.toString())
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isBadRequest());

        // Malformed date format
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .param("startDate", "not-a-date")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void test4_SlaComplianceAndAverageResolutionSubtractPausedMinutesAndExcludeCancelledRejected() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);

        // Ticket 1 (IT): RESOLVED on time — created 6h ago, resolved 1h ago, paused 60 mins -> net resolution = 4.0h, MET SLA
        saveTicket("P8-TKT-001", itLaptop, "IT", TicketPriority.HIGH, TicketStatus.RESOLVED,
                now.minusHours(6), now.minusHours(1), now.plusHours(4), 60);

        // Ticket 2 (IT): CLOSED late — created 10h ago, resolved 2h ago, paused 0 mins -> net resolution = 8.0h, BREACHED SLA
        saveTicket("P8-TKT-002", itLaptop, "IT", TicketPriority.URGENT, TicketStatus.CLOSED,
                now.minusHours(10), now.minusHours(2), now.minusHours(3), 0);

        // Ticket 3 (IT): IN_PROGRESS and overdue -> counts as evaluated & breached & activeOverdue
        saveTicket("P8-TKT-003", itLaptop, "IT", TicketPriority.CRITICAL, TicketStatus.IN_PROGRESS,
                now.minusHours(20), null, now.minusHours(2), 0);

        // Ticket 4 (IT): ON_HOLD past target date -> paused clock, MUST NOT count as overdue!
        saveTicket("P8-TKT-004", itLaptop, "IT", TicketPriority.MEDIUM, TicketStatus.ON_HOLD,
                now.minusHours(20), null, now.minusHours(2), 120);

        // Ticket 5 (IT): CANCELLED -> MUST BE EXCLUDED from SLA evaluation!
        saveTicket("P8-TKT-005", itLaptop, "IT", TicketPriority.LOW, TicketStatus.CANCELLED,
                now.minusHours(5), null, now.minusHours(1), 0);

        // Ticket 6 (HR): RESOLVED on time -> isolated to HR
        saveTicket("P8-TKT-006", hrLetter, "HR", TicketPriority.LOW, TicketStatus.RESOLVED,
                now.minusHours(4), now.minusHours(2), now.plusHours(4), 0);

        // Verify IT-scoped SLA compliance:
        // Evaluated IT tickets = T1 (met), T2 (breached), T3 (active overdue breached) = 3 total, 1 met -> 33.33%
        // Avg resolution hours across T1 (4.0h) and T2 (8.0h) = 6.0h
        mockMvc.perform(get("/api/v1/analytics/sla/compliance")
                        .cookie(new Cookie("accessToken", itJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluatedTickets").value(3))
                .andExpect(jsonPath("$.metSlaCount").value(1))
                .andExpect(jsonPath("$.breachedSlaCount").value(2))
                .andExpect(jsonPath("$.activeOverdueCount").value(1))
                .andExpect(jsonPath("$.compliancePercentage").value(33.33))
                .andExpect(jsonPath("$.averageResolutionHours").value(6.0));
    }

    @Test
    @Order(5)
    void test5_PriorityAndStatusDistributionsCoverAllFivePriorities() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        saveTicket("P8-PRI-1", itLaptop, "IT", TicketPriority.LOW, TicketStatus.SUBMITTED, now.minusHours(2), null, now.plusHours(8), 0);
        saveTicket("P8-PRI-2", itLaptop, "IT", TicketPriority.URGENT, TicketStatus.IN_PROGRESS, now.minusHours(2), null, now.plusHours(8), 0);
        saveTicket("P8-PRI-3", hrLetter, "HR", TicketPriority.CRITICAL, TicketStatus.RESOLVED, now.minusHours(4), now.minusHours(1), now.plusHours(8), 0);

        mockMvc.perform(get("/api/v1/analytics/tickets/priority-distribution")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.priority=='URGENT')].count", contains(1)))
                .andExpect(jsonPath("$[?(@.priority=='CRITICAL')].count", contains(1)));

        mockMvc.perform(get("/api/v1/analytics/tickets/status-distribution")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @Order(6)
    void test6_TrendsDepartmentPerformanceAndServicePopularityAggregateAccurately() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        saveTicket("P8-TRD-1", itLaptop, "IT", TicketPriority.HIGH, TicketStatus.SUBMITTED, now.minusDays(1), null, now.plusHours(12), 0);
        saveTicket("P8-TRD-2", itLaptop, "IT", TicketPriority.MEDIUM, TicketStatus.ASSIGNED, now, null, now.plusHours(12), 0);
        saveTicket("P8-TRD-3", hrLetter, "HR", TicketPriority.LOW, TicketStatus.RESOLVED, now, now, now.plusHours(8), 0);

        // 3-day trend window should return exactly 3 daily buckets (zero-filled where empty)
        mockMvc.perform(get("/api/v1/analytics/tickets/trends")
                        .param("startDate", today.minusDays(2).toString())
                        .param("endDate", today.toString())
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].submittedCount").value(0))
                .andExpect(jsonPath("$[1].submittedCount").value(1))
                .andExpect(jsonPath("$[2].submittedCount").value(2))
                .andExpect(jsonPath("$[2].resolvedCount").value(1));

        // Department performance: IT has openBacklog=2, HR has openBacklog=0 -> IT sorted first
        mockMvc.perform(get("/api/v1/analytics/departments/performance")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("IT"))
                .andExpect(jsonPath("$[0].openBacklogSnapshot").value(2))
                .andExpect(jsonPath("$[1].department").value("HR"))
                .andExpect(jsonPath("$[1].resolvedInPeriod").value(1));

        // Service popularity: Developer Laptop requested twice vs HR Letter once
        mockMvc.perform(get("/api/v1/analytics/services/popularity")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemCode").value("P8-ANA-LAPTOP"))
                .andExpect(jsonPath("$[0].requestCount").value(2));
    }

    @Test
    @Order(7)
    void test7_ApprovalOverviewTracksPendingStageOverdueAndDecisions() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        ServiceTicket t1 = saveTicket("P8-APP-1", itLaptop, "IT", TicketPriority.HIGH, TicketStatus.PENDING_APPROVAL,
                now.minusHours(30), null, now.plusHours(12), 0);
        ServiceTicket t2 = saveTicket("P8-APP-2", hrLetter, "HR", TicketPriority.LOW, TicketStatus.REJECTED,
                now.minusHours(3), null, now.plusHours(12), 0);

        // Overdue pending Stage 1 task (created 30h ago > 24h threshold)
        ApprovalTask overdueTask = new ApprovalTask(t1, hrManager, 1);
        overdueTask.setCreatedAt(now.minusHours(30));
        approvalTaskRepository.saveAndFlush(overdueTask);

        // Rejected task in period
        ApprovalTask rejectedTask = new ApprovalTask(t2, hrManager, 1);
        rejectedTask.setCreatedAt(now.minusHours(3));
        rejectedTask.decide(ApprovalStatus.REJECTED, "Budget frozen for this quarter", now.minusHours(2));
        approvalTaskRepository.saveAndFlush(rejectedTask);

        mockMvc.perform(get("/api/v1/analytics/approvals/overview")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSnapshot").value(1))
                .andExpect(jsonPath("$.stage1PendingSnapshot").value(1))
                .andExpect(jsonPath("$.overduePendingSnapshot").value(1))
                .andExpect(jsonPath("$.rejectedInPeriod").value(1));

        // HR Manager sees only HR approvals (0 pending, 1 rejected)
        mockMvc.perform(get("/api/v1/analytics/approvals/overview")
                        .cookie(new Cookie("accessToken", hrMgrJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSnapshot").value(0))
                .andExpect(jsonPath("$.rejectedInPeriod").value(1));
    }

    @Test
    @Order(8)
    void test8_DashboardBundleReturnsCohesiveSingleRoundtripPayload() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        saveTicket("P8-BND-1", itLaptop, "IT", TicketPriority.HIGH, TicketStatus.SUBMITTED, now.minusHours(1), null, now.plusHours(16), 0);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.periodTotalTickets").value(1))
                .andExpect(jsonPath("$.statusDistribution").isArray())
                .andExpect(jsonPath("$.priorityDistribution", hasSize(5)))
                .andExpect(jsonPath("$.trends").isArray())
                .andExpect(jsonPath("$.departmentPerformance").isArray())
                .andExpect(jsonPath("$.servicePopularity").isArray())
                .andExpect(jsonPath("$.slaCompliance").isMap())
                .andExpect(jsonPath("$.approvalsOverview").isMap());
    }

    private ServiceTicket saveTicket(String number, ServiceCatalogItem item, String dept,
                                     TicketPriority priority, TicketStatus status,
                                     LocalDateTime createdAt, LocalDateTime resolvedAt,
                                     LocalDateTime slaTarget, int pausedMins) {
        ServiceTicket t = new ServiceTicket();
        t.setTicketNumber(number);
        t.setCatalogItem(item);
        t.setRequester(plainEmployee);
        t.setDepartment(dept);
        t.setPriority(priority);
        t.setStatus(status);
        t.setDescription("Analytics verification ticket " + number);
        t.setCreatedAt(createdAt);
        t.setResolvedAt(resolvedAt);
        t.setSlaTargetDate(slaTarget);
        t.setTotalPausedMinutes(pausedMins);
        return serviceTicketRepository.saveAndFlush(t);
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
        if (userRoleMappingRepo.findByUserId(user.getId()).isEmpty()) {
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(user);
            m.setRole(role);
            userRoleMappingRepo.saveAndFlush(m);
        }
        return user;
    }

    private void getOrCreateProfile(UserAccount user, String first, String last, String dept) {
        if (profileRepo.findByUserId(user.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(user);
            p.setEmployeeId("EMP-" + user.getId());
            p.setFirstName(first);
            p.setLastName(last);
            p.setDepartment(dept);
            p.setDesignation("Staff");
            p.setDateOfJoining(LocalDate.of(2024, 1, 1));
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
