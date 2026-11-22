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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P9 Scaled Performance Regression Suite (4x P8 synthetic dataset:
 * 480 service tickets across 4 departments, 600 notifications, 100 approvals,
 * and 12-thread concurrent mixed load testing).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class P9ScalePerformanceTests {

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

    private String adminJwt;
    private String employeeJwt;

    @BeforeEach
    void seed4xDataset() throws Exception {
        inAppAlertRepository.deleteAll();
        approvalTaskRepository.deleteAll();
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();

        Role rAdmin = getOrCreateRole("ROLE_ADMIN");
        Role rEmp = getOrCreateRole("ROLE_EMPLOYEE");

        UserAccount adminUser = getOrCreateUser("p9_scale_admin", "Password123!", rAdmin, "Operations");
        UserAccount empUser = getOrCreateUser("p9_scale_emp", "Password123!", rEmp, "Engineering");

        ServiceCatalogItem[] items = new ServiceCatalogItem[] {
                getOrCreateItem("P9-SCL-IT", "Scale Laptop", "Hardware", "IT"),
                getOrCreateItem("P9-SCL-HR", "Scale Payroll", "Payroll", "HR"),
                getOrCreateItem("P9-SCL-FAC", "Scale Access Card", "Security", "Facilities"),
                getOrCreateItem("P9-SCL-FIN", "Scale Expense Audit", "Finance", "Finance")
        };
        String[] depts = {"IT", "HR", "Facilities", "Finance"};
        TicketPriority[] priorities = TicketPriority.values();
        TicketStatus[] statuses = {
                TicketStatus.SUBMITTED, TicketStatus.PENDING_APPROVAL, TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD, TicketStatus.RESOLVED, TicketStatus.CLOSED
        };

        LocalDateTime now = LocalDateTime.now(clock);
        List<ServiceTicket> tickets = new ArrayList<>(480);
        for (int i = 0; i < 480; i++) {
            int idx = i % 4;
            TicketStatus st = statuses[i % statuses.length];
            LocalDateTime created = now.minusDays(i % 60).minusHours(i % 9);
            ServiceTicket t = new ServiceTicket();
            t.setTicketNumber(String.format("P9-SCL-%05d", i + 1));
            t.setCatalogItem(items[idx]);
            t.setRequester(empUser);
            t.setDepartment(depts[idx]);
            t.setPriority(priorities[i % priorities.length]);
            t.setStatus(st);
            t.setDescription("P9 scaled dataset ticket #" + (i + 1));
            t.setCreatedAt(created);
            t.setSlaTargetDate(created.plusHours(16));
            if (st == TicketStatus.RESOLVED || st == TicketStatus.CLOSED) {
                t.setResolvedAt(created.plusHours((i % 3 == 0) ? 20 : 10));
            }
            t.setTotalPausedMinutes((i % 5 == 0) ? 45 : 0);
            tickets.add(t);
        }
        tickets = serviceTicketRepository.saveAllAndFlush(tickets);

        List<InAppAlert> alerts = new ArrayList<>(600);
        for (int i = 0; i < 600; i++) {
            InAppAlert a = new InAppAlert(
                    empUser,
                    tickets.get(i % tickets.size()),
                    (i % 3 == 0) ? "TICKET_RESOLVED" : "STATUS_CHANGED",
                    "Scaled Alert #" + (i + 1),
                    "Scaled workload alert message #" + (i + 1)
            );
            a.setIsRead(i >= 120); // 120 unread, 480 read
            a.setCreatedAt(now.minusMinutes(i * 3L));
            alerts.add(a);
        }
        inAppAlertRepository.saveAllAndFlush(alerts);

        List<ApprovalTask> tasks = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            ApprovalTask task = new ApprovalTask(tickets.get(i), adminUser, 1);
            task.setCreatedAt(now.minusHours(i));
            if (i >= 40) {
                task.decide(ApprovalStatus.APPROVED, "Scaled approval", now.minusMinutes(i * 15L));
            }
            tasks.add(task);
        }
        approvalTaskRepository.saveAllAndFlush(tasks);

        adminJwt = login("p9_scale_admin", "Password123!");
        employeeJwt = login("p9_scale_emp", "Password123!");
    }

    @Test
    @Order(1)
    @DisplayName("P9 Scale Benchmark (480 tickets, 600 alerts, 100 approvals) + 12-thread concurrent load")
    void test1_ScaledDatasetBenchmarkAndConcurrentLoad() throws Exception {
        int warmup = 3;
        int iterations = 20;

        System.out.println("=== P9 SCALED BENCHMARK (Dataset: 480 tickets, 600 alerts, 100 approvals; Iterations=" + iterations + ") ===");
        logBenchmark("/api/v1/notifications/my?page=0&size=20", employeeJwt, warmup, iterations);
        logBenchmark("/api/v1/notifications/my/unread-count", employeeJwt, warmup, iterations);
        logBenchmark("/api/v1/analytics/overview", adminJwt, warmup, iterations);
        logBenchmark("/api/v1/analytics/dashboard", adminJwt, warmup, iterations);
        logBenchmark("/api/v1/departments/IT/queue?page=0&size=20", adminJwt, warmup, iterations);

        // 12-thread concurrent mixed load (120 requests total)
        int threads = 12;
        int reqsPerThread = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long suiteStart = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            futures.add(pool.submit(() -> {
                try {
                    gate.await();
                    for (int i = 0; i < reqsPerThread; i++) {
                        String path;
                        String jwt;
                        switch ((id + i) % 4) {
                            case 0 -> { path = "/api/v1/notifications/my/unread-count"; jwt = employeeJwt; }
                            case 1 -> { path = "/api/v1/notifications/my?page=0&size=20"; jwt = employeeJwt; }
                            case 2 -> { path = "/api/v1/analytics/dashboard"; jwt = adminJwt; }
                            default -> { path = "/api/v1/departments/IT/queue?page=0&size=20"; jwt = adminJwt; }
                        }
                        long t0 = System.nanoTime();
                        MvcResult res = mockMvc.perform(get(path).cookie(new Cookie("accessToken", jwt))).andReturn();
                        latencies.add(System.nanoTime() - t0);
                        if (res.getResponse().getStatus() != 200) {
                            errors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }
        gate.countDown();
        for (Future<?> f : futures) {
            f.get(45, TimeUnit.SECONDS);
        }
        pool.shutdown();

        long elapsedMs = Math.max(1L, (System.nanoTime() - suiteStart) / 1_000_000L);
        int total = threads * reqsPerThread;
        List<Double> sorted = latencies.stream().map(ns -> ns / 1_000_000.0).sorted().toList();
        double avg = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double p50 = sorted.get(sorted.size() / 2);
        double p95 = sorted.get((int) Math.floor(sorted.size() * 0.95));
        double throughput = (total * 1000.0) / elapsedMs;

        Runtime rt = Runtime.getRuntime();
        long usedHeapMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        System.out.printf("P9_CONCURRENT_LOAD | threads=%d | total=%d | elapsed=%dms | errors=%d | avg=%.2fms | p50=%.2fms | p95=%.2fms | throughput=%.1f req/s | heapUsed=%dMB%n",
                threads, total, elapsedMs, errors.get(), avg, p50, p95, throughput, usedHeapMb);

        assertEquals(0, errors.get(), "12-thread concurrent scaled load must produce 0 HTTP errors");
        assertEquals(total, latencies.size());
    }

    private void logBenchmark(String path, String jwt, int warmup, int iterations) throws Exception {
        for (int i = 0; i < warmup; i++) {
            mockMvc.perform(get(path).cookie(new Cookie("accessToken", jwt))).andExpect(status().isOk());
        }
        double[] samples = new double[iterations];
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            mockMvc.perform(get(path).cookie(new Cookie("accessToken", jwt))).andExpect(status().isOk());
            samples[i] = (System.nanoTime() - t0) / 1_000_000.0;
        }
        long elapsedMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
        Arrays.sort(samples);
        double avg = Arrays.stream(samples).average().orElse(0.0);
        double p50 = samples[iterations / 2];
        double p95 = samples[Math.min(iterations - 1, (int) Math.floor(iterations * 0.95))];
        double tps = (iterations * 1000.0) / elapsedMs;
        System.out.printf("P9_SCALE_BENCH | %-46s | avg=%.2fms | p50=%.2fms | p95=%.2fms | throughput=%.1f req/s%n",
                path, avg, p50, p95, tps);
    }

    private ServiceCatalogItem getOrCreateItem(String code, String title, String cat, String dept) {
        return serviceCatalogRepository.findByItemCode(code).orElseGet(() ->
                serviceCatalogRepository.saveAndFlush(new ServiceCatalogItem(
                        code, title, cat, dept, "Scaled item " + code, new BigDecimal("500.00"), 16, false, false, true)));
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
            p.setEmployeeId("EMP-SCL-" + user.getId());
            p.setFirstName("Scale");
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

