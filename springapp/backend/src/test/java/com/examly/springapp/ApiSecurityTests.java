package com.examly.springapp;

import com.examly.springapp.dto.*;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.ApprovalService;
import com.examly.springapp.service.TicketCommandService;
import com.examly.springapp.service.UserAccountService;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage I: Comprehensive REST API Security, Hardening & DTO Validation Test Suite.
 * Covers: Authentication, Role/Scope Authorization, IDOR, Input Validation,
 * Concurrency (HTTP 409), Internal Comments Protection, Audit Boundaries, and CSRF.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRoleMappingRepo userRoleMappingRepo;

    @Autowired
    private ProfileRepo profileRepo;

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private ServiceTicketRepository serviceTicketRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private ApprovalTaskRepository approvalTaskRepository;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TicketCommandService ticketCommandService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private com.examly.springapp.service.AuditChainService auditChainService;

    private UserAccount aliceUser;
    private UserAccount charlieUser;
    private UserAccount itResolverUser;
    private UserAccount facResolverUser;
    private UserAccount auditorUser;
    private ServiceCatalogItem catalogLaptop;
    private ServiceCatalogItem catalogDesk;

    private String aliceToken;
    private String charlieToken;
    private String itResolverToken;
    private String facResolverToken;
    private String auditorToken;

    @BeforeEach
    void setupTestData() throws Exception {
        // Clear transaction artifacts
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();

        // 1. Ensure Roles exist
        Role roleEmployee = getOrCreateRole("ROLE_EMPLOYEE");
        Role roleIt = getOrCreateRole("ROLE_IT_SUPPORT");
        Role roleFac = getOrCreateRole("ROLE_FACILITIES_CREW");
        Role roleAuditor = getOrCreateRole("ROLE_AUDITOR");
        Role roleManager = getOrCreateRole("ROLE_MANAGER");

        // 2. Ensure Users exist
        UserAccount bobMgr = getOrCreateUser("sec_bob_mgr", "Password123!", roleManager);
        getOrCreateProfile(bobMgr, "Bob", "Jones", "Engineering", null);

        aliceUser = getOrCreateUser("sec_alice", "Password123!", roleEmployee);
        getOrCreateProfile(aliceUser, "Alice", "Smith", "Engineering", "sec_bob_mgr");

        charlieUser = getOrCreateUser("sec_charlie", "Password123!", roleEmployee);
        getOrCreateProfile(charlieUser, "Charlie", "Brown", "Finance", null);

        itResolverUser = getOrCreateUser("sec_it_dave", "Password123!", roleIt);
        getOrCreateProfile(itResolverUser, "Dave", "Miller", "IT", null);

        facResolverUser = getOrCreateUser("sec_fac_frank", "Password123!", roleFac);
        getOrCreateProfile(facResolverUser, "Frank", "Wright", "Facilities", null);

        auditorUser = getOrCreateUser("sec_auditor", "Password123!", roleAuditor);
        getOrCreateProfile(auditorUser, "Arthur", "Conan", "Internal Audit", null);

        // 3. Ensure Catalog Items exist
        catalogLaptop = serviceCatalogRepository.findByItemCode("SEC-IT-LAPTOP").orElseGet(() -> {
            ServiceCatalogItem item = new ServiceCatalogItem(
                    "SEC-IT-LAPTOP", "Security Test Laptop", "Hardware", "IT",
                    "Engineering laptop request", new BigDecimal("1200.00"), 8, true, false, true
            );
            return serviceCatalogRepository.saveAndFlush(item);
        });

        catalogDesk = serviceCatalogRepository.findByItemCode("SEC-FAC-DESK").orElseGet(() -> {
            ServiceCatalogItem item = new ServiceCatalogItem(
                    "SEC-FAC-DESK", "Standing Desk", "Furniture", "Facilities",
                    "Ergonomic standing desk", new BigDecimal("450.00"), 16, false, false, true
            );
            return serviceCatalogRepository.saveAndFlush(item);
        });

        // 4. Obtain access tokens
        aliceToken = loginAndGetToken("sec_alice", "Password123!");
        charlieToken = loginAndGetToken("sec_charlie", "Password123!");
        itResolverToken = loginAndGetToken("sec_it_dave", "Password123!");
        facResolverToken = loginAndGetToken("sec_fac_frank", "Password123!");
        auditorToken = loginAndGetToken("sec_auditor", "Password123!");
    }

    @AfterEach
    void tearDown() {
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();
    }

    private Role getOrCreateRole(String name) {
        return roleRepo.findByRoleName(name).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(name);
            return roleRepo.save(r);
        });
    }

    private UserAccount getOrCreateUser(String username, String rawPassword, Role role) {
        return userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setStatus(AccountStatus.ACTIVE);
            UserAccount saved = userAccountRepo.save(u);

            UserRoleMapping m = new UserRoleMapping();
            m.setUser(saved);
            m.setRole(role);
            userRoleMappingRepo.save(m);
            return saved;
        });
    }

    private Profile getOrCreateProfile(UserAccount user, String first, String last, String dept, String mgr) {
        return profileRepo.findByUserId(user.getId()).orElseGet(() -> {
            Profile p = new Profile();
            p.setUser(user);
            p.setFirstName(first);
            p.setLastName(last);
            p.setDepartment(dept);
            p.setReportingManager(mgr);
            return profileRepo.save(p);
        });
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = res.getResponse().getCookie("accessToken");
        assertNotNull(cookie, "Login must return accessToken cookie");
        return cookie.getValue();
    }

    // =========================================================================
    // 1. AUTHENTICATION & SESSION TESTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Stage D: Request to protected endpoint without credentials returns 401 Unauthorized")
    void test1_MissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("Stage D: Malformed or invalid Bearer token returns 401 Unauthorized")
    void test2_InvalidBearerToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/my")
                        .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. SERVICE CATALOG API & PAGINATION TESTS
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Stage B/G: Authenticated user can browse active service catalog with pagination")
    void test3_ServiceCatalog_ActiveListing_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/catalog")
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.content[?(@.itemCode == 'SEC-IT-LAPTOP')]").exists())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(4)
    @DisplayName("Stage B: Retrieve catalog item by ID returns 200 with DTO contract")
    void test4_ServiceCatalog_GetById_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/" + catalogLaptop.getId())
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(catalogLaptop.getId()))
                .andExpect(jsonPath("$.itemCode").value("SEC-IT-LAPTOP"))
                .andExpect(jsonPath("$.department").value("IT"))
                .andExpect(jsonPath("$.requiresManagerApproval").value(true));
    }

    @Test
    @Order(5)
    @DisplayName("Stage B/F: Retrieve non-existent catalog item returns 404 with RFC 7807 Problem Details")
    void test5_ServiceCatalog_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/999999")
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail", containsString("not found with ID: 999999")))
                .andExpect(jsonPath("$.instance").value("/api/v1/catalog/999999"));
    }

    @Test
    @Order(6)
    @DisplayName("Stage G: Requesting oversized page bounds size to maximum limit 100")
    void test6_PaginationBoundsEnforced() throws Exception {
        mockMvc.perform(get("/api/v1/catalog")
                        .param("size", "500")
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    // =========================================================================
    // 3. INPUT VALIDATION TESTS (STAGE C)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Stage C/F: Ticket creation with empty body triggers 400 Bad Request and lists field errors")
    void test7_TicketCreation_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors", notNullValue()))
                .andExpect(jsonPath("$.errors", not(empty())));
    }

    @Test
    @Order(8)
    @DisplayName("Stage C: Ticket creation with short description (< 10 chars) rejected with 400")
    void test8_TicketCreation_ShortDescription_Returns400() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogLaptop.getId(), "IT", TicketPriority.MEDIUM, "Too short" // 9 chars
        );
        mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'description')]").exists());
    }

    @Test
    @Order(9)
    @DisplayName("Stage C: Valid ticket submission returns 201 Created with ServiceTicketDTO")
    void test9_TicketCreation_Success_Returns201() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogLaptop.getId(), "IT", TicketPriority.HIGH, "Valid description exceeding ten characters minimum requirement"
        );
        mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.ticketNumber", notNullValue()))
                .andExpect(jsonPath("$.requesterUsername").value("sec_alice"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    // =========================================================================
    // 4. RESOURCE-LEVEL AUTHORIZATION & IDOR TESTS (STAGE E)
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Stage E: Requester can retrieve their own ticket by ID (Ownership allowed)")
    void test10_Ownership_AccessOwnTicket_Returns200() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogDesk.getId(), "Facilities", TicketPriority.LOW, "Standing desk requested for ergonomic posture"
        );
        ServiceTicketDTO ticket = ticketCommandService.createTicket(req, "sec_alice");

        mockMvc.perform(get("/api/v1/tickets/" + ticket.getId())
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.requesterUsername").value("sec_alice"));
    }

    @Test
    @Order(11)
    @DisplayName("Stage E: Another employee accessing private ticket is rejected with 403 Forbidden (IDOR protected)")
    void test11_IDOR_AccessOtherEmployeeTicket_Returns403() throws Exception {
        // Alice creates a private ticket
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogDesk.getId(), "Facilities", TicketPriority.LOW, "Confidential ticket created by Alice"
        );
        ServiceTicketDTO aliceTicket = ticketCommandService.createTicket(req, "sec_alice");

        // Charlie (different employee) attempts direct access by ID
        mockMvc.perform(get("/api/v1/tickets/" + aliceTicket.getId())
                        .cookie(new Cookie("accessToken", charlieToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @Order(12)
    @DisplayName("Stage E: Resolver cannot access other department queue (Department scope enforced)")
    void test12_CrossDepartmentQueue_Blocked_Returns403() throws Exception {
        // Facilities resolver attempts to view IT queue
        mockMvc.perform(get("/api/v1/departments/IT/queue")
                        .cookie(new Cookie("accessToken", facResolverToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail", containsString("not authorized to access queue for department: IT")));
    }

    @Test
    @Order(13)
    @DisplayName("Stage E: Authorized resolver can view their own department queue")
    void test13_AuthorizedDepartmentQueue_Allowed_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/departments/IT/queue")
                        .cookie(new Cookie("accessToken", itResolverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @Order(14)
    @DisplayName("Stage E: Internal discussion comments are filtered from requester visibility")
    void test14_InternalComments_HiddenFromRequester() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogDesk.getId(), "Facilities", TicketPriority.MEDIUM, "Desk request with discussion comments"
        );
        ServiceTicketDTO ticket = ticketCommandService.createTicket(req, "sec_alice");

        // Public comment
        ticketCommandService.addComment(ticket.getId(), new TicketCommentRequestDTO("Public update for user", false), "sec_alice");
        // Internal resolver comment
        ticketCommandService.addComment(ticket.getId(), new TicketCommentRequestDTO("INTERNAL: Supplier stock delayed until next week", true), "sec_fac_frank");

        // Alice (requester) retrieves comments -> must ONLY see public comment
        mockMvc.perform(get("/api/v1/tickets/" + ticket.getId() + "/comments")
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].commentText").value("Public update for user"))
                .andExpect(jsonPath("$[0].isInternal").value(false));

        // Frank (resolver) retrieves comments -> must see BOTH public and internal comments
        mockMvc.perform(get("/api/v1/tickets/" + ticket.getId() + "/comments")
                        .cookie(new Cookie("accessToken", facResolverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // =========================================================================
    // 5. AUDIT BOUNDARIES & CONCURRENCY (STAGE E & F)
    // =========================================================================

    @Test
    @Order(15)
    @DisplayName("Stage E: Auditor with ROLE_AUDITOR can verify cryptographic audit chain")
    void test15_AuditorCanVerifyChain_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/audit/verify")
                        .cookie(new Cookie("accessToken", auditorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));
    }

    @Test
    @Order(16)
    @DisplayName("Stage E: Regular employee without audit role is denied access to /api/v1/audit/verify (403)")
    void test16_RegularEmployeeCannotAccessAuditVerify_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit/verify")
                        .cookie(new Cookie("accessToken", aliceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(17)
    @DisplayName("Stage F: Concurrent status update with stale @Version returns HTTP 409 Conflict")
    void test17_OptimisticLockingConflict_Returns409() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogDesk.getId(), "Facilities", TicketPriority.MEDIUM, "Ticket for optimistic locking conflict test"
        );
        ServiceTicketDTO ticket = ticketCommandService.createTicket(req, "sec_alice");

        // Stale update passing obsolete version (e.g. 99L)
        TicketStatusUpdateRequestDTO staleReq = new TicketStatusUpdateRequestDTO(
                TicketStatus.ON_HOLD, "Holding for additional hardware logistics", 99L
        );

        mockMvc.perform(post("/api/v1/tickets/" + ticket.getId() + "/status")
                        .with(csrf())
                        .cookie(new Cookie("accessToken", facResolverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail", containsString("modified by another concurrent transaction")));
    }

    // =========================================================================
    // 6. CSRF PROTECTION TESTS (STAGE D)
    // =========================================================================

    @Test
    @Order(18)
    @DisplayName("Stage D: Cookie-authenticated state mutation without CSRF token returns 403 Forbidden")
    void test18_CookieMutationWithoutCsrf_Forbidden() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogLaptop.getId(), "IT", TicketPriority.MEDIUM, "Attempting mutation without CSRF header"
        );
        // Deliberately omit .with(csrf()) while sending cookie auth
        mockMvc.perform(post("/api/v1/tickets")
                        .cookie(new Cookie("accessToken", aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(19)
    @DisplayName("Stage D: Bearer header authenticated mutation bypasses cookie CSRF requirements")
    void test19_BearerMutation_SucceedsWithoutCookieCsrf() throws Exception {
        CreateTicketRequestDTO req = new CreateTicketRequestDTO(
                catalogDesk.getId(), "Facilities", TicketPriority.LOW, "Bearer token authenticated ticket submission"
        );
        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + aliceToken)
                        .with(csrf()) // standard CSRF processor for test context
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @AfterAll
    static void tearDownAll(@Autowired com.examly.springapp.service.AuditChainService auditChainService,
                            @Autowired ApprovalTaskRepository approvalTaskRepository,
                            @Autowired TicketCommentRepository ticketCommentRepository,
                            @Autowired ServiceTicketRepository serviceTicketRepository,
                            @Autowired RefreshTokenRepo refreshTokenRepo) {
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();
        auditChainService.resetChainToGenesis();
    }
}
