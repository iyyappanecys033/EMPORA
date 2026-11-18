package com.examly.springapp;

import com.examly.springapp.config.CorrelationIdFilter;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P8 Performance Benchmarking & Observability Test Suite.
 * Separates functional/observability assertions from hardware-sensitive timings
 * while recording reproducible latency percentiles (Avg, P50, P95) and throughput.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceBenchmarkTests {

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
    private UserAccount employeeUser;
    private String adminJwt;
    private String employeeJwt;

    @BeforeEach
    void setupDataset() throws Exception {
        inAppAlertRepository.deleteAll();
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();

        Role rAdmin = getOrCreateRole("ROLE_ADMIN");
        Role rEmp = getOrCreateRole("ROLE_EMPLOYEE");

        adminUser = getOrCreateUser("p8_perf_admin", "Password123!", rAdmin, "Operations");
        employeeUser = getOrCreateUser("p8_perf_emp", "Password123!", rEmp, "Engineering");

        ServiceCatalogItem itItem = serviceCatalogRepository.findByItemCode("P8-PERF-IT").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-PERF-IT", "Benchmark Laptop", "Hardware", "IT",
                        "Laptop provisioning", new BigDecimal("1800.00"), 16, true, false, true)));
        ServiceCatalogItem hrItem = serviceCatalogRepository.findByItemCode("P8-PERF-HR").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-PERF-HR", "Benchmark Payroll Query", "Payroll", "HR",
                        "Payroll inquiry", new BigDecimal("0.00"), 8, false, false, true)));
        ServiceCatalogItem facItem = serviceCatalogRepository.findByItemCode("P8-PERF-FAC").orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        "P8-PERF-FAC", "Benchmark Badge Request", "Security", "Facilities",
                        "Access card", new BigDecimal("25.00"), 12, false, false, true)));

        LocalDateTime now = LocalDateTime.now(clock);
        ServiceCatalogItem[] items = {itItem, hrItem, facItem};
        String[] depts = {"IT", "HR", "Facilities"};
        TicketPriority[] priorities = TicketPriority.values();
        TicketStatus[] statuses = {
                TicketStatus.SUBMITTED, TicketStatus.PENDING_APPROVAL, TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD, TicketStatus.RESOLVED, TicketStatus.CLOSED
        };

        List<ServiceTicket> tickets = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            int idx = i % 3;
            TicketStatus st = statuses[i % statuses.length];
            LocalDateTime created = now.minusDays(i % 28).minusHours(i % 8);
            ServiceTicket t = new ServiceTicket();
            t.setTicketNumber(String.format("P8-PERF-%04d", i + 1));
            t.setCatalogItem(items[idx]);
            t.setRequester(employeeUser);
            t.setDepartment(depts[idx]);
            t.setPriority(priorities[i % priorities.length]);
            t.setStatus(st);
            t.setDescription("Performance benchmark workload ticket #" + (i + 1));
            t.setCreatedAt(created);
            t.setSlaTargetDate(created.plusHours(16));
            if (st == TicketStatus.RESOLVED || st == TicketStatus.CLOSED) {
                t.setResolvedAt(created.plusHours((i % 2 == 0) ? 10 : 20));
            }
            t.setTotalPausedMinutes((i % 4 == 0) ? 30 : 0);
            tickets.add(t);
        }
        tickets = serviceTicketRepository.saveAllAndFlush(tickets);

        List<InAppAlert> alerts = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            InAppAlert a = new InAppAlert(
                    employeeUser,
                    tickets.get(i % tickets.size()),
                    (i % 2 == 0) ? "STATUS_CHANGED" : "TICKET_SUBMITTED",
                    "Benchmark Alert #" + (i + 1),
                    "Automated benchmark notification message for ticket #" + ((i % tickets.size()) + 1)
            );
            a.setIsRead(i >= 40); // 40 unread, 110 read
            a.setCreatedAt(now.minusMinutes(i * 5L));
            alerts.add(a);
        }
        inAppAlertRepository.saveAllAndFlush(alerts);

        List<ApprovalTask> tasks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            ApprovalTask task = new ApprovalTask(tickets.get(i), adminUser, 1);
            task.setCreatedAt(now.minusHours(i * 2L));
            if (i >= 15) {
                task.decide(ApprovalStatus.APPROVED, "Benchmark approval", now.minusHours(i));
            }
            tasks.add(task);
        }
        approvalTaskRepository.saveAllAndFlush(tasks);

        adminJwt = login("p8_perf_admin", "Password123!");
        employeeJwt = login("p8_perf_emp", "Password123!");
    }

    @Test
    @Order(1)
    void test1_HealthEndpointReportsUpAndDatabaseConnectivity() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @Order(2)
    void test2_CorrelationIdHeaderPropagatedAndSanitized() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "epams-trace-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, "epams-trace-001"));

        // Sanitization strips malicious characters/CRLF
        MvcResult sanitized = mockMvc.perform(get("/api/v1/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "bad<script>id"))
                .andExpect(status().isOk())
                .andReturn();
        String returnedId = sanitized.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(returnedId);
        assertFalse(returnedId.contains("<"), "Sanitized correlation ID must strip HTML/special characters");
    }

    @Test
    @Order(3)
    void test3_EndpointLatencyBenchmarkSuite() throws Exception {
        int warmup = 3;
        int iterations = 25;

        Map<String, double[]> results = new LinkedHashMap<>();
        results.put("GET /api/v1/notifications/my?page=0&size=20",
                measureEndpoint("/api/v1/notifications/my?page=0&size=20", employeeJwt, warmup, iterations));
        results.put("GET /api/v1/notifications/my/unread-count",
                measureEndpoint("/api/v1/notifications/my/unread-count", employeeJwt, warmup, iterations));
        results.put("GET /api/v1/analytics/overview",
                measureEndpoint("/api/v1/analytics/overview", adminJwt, warmup, iterations));
        results.put("GET /api/v1/analytics/dashboard",
                measureEndpoint("/api/v1/analytics/dashboard", adminJwt, warmup, iterations));
        results.put("GET /api/v1/departments/IT/queue?page=0&size=10",
                measureEndpoint("/api/v1/departments/IT/queue?page=0&size=10", adminJwt, warmup, iterations));
        results.put("GET /api/v1/catalog?page=0&size=10",
                measureEndpoint("/api/v1/catalog?page=0&size=10", employeeJwt, warmup, iterations));

        System.out.println("=== P8 BENCHMARK RESULTS (Dataset: 120 tickets, 150 alerts, 30 approvals; Iterations=" + iterations + ") ===");
        for (Map.Entry<String, double[]> entry : results.entrySet()) {
            double[] m = entry.getValue();
            System.out.printf("BENCHMARK | %-46s | avg=%.2fms | p50=%.2fms | p95=%.2fms | throughput=%.1f req/s%n",
                    entry.getKey(), m[0], m[1], m[2], m[3]);
            assertTrue(m[0] >= 0.0, "Latency metric must be non-negative");
        }
    }

    @Test
    @Order(4)
    void test4_ConcurrentLoadTestZeroErrorRate() throws Exception {
        int threads = 8;
        int requestsPerThread = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger(0);
        List<Long> latenciesNs = Collections.synchronizedList(new ArrayList<>());

        long suiteStart = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int workerId = t;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        String path;
                        String token;
                        switch ((workerId + i) % 4) {
                            case 0 -> { path = "/api/v1/notifications/my/unread-count"; token = employeeJwt; }
                            case 1 -> { path = "/api/v1/notifications/my?page=0&size=10"; token = employeeJwt; }
                            case 2 -> { path = "/api/v1/analytics/overview"; token = adminJwt; }
                            default -> { path = "/api/v1/departments/IT/queue?page=0&size=10"; token = adminJwt; }
                        }
                        long t0 = System.nanoTime();
                        MvcResult res = mockMvc.perform(get(path).cookie(new Cookie("accessToken", token))).andReturn();
                        latenciesNs.add(System.nanoTime() - t0);
                        if (res.getResponse().getStatus() != 200) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        long totalElapsedMs = Math.max(1L, (System.nanoTime() - suiteStart) / 1_000_000L);
        int totalRequests = threads * requestsPerThread;
        List<Double> sortedMs = latenciesNs.stream().map(ns -> ns / 1_000_000.0).sorted().toList();
        double avgMs = sortedMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double p50Ms = sortedMs.get(sortedMs.size() / 2);
        double p95Ms = sortedMs.get((int) Math.floor(sortedMs.size() * 0.95));
        double throughput = (totalRequests * 1000.0) / totalElapsedMs;

        System.out.printf("CONCURRENT LOAD | threads=%d | totalRequests=%d | elapsed=%dms | errors=%d | avg=%.2fms | p50=%.2fms | p95=%.2fms | throughput=%.1f req/s%n",
                threads, totalRequests, totalElapsedMs, failures.get(), avgMs, p50Ms, p95Ms, throughput);

        assertEquals(0, failures.get(), "Concurrent load test must produce 0 HTTP errors");
        assertEquals(totalRequests, latenciesNs.size());
    }

    private double[] measureEndpoint(String url, String jwt, int warmup, int iterations) throws Exception {
        for (int i = 0; i < warmup; i++) {
            mockMvc.perform(get(url).cookie(new Cookie("accessToken", jwt))).andExpect(status().isOk());
        }
        double[] samplesMs = new double[iterations];
        long totalStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            mockMvc.perform(get(url).cookie(new Cookie("accessToken", jwt))).andExpect(status().isOk());
            samplesMs[i] = (System.nanoTime() - t0) / 1_000_000.0;
        }
        long totalMs = Math.max(1L, (System.nanoTime() - totalStart) / 1_000_000L);
        Arrays.sort(samplesMs);
        double avg = Arrays.stream(samplesMs).average().orElse(0.0);
        double p50 = samplesMs[iterations / 2];
        double p95 = samplesMs[Math.min(iterations - 1, (int) Math.floor(iterations * 0.95))];
        double throughput = (iterations * 1000.0) / totalMs;
        return new double[]{avg, p50, p95, throughput};
    }

    private Role getOrCreateRole(String name) {
        return roleRepo.findByRoleName(name).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(name);
            return roleRepo.saveAndFlush(r);
        });
    }

    private UserAccount getOrCreateUser(String username, String password, Role role, String dept) {
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
        if (profileRepo.findByUserId(user.getId()).isEmpty()) {
            Profile p = new Profile();
            p.setUser(user);
            p.setEmployeeId("EMP-" + user.getId());
            p.setFirstName("Perf");
            p.setLastName(username);
            p.setDepartment(dept);
            p.setDesignation("Engineer");
            p.setDateOfJoining(LocalDate.of(2024, 1, 1));
            profileRepo.saveAndFlush(p);
        }
        return user;
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
