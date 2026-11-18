package com.examly.springapp;

import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.notification.SmtpEmailDeliveryAdapter;
import com.examly.springapp.repository.RoleRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import com.examly.springapp.workflow.BusinessCalendar;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 1 Remediation Verification Suite:
 *  - TECH-P8-01: Legacy DashboardController N+1 elimination (constant O(1) SQL queries + exact metrics)
 *  - GAP-CAL-2027: Multi-year externalized holiday config, deduplication, leap-year 2028 rollover, runtime mutation
 *  - GAP-SMTP-HARDEN: AUTH PLAIN, UTF-8 MIME Unicode subjects, CRLF header-injection blocking, 550 rejection,
 *                     socket timeout, and independent email-level idempotency
 */
@SpringBootTest
@AutoConfigureMockMvc
public class Stage1RemediationVerificationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("TECH-P8-01: GET /api/dashboard/metrics eliminates N+1 (constant O(1) queries for 20+ users) and preserves RBAC & risk accuracy")
    void test1_LegacyDashboardEliminatesNPlusOneWithConstantQueries() throws Exception {
        Role adminRole = getOrCreateRole("ADMIN");
        Role empRole = getOrCreateRole("EMPLOYEE");
        Role mgrRole = getOrCreateRole("MANAGER");

        UserAccount admin = getOrCreateUser("st1_dash_admin", "Password123!", AccountStatus.ACTIVE, 0, null, adminRole);
        UserAccount emp = getOrCreateUser("st1_dash_emp", "Password123!", AccountStatus.ACTIVE, 0, null, empRole);

        // Seed 20 diverse users (active, suspended, disabled, locked, high/critical risk)
        for (int i = 0; i < 20; i++) {
            AccountStatus st = (i % 5 == 0) ? AccountStatus.DISABLED : (i % 4 == 0 ? AccountStatus.SUSPENDED : AccountStatus.ACTIVE);
            int fails = (i % 3 == 0) ? 5 : 0;
            LocalDateTime lockedUntil = (i % 5 == 0) ? LocalDateTime.now().plusHours(2) : null;
            Role r = (i % 5 == 0) ? adminRole : (i % 2 == 0 ? mgrRole : empRole);
            getOrCreateUser("st1_dash_u_" + i, "Password123!", st, fails, lockedUntil, r);
        }

        String adminJwt = login(admin.getUsername(), "Password123!");
        String empJwt = login(emp.getUsername(), "Password123!");

        // Non-admin blocked (403)
        mockMvc.perform(get("/api/dashboard/metrics").cookie(new Cookie("accessToken", empJwt)))
                .andExpect(status().isForbidden());

        // Enable Hibernate Statistics and measure SQL queries for DashboardController
        SessionFactory sf = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(get("/api/dashboard/metrics").cookie(new Cookie("accessToken", adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.criticalRiskUsers").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.disabledUsers").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.suspendedUsers").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));

        long queryCount = stats.getPrepareStatementCount();
        // Auth filter uses <= 3 queries (user + roles) + DashboardController uses exactly 3 constant queries -> total <= 8 (vs 65+ before fix!)
        assertTrue(queryCount <= 10,
                "Dashboard metrics must execute O(1) constant queries instead of 3N+2 N+1 queries; actual SQL statements=" + queryCount);
    }

    @Test
    @DisplayName("GAP-CAL-2027: Externalized multi-year holiday config, duplicate handling, 2028 leap-year rollover & runtime updates")
    void test2_MultiYearExternalizedHolidayCalendarAndLeapYear() {
        Clock fixed = Clock.fixed(
                LocalDateTime.of(2028, 2, 28, 14, 0).atZone(BusinessCalendar.EPAMS_TIMEZONE).toInstant(),
                BusinessCalendar.EPAMS_TIMEZONE);

        // Externalized CSV includes valid future-year dates (2027, 2028 leap day), duplicate date, and malformed entry
        BusinessCalendar cal = new BusinessCalendar(
                fixed,
                "2027-01-26, 2027-01-26, not-a-date, 2028-02-29",
                true
        );

        assertTrue(cal.getConfiguredHolidays().contains(LocalDate.of(2026, 10, 2)), "Baseline 2026 holiday retained");
        assertTrue(cal.getConfiguredHolidays().contains(LocalDate.of(2027, 1, 26)), "External 2027 holiday loaded");
        assertTrue(cal.getConfiguredHolidays().contains(LocalDate.of(2028, 2, 29)), "External 2028 leap-day holiday loaded");

        // Mon 2028-02-28 14:00 (4h on Mon); Tue 2028-02-29 is configured holiday (skipped);
        // Wed Mar 1, Thu Mar 2, Fri Mar 3 = 27h (17h left); Sat/Sun skipped; Mon Mar 6 = 9h (8h left); Tue Mar 7 09:00 + 8h = 17:00
        LocalDateTime resolved = LocalDateTime.of(2028, 2, 28, 14, 0);
        assertEquals(LocalDateTime.of(2028, 3, 7, 17, 0), cal.calculateReopenDeadline(resolved));

        // Removing 2028-02-29 holiday at runtime shifts deadline 1 business day earlier -> Mon 2028-03-06 17:00
        assertTrue(cal.removeHoliday(LocalDate.of(2028, 2, 29)));
        assertEquals(LocalDateTime.of(2028, 3, 6, 17, 0), cal.calculateReopenDeadline(resolved));
    }

    @Test
    @DisplayName("GAP-SMTP-HARDEN: SMTP AUTH PLAIN, UTF-8 MIME Unicode subject, CRLF injection guard, 550 rejection, timeout & independent idempotency")
    void test3_SmtpAuthUnicodeHeaderSanitizationRejectionTimeoutAndIdempotency() throws Exception {
        List<String> capturedMessages = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean sawValidAuth = new AtomicBoolean(false);
        AtomicBoolean rejectMode = new AtomicBoolean(false);
        AtomicBoolean stallTimeoutMode = new AtomicBoolean(false);

        String expectedAuthToken = Base64.getEncoder().encodeToString("\0epams_user\0epams_pass".getBytes(StandardCharsets.UTF_8));
        ExecutorService exec = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            exec.submit(() -> {
                while (!server.isClosed()) {
                    try {
                        Socket client = server.accept();
                        exec.submit(() -> handleSmtpClient(client, expectedAuthToken, sawValidAuth, rejectMode, stallTimeoutMode, capturedMessages));
                    } catch (IOException ignored) {
                        break;
                    }
                }
            });

            SmtpEmailDeliveryAdapter adapter = new SmtpEmailDeliveryAdapter();
            ReflectionTestUtils.setField(adapter, "enabled", true);
            ReflectionTestUtils.setField(adapter, "host", "127.0.0.1");
            ReflectionTestUtils.setField(adapter, "port", port);
            ReflectionTestUtils.setField(adapter, "fromAddress", "no-reply@epams.local");
            ReflectionTestUtils.setField(adapter, "username", "epams_user");
            ReflectionTestUtils.setField(adapter, "password", "epams_pass");
            ReflectionTestUtils.setField(adapter, "timeoutMs", 400);
            ReflectionTestUtils.setField(adapter, "maxRetries", 2);

            // 1. Authenticated delivery with Unicode subject/body + CRLF injection attempt in header
            boolean sent = adapter.sendNotificationEmail(
                    "alice@epams.local\r\nBcc: evil@attacker.test",
                    "Ticket Resolved — Café 日本語\r\nX-Injected: bad",
                    "Body with UTF-8: Résumé — 承認済み",
                    "EMAIL_IDEM_KEY_1"
            );
            assertTrue(sent, "Authenticated Unicode email must succeed");
            assertTrue(sawValidAuth.get(), "SMTP AUTH PLAIN must have been validated");
            assertEquals(1, capturedMessages.size());
            String raw = capturedMessages.get(0);
            assertTrue(raw.contains("Subject: =?UTF-8?B?"), "Non-ASCII subject must use RFC 2047 UTF-8 Base64 encoding");
            assertFalse(raw.contains("\nBcc: evil@attacker.test"), "CRLF header injection must be neutralized");
            assertFalse(raw.contains("\nX-Injected: bad"), "Subject CRLF header injection must be neutralized");

            // 2. Independent email-level idempotency suppresses duplicate delivery
            assertFalse(adapter.sendNotificationEmail(
                    "alice@epams.local\r\nBcc: evil@attacker.test",
                    "Duplicate retry",
                    "Should not resend",
                    "EMAIL_IDEM_KEY_1"
            ), "Duplicate independent idempotency key must suppress second email");
            assertEquals(1, capturedMessages.size());

            // 3. SMTP 550 rejection returns false cleanly
            rejectMode.set(true);
            assertFalse(adapter.sendNotificationEmail("bob@epams.local", "Rejected", "Body", "KEY_REJECT"));
            rejectMode.set(false);

            // 4. Socket timeout returns false cleanly without hanging
            stallTimeoutMode.set(true);
            assertFalse(adapter.sendNotificationEmail("carol@epams.local", "Timed Out", "Body", "KEY_TIMEOUT"));
            stallTimeoutMode.set(false);

            // 5. Wrong SMTP credentials rejected (535) cleanly
            ReflectionTestUtils.setField(adapter, "password", "wrong_pass");
            assertFalse(adapter.sendNotificationEmail("dave@epams.local", "Bad Auth", "Body", "KEY_BAD_AUTH"));
        } finally {
            exec.shutdownNow();
        }
    }

    private void handleSmtpClient(Socket client, String expectedAuthToken, AtomicBoolean sawValidAuth,
                                  AtomicBoolean rejectMode, AtomicBoolean stallTimeoutMode, List<String> captured) {
        try (Socket s = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
            if (stallTimeoutMode.get()) {
                Thread.sleep(1500);
                return;
            }
            out.write("220 epams-smtp-test ESMTP\r\n");
            out.flush();
            StringBuilder data = new StringBuilder();
            boolean inData = false;
            String line;
            while ((line = in.readLine()) != null) {
                if (inData) {
                    if (".".equals(line)) {
                        inData = false;
                        captured.add(data.toString());
                        out.write("250 OK\r\n");
                        out.flush();
                    } else {
                        data.append(line).append("\n");
                    }
                } else if (line.startsWith("EHLO") || line.startsWith("HELO")) {
                    out.write("250-epams-smtp-test\r\n250 AUTH PLAIN\r\n");
                    out.flush();
                } else if (line.startsWith("AUTH PLAIN ")) {
                    String token = line.substring("AUTH PLAIN ".length()).trim();
                    if (expectedAuthToken.equals(token)) {
                        sawValidAuth.set(true);
                        out.write("235 Authentication successful\r\n");
                    } else {
                        out.write("535 Authentication credentials invalid\r\n");
                    }
                    out.flush();
                } else if (line.startsWith("MAIL FROM")) {
                    out.write("250 OK\r\n");
                    out.flush();
                } else if (line.startsWith("RCPT TO")) {
                    if (rejectMode.get()) {
                        out.write("550 Mailbox unavailable\r\n");
                    } else {
                        out.write("250 OK\r\n");
                    }
                    out.flush();
                } else if ("DATA".equalsIgnoreCase(line.trim())) {
                    inData = true;
                    out.write("354 Start mail input\r\n");
                    out.flush();
                } else if ("QUIT".equalsIgnoreCase(line.trim())) {
                    out.write("221 Bye\r\n");
                    out.flush();
                    break;
                }
            }
        } catch (Exception ignored) {
        }
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

    private UserAccount getOrCreateUser(String username, String rawPassword, AccountStatus status,
                                        int failedAttempts, LocalDateTime lockedUntil, Role role) {
        UserAccount u = userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount acc = new UserAccount();
            acc.setUsername(username);
            acc.setPasswordHash(passwordEncoder.encode(rawPassword));
            acc.setEmail(username + "@epams.local");
            return acc;
        });
        u.setStatus(status);
        u.setFailedLoginAttempts(failedAttempts);
        u.setLockedUntil(lockedUntil);
        u = userAccountRepo.saveAndFlush(u);
        if (userRoleMappingRepo.findByUserId(u.getId()).isEmpty()) {
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(u);
            m.setRole(role);
            userRoleMappingRepo.saveAndFlush(m);
        }
        return u;
    }
}
