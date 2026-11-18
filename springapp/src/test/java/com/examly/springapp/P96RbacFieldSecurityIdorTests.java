package com.examly.springapp;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P9.6 Stages G & J — Complete Role-Based Access Control (RBAC), Field-Level Security,
 * Mass Assignment Protection, IDOR/BOLA Defense, Security Headers, and Session Invalidation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class P96RbacFieldSecurityIdorTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private ProfileRepo profileRepo;
    @Autowired private ServiceCatalogRepository catalogRepository;
    @Autowired private ServiceTicketRepository ticketRepository;
    @Autowired private TicketCommentRepository commentRepository;
    @Autowired private InAppAlertRepository alertRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;

    private UserAccount empA;
    private UserAccount empB;
    private UserAccount itResolver;
    private UserAccount auditor;
    private Profile profileB;
    private ServiceCatalogItem itCatalog;
    private ServiceTicket ticketOfEmpB;
    private InAppAlert alertOfEmpB;

    @BeforeEach
    void setUp() {
        ensureUser("sec_admin", "ADMIN", "Executive");
        ensureUser("sec_hr", "HR_MANAGER", "HR");
        auditor = ensureUser("sec_auditor", "AUDITOR", "Compliance");
        itResolver = ensureUser("sec_it_res", "IT_SUPPORT", "IT");
        empA = ensureUser("sec_emp_a", "EMPLOYEE", "Engineering");
        empB = ensureUser("sec_emp_b", "EMPLOYEE", "Finance");
        profileB = profileRepo.findByUserId(empB.getId()).orElseThrow();

        itCatalog = catalogRepository.findByItemCode("SEC-IT-01").orElseGet(() -> {
            ServiceCatalogItem item = new ServiceCatalogItem();
            item.setItemCode("SEC-IT-01");
            item.setTitle("Security Test IT Service");
            item.setCategory("Access");
            item.setDepartment("IT");
            item.setDescription("IT access provisioning for RBAC/IDOR security suite.");
            item.setEstimatedCost(BigDecimal.ZERO);
            item.setSlaTargetHours(16);
            item.setRequiresManagerApproval(false);
            item.setRequiresSecondaryApproval(false);
            item.setActive(true);
            return catalogRepository.save(item);
        });

        ServiceTicket t = new ServiceTicket();
        t.setTicketNumber("TKT-SEC-IDOR-01");
        t.setCatalogItem(itCatalog);
        t.setRequester(empB);
        t.setAssignee(itResolver);
        t.setDepartment("IT");
        t.setPriority(TicketPriority.HIGH);
        t.setStatus(TicketStatus.ASSIGNED);
        t.setDescription("Confidential Finance workstation ticket owned by empB.");
        t.setCreatedAt(LocalDateTime.now().minusHours(2));
        t.setSlaTargetDate(LocalDateTime.now().plusHours(14));
        ticketOfEmpB = ticketRepository.save(t);

        TicketComment internalNote = new TicketComment();
        internalNote.setTicket(ticketOfEmpB);
        internalNote.setAuthor(itResolver);
        internalNote.setCommentText("INTERNAL_RESOLVER_SECRET_NOTE: Escalation diagnostics.");
        internalNote.setInternal(true);
        internalNote.setCreatedAt(LocalDateTime.now());
        commentRepository.save(internalNote);

        TicketComment publicNote = new TicketComment();
        publicNote.setTicket(ticketOfEmpB);
        publicNote.setAuthor(empB);
        publicNote.setCommentText("Public requester clarification message.");
        publicNote.setInternal(false);
        publicNote.setCreatedAt(LocalDateTime.now());
        commentRepository.save(publicNote);

        InAppAlert alert = new InAppAlert();
        alert.setRecipient(empB);
        alert.setAlertType("TICKET_SUBMITTED");
        alert.setTitle("Private Alert for Emp B");
        alert.setMessage("Only Emp B may read or modify this alert.");
        alert.setRelatedEntityType("TICKET");
        alert.setRelatedEntityId(ticketOfEmpB.getId());
        alert.setDedupKey("SEC_ALERT_EMPB_" + System.nanoTime());
        alert.setRead(false);
        alert.setCreatedAt(LocalDateTime.now());
        alertOfEmpB = alertRepository.save(alert);
    }

    private UserAccount ensureUser(String username, String roleName, String department) {
        return userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount ua = new UserAccount();
            ua.setUsername(username);
            ua.setPasswordHash(passwordEncoder.encode("SecPass!2026"));
            ua.setEmail(username + "@epams.test");
            ua.setPhone("9111122222");
            ua.setStatus(AccountStatus.ACTIVE);
            ua = userAccountRepo.save(ua);

            Role role = roleRepo.findByRoleName(roleName).orElseGet(() -> {
                Role r = new Role();
                r.setRoleName(roleName);
                return roleRepo.save(r);
            });
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(ua);
            m.setRole(role);
            userRoleMappingRepo.save(m);

            Profile p = new Profile();
            p.setEmployeeId("EMP-" + username.toUpperCase());
            p.setFirstName("Sec");
            p.setLastName(username);
            p.setDepartment(department);
            p.setDesignation(roleName);
            p.setSalary(115000.0);
            p.setAddress("Confidential Street 99");
            p.setEmergencyContact("9999988888");
            p.setUser(ua);
            profileRepo.save(p);
            return ua;
        });
    }

    @Test
    @WithMockUser(username = "sec_emp_a", roles = {"EMPLOYEE"})
    @DisplayName("P96-SEC-01: IDOR/BOLA defense blocks cross-user tickets, comments, notifications, profiles, risk, and queues")
    void testIdorAndBolaBlockedForRegularEmployee() throws Exception {
        // Cross-user ticket read, comment, cancel -> 403
        mockMvc.perform(get("/api/v1/tickets/" + ticketOfEmpB.getId()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/" + ticketOfEmpB.getId() + "/comments"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/tickets/" + ticketOfEmpB.getId() + "/cancel").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Unauthorized cancel attempt on colleague ticket.\"}"))
                .andExpect(status().isForbidden());

        // Cross-user notification mark-as-read -> 403 or 404 (never 200)
        int notifStatus = mockMvc.perform(put("/api/v1/notifications/" + alertOfEmpB.getId() + "/read").with(csrf()))
                .andReturn().getResponse().getStatus();
        assertTrue(notifStatus == 403 || notifStatus == 404,
                "Cross-user notification read must be denied (403/404), got: " + notifStatus);

        // Cross-user profile details, user risk, admin dashboard, analytics, audit, and queues -> 403
        mockMvc.perform(get("/api/profiles/" + profileB.getId() + "/details")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/" + empB.getId() + "/risk")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/metrics")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/analytics/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit/logs")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit/verify")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/departments/IT/queue")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sec_emp_b", roles = {"EMPLOYEE"})
    @DisplayName("P96-SEC-02: Field-level access hides internal comments, redacts directory PII/salary, and ignores mass-assignment fields")
    void testFieldLevelAccessAndMassAssignmentProtection() throws Exception {
        // Requester sees public comment but NEVER internal resolver comment
        mockMvc.perform(get("/api/v1/tickets/" + ticketOfEmpB.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Public requester clarification")))
                .andExpect(content().string(not(containsString("INTERNAL_RESOLVER_SECRET_NOTE"))));

        // Non-HR/Admin directory response redacts salary, dob, address, emergencyContact
        mockMvc.perform(get("/api/profiles/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].salary").doesNotExist())
                .andExpect(jsonPath("$.content[0].address").doesNotExist())
                .andExpect(jsonPath("$.content[0].emergencyContact").doesNotExist());

        // Mass-assignment attempt on POST /api/v1/tickets cannot override status, requester, or assignee
        Map<String, Object> massAssign = new LinkedHashMap<>();
        massAssign.put("catalogItemId", itCatalog.getId());
        massAssign.put("department", "IT");
        massAssign.put("priority", "LOW");
        massAssign.put("description", "Testing server-side protection against mass assignment.");
        massAssign.put("status", "CLOSED");
        massAssign.put("requesterId", empA.getId());
        massAssign.put("assigneeId", itResolver.getId());

        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(massAssign)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(not("CLOSED")))
                .andExpect(jsonPath("$.requesterUsername").value("sec_emp_b"));
    }

    @Test
    @WithMockUser(username = "sec_auditor", roles = {"AUDITOR"})
    @DisplayName("P96-SEC-03: Auditor has read-only governance access and is strictly denied all mutations & passwordHash")
    void testAuditorReadOnlyBoundariesAndNoPasswordHashLeak() throws Exception {
        // Permitted reads (and passwordHash NEVER serialized!)
        mockMvc.perform(get("/api/v1/audit/logs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit/verify")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/analytics/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("$2a$"))));

        // Denied mutations -> 403
        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rogue\",\"passwordHash\":\"bad\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/roles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"SUPER\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/profiles/" + profileB.getId()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/profiles/employee").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rogue_emp\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sec_it_res", roles = {"IT_SUPPORT"})
    @DisplayName("P96-SEC-04: Cross-department resolver queue isolation blocks IT specialist from HR queue")
    void testCrossDepartmentQueueIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/departments/IT/queue")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/departments/HR/queue")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/" + ticketOfEmpB.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INTERNAL_RESOLVER_SECRET_NOTE")));
    }

    @Test
    @DisplayName("P96-SEC-05: Security headers present and previously issued JWT rejected immediately after account suspension")
    void testSecurityHeadersAndSuspendedAccountTokenInvalidation() throws Exception {
        // Security headers verified on live response
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Security-Policy"));

        // Issue valid JWT for empA, verify access succeeds, then suspend empA -> immediate 401!
        UserDetails ud = userDetailsService.loadUserByUsername("sec_emp_a");
        String jwt = jwtService.generateToken(ud);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sec_emp_a"));

        empA.setStatus(AccountStatus.SUSPENDED);
        userAccountRepo.saveAndFlush(empA);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }
}
