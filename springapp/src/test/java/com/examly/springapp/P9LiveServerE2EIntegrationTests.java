package com.examly.springapp;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P9 Live-Server HTTP End-to-End Integration Suite (RANDOM_PORT embedded Tomcat).
 * Exercises real TCP HTTP/1.1 transport, real HTTP-Only JWT Set-Cookie headers,
 * real XSRF-TOKEN cookie-to-header double-submit CSRF validation, silent refresh,
 * and complete cross-persona workflows E2E-01 through E2E-08.
 *
 * NOTE: This verifies real live-server HTTP workflows; browser-rendered Playwright E2E
 * execution is tracked separately in docs/qa/p9-e2e-report.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class P9LiveServerE2EIntegrationTests {

    @LocalServerPort
    private int port;

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

    private ServiceCatalogItem laptopItem;

    @BeforeEach
    void seedE2EData() {
        inAppAlertRepository.deleteAll();
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();

        Role rEmp = getOrCreateRole("ROLE_EMPLOYEE");
        Role rMgr = getOrCreateRole("ROLE_MANAGER");
        Role rIt = getOrCreateRole("ROLE_IT_SUPPORT");
        Role rFac = getOrCreateRole("ROLE_FACILITIES_CREW");
        Role rAuditor = getOrCreateRole("ROLE_AUDITOR");
        Role rAdmin = getOrCreateRole("ROLE_ADMIN");

        UserAccount mgr = getOrCreateUser("e2e_mgr_bob", "Password123!", rMgr);
        getOrCreateProfile(mgr, "Bob", "Manager", "IT", null);

        UserAccount emp = getOrCreateUser("e2e_emp_alice", "Password123!", rEmp);
        getOrCreateProfile(emp, "Alice", "Employee", "Engineering", "e2e_mgr_bob");

        UserAccount other = getOrCreateUser("e2e_emp_charlie", "Password123!", rEmp);
        getOrCreateProfile(other, "Charlie", "Other", "Finance", null);

        UserAccount itSpec = getOrCreateUser("e2e_it_dave", "Password123!", rIt);
        getOrCreateProfile(itSpec, "Dave", "IT", "IT", null);

        UserAccount facSpec = getOrCreateUser("e2e_fac_frank", "Password123!", rFac);
        getOrCreateProfile(facSpec, "Frank", "Facilities", "Facilities", null);

        UserAccount auditor = getOrCreateUser("e2e_auditor", "Password123!", rAuditor);
        getOrCreateProfile(auditor, "Arthur", "Auditor", "Audit", null);

        UserAccount admin = getOrCreateUser("e2e_admin", "Password123!", rAdmin);
        getOrCreateProfile(admin, "Super", "Admin", "Operations", null);

        laptopItem = serviceCatalogRepository.findByItemCode("E2E-IT-LAPTOP").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "E2E-IT-LAPTOP", "E2E Developer Laptop", "Hardware", "IT",
                        "Full lifecycle E2E test laptop", new BigDecimal("1800.00"), 16, true, false, true)));
    }

    @Test
    @Order(1)
    @DisplayName("E2E-01 & E2E-08: Real HTTP cookie attributes, CSRF validation, silent refresh, and logout")
    void test1_LiveCookieCsrfRefreshAndLogoutFlow() throws Exception {
        LiveSession alice = new LiveSession();

        // 1. Login -> verify Set-Cookie headers enforce HttpOnly and SameSite=Lax
        HttpResponse<String> loginRes = alice.postRaw("/api/auth/login",
                "{\"username\":\"e2e_emp_alice\",\"password\":\"Password123!\"}", false);
        assertEquals(200, loginRes.statusCode());

        List<String> setCookies = loginRes.headers().allValues("set-cookie");
        assertTrue(setCookies.stream().anyMatch(c -> c.contains("accessToken=") && c.contains("HttpOnly") && c.contains("SameSite=Lax")),
                "accessToken cookie must be HttpOnly and SameSite=Lax: " + setCookies);
        assertTrue(setCookies.stream().anyMatch(c -> c.contains("refreshToken=") && c.contains("HttpOnly") && c.contains("Path=/api/auth")),
                "refreshToken cookie must be HttpOnly and scoped to /api/auth: " + setCookies);

        // 2. Session restore via GET /api/users/me -> also initializes XSRF-TOKEN cookie
        HttpResponse<String> meRes = alice.get("/api/users/me");
        assertEquals(200, meRes.statusCode());
        assertEquals("e2e_emp_alice", objectMapper.readTree(meRes.body()).get("username").asText());

        // 3. Acquire CSRF token explicitly & verify mutation WITHOUT X-XSRF-TOKEN fails with 403
        alice.get("/api/auth/csrf");
        HttpResponse<String> noCsrfRes = alice.postRaw("/api/v1/tickets",
                "{\"catalogItemId\":" + laptopItem.getId() + ",\"department\":\"IT\",\"priority\":\"URGENT\",\"description\":\"Missing CSRF header test\"}",
                false);
        assertEquals(403, noCsrfRes.statusCode(), "Cookie-authenticated mutation without X-XSRF-TOKEN must be rejected with 403");

        // 4. Silent refresh via POST /api/auth/refresh rotates cookies cleanly
        HttpResponse<String> refreshRes = alice.postRaw("/api/auth/refresh", "{}", false);
        assertEquals(200, refreshRes.statusCode());

        // 5. Logout with valid CSRF token clears cookies -> subsequent /api/users/me returns 401
        HttpResponse<String> logoutRes = alice.postWithCsrf("/api/auth/logout", "{}");
        assertEquals(200, logoutRes.statusCode(), "Logout failed: body=" + logoutRes.body() + " xsrf=" + alice.getXsrfToken() + " cookies=" + alice.cookieManager.getCookieStore().getCookies());

        HttpResponse<String> afterLogout = alice.get("/api/users/me");
        assertEquals(401, afterLogout.statusCode(), "Session must be invalid after logout");
    }

    @Test
    @Order(2)
    @DisplayName("E2E-02 to E2E-07: Full Multi-Persona Lifecycle (Catalog -> Ticket -> Approval -> Queue -> Comment -> Notification -> Analytics -> Audit)")
    void test2_CompleteCrossPersonaLiveHttpLifecycle() throws Exception {
        LiveSession alice = loginSession("e2e_emp_alice", "Password123!");
        LiveSession charlie = loginSession("e2e_emp_charlie", "Password123!");
        LiveSession bobMgr = loginSession("e2e_mgr_bob", "Password123!");
        LiveSession daveIt = loginSession("e2e_it_dave", "Password123!");
        LiveSession frankFac = loginSession("e2e_fac_frank", "Password123!");
        LiveSession auditor = loginSession("e2e_auditor", "Password123!");

        // E2E-02: Alice browses catalog & submits URGENT ticket requiring manager approval
        HttpResponse<String> catRes = alice.get("/api/v1/catalog?department=IT");
        assertEquals(200, catRes.statusCode());
        assertTrue(catRes.body().contains("E2E Developer Laptop"));

        HttpResponse<String> createRes = alice.postWithCsrf("/api/v1/tickets",
                "{\"catalogItemId\":" + laptopItem.getId() + ",\"department\":\"IT\",\"priority\":\"URGENT\",\"description\":\"Live E2E developer workstation requisition\"}");
        assertEquals(201, createRes.statusCode(), "Create ticket failed: body=" + createRes.body() + " xsrf=" + alice.getXsrfToken() + " cookies=" + alice.cookieManager.getCookieStore().getCookies());
        JsonNode createdTicket = objectMapper.readTree(createRes.body());
        long ticketId = createdTicket.get("id").asLong();
        assertEquals("PENDING_APPROVAL", createdTicket.get("status").asText());
        assertEquals("URGENT", createdTicket.get("priority").asText());

        // E2E-08: Unrelated employee Charlie blocked from Alice's ticket & approval history (403)
        assertEquals(403, charlie.get("/api/v1/tickets/" + ticketId).statusCode());
        assertEquals(403, charlie.get("/api/v1/approvals/tickets/" + ticketId).statusCode());

        // E2E-04 & E2E-05: Bob receives APPROVAL_REQUIRED notification and approves ticket
        HttpResponse<String> bobUnread = bobMgr.get("/api/v1/notifications/my/unread-count");
        assertEquals(200, bobUnread.statusCode());
        assertTrue(objectMapper.readTree(bobUnread.body()).get("unreadCount").asLong() >= 1);

        HttpResponse<String> pendingRes = bobMgr.get("/api/v1/approvals/pending");
        assertEquals(200, pendingRes.statusCode());
        long taskId = objectMapper.readTree(pendingRes.body()).get(0).get("id").asLong();

        HttpResponse<String> approveRes = bobMgr.postWithCsrf("/api/v1/approvals/" + taskId + "/decide",
                "{\"decision\":\"APPROVED\",\"reason\":\"Approved for Q4 live E2E sprint\"}");
        assertEquals(200, approveRes.statusCode());
        assertEquals("ASSIGNED", objectMapper.readTree(approveRes.body()).get("newTicketStatus").asText());

        // E2E-03 & E2E-08: Facilities specialist Frank blocked from IT queue (403); IT specialist Dave claims & resolves
        assertEquals(403, frankFac.get("/api/v1/departments/IT/queue").statusCode());

        HttpResponse<String> itQueue = daveIt.get("/api/v1/departments/IT/queue");
        assertEquals(200, itQueue.statusCode());
        assertTrue(itQueue.body().contains(createdTicket.get("ticketNumber").asText()));

        long daveUserId = objectMapper.readTree(daveIt.get("/api/users/me").body()).get("id").asLong();
        HttpResponse<String> assignRes = daveIt.postWithCsrf("/api/v1/tickets/" + ticketId + "/assign",
                "{\"assigneeId\":" + daveUserId + "}");
        assertEquals(200, assignRes.statusCode(), "Assign failed: " + assignRes.body());
        long versionAfterAssign = objectMapper.readTree(assignRes.body()).get("version").asLong();

        // Dave posts internal note + public note; Alice sees ONLY public note
        assertEquals(201, daveIt.postWithCsrf("/api/v1/tickets/" + ticketId + "/comments",
                "{\"commentText\":\"INTERNAL_SECRET_ASSET_TAG_999\",\"isInternal\":true}").statusCode());
        assertEquals(201, daveIt.postWithCsrf("/api/v1/tickets/" + ticketId + "/comments",
                "{\"commentText\":\"Laptop provisioned and ready for pickup.\",\"isInternal\":false}").statusCode());

        HttpResponse<String> aliceComments = alice.get("/api/v1/tickets/" + ticketId + "/comments");
        assertEquals(200, aliceComments.statusCode());
        assertTrue(aliceComments.body().contains("Laptop provisioned"));
        assertFalse(aliceComments.body().contains("INTERNAL_SECRET_ASSET_TAG_999"), "Internal note must not leak to requester");

        // assignTicket transitions ticket to IN_PROGRESS; Dave now resolves ticket directly
        assertEquals("IN_PROGRESS", objectMapper.readTree(assignRes.body()).get("status").asText());
        HttpResponse<String> resolveRes = daveIt.postWithCsrf("/api/v1/tickets/" + ticketId + "/status",
                "{\"status\":\"RESOLVED\",\"reason\":\"Hardware delivered and verified.\",\"version\":" + versionAfterAssign + "}");
        assertEquals(200, resolveRes.statusCode(), "RESOLVED failed: " + resolveRes.body());
        assertEquals("RESOLVED", objectMapper.readTree(resolveRes.body()).get("status").asText());

        // E2E-05: Alice verifies notifications, marks one read, marks all read; Charlie blocked (403)
        HttpResponse<String> aliceNotifs = alice.get("/api/v1/notifications/my");
        assertEquals(200, aliceNotifs.statusCode());
        long aliceNotifId = objectMapper.readTree(aliceNotifs.body()).get("content").get(0).get("id").asLong();
        assertEquals(403, charlie.putWithCsrf("/api/v1/notifications/" + aliceNotifId + "/read", "{}").statusCode());
        assertEquals(200, alice.putWithCsrf("/api/v1/notifications/" + aliceNotifId + "/read", "{}").statusCode());
        assertEquals(200, alice.putWithCsrf("/api/v1/notifications/my/read-all", "{}").statusCode());
        assertEquals(0, objectMapper.readTree(alice.get("/api/v1/notifications/my/unread-count").body()).get("unreadCount").asLong());

        // E2E-06: Analytics dashboard reflects exact resolved ticket & blocks employee / cross-department
        assertEquals(403, alice.get("/api/v1/analytics/dashboard").statusCode());
        assertEquals(403, daveIt.get("/api/v1/analytics/dashboard?department=HR").statusCode());
        HttpResponse<String> daveAnalytics = daveIt.get("/api/v1/analytics/dashboard");
        assertEquals(200, daveAnalytics.statusCode());
        JsonNode dash = objectMapper.readTree(daveAnalytics.body());
        assertEquals(1, dash.get("overview").get("periodTotalTickets").asLong());
        assertEquals(1, dash.get("overview").get("resolvedTicketsInPeriod").asLong());
        assertEquals("IT", dash.get("overview").get("scopedDepartment").asText());

        // E2E-07: Auditor verifies cryptographic audit chain integrity; employee blocked (403)
        assertEquals(403, alice.get("/api/v1/audit/verify").statusCode());
        HttpResponse<String> verifyRes = auditor.get("/api/v1/audit/verify");
        assertEquals(200, verifyRes.statusCode());
        assertTrue(objectMapper.readTree(verifyRes.body()).get("valid").asBoolean(), "Audit chain must be cryptographically valid");
    }

    private LiveSession loginSession(String username, String password) throws Exception {
        LiveSession s = new LiveSession();
        HttpResponse<String> res = s.postRaw("/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", false);
        assertEquals(200, res.statusCode(), "Login failed for " + username);
        s.get("/api/auth/csrf");
        return s;
    }

    private class LiveSession {
        private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();

        HttpResponse<String> get(String path) throws Exception {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .GET()
                    .build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> postWithCsrf(String path, String jsonBody) throws Exception {
            return postRaw(path, jsonBody, true);
        }

        HttpResponse<String> postRaw(String path, String jsonBody, boolean includeCsrf) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            if (includeCsrf) {
                getXsrfToken().ifPresent(tok -> builder.header("X-XSRF-TOKEN", tok));
            }
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> putWithCsrf(String path, String jsonBody) throws Exception {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
            getXsrfToken().ifPresent(tok -> builder.header("X-XSRF-TOKEN", tok));
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }

        private Optional<String> getXsrfToken() {
            return cookieManager.getCookieStore().getCookies().stream()
                    .filter(c -> "XSRF-TOKEN".equals(c.getName()))
                    .map(HttpCookie::getValue)
                    .findFirst();
        }
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

    private void getOrCreateProfile(UserAccount user, String first, String last, String dept, String manager) {
        if (profileRepo.findByUserId(user.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(user);
            p.setEmployeeId("EMP-E2E-" + user.getId());
            p.setFirstName(first);
            p.setLastName(last);
            p.setDepartment(dept);
            p.setDesignation("Staff");
            p.setDateOfJoining(LocalDate.of(2024, 1, 1));
            p.setReportingManager(manager);
            profileRepo.saveAndFlush(p);
        }
    }
}

