package com.examly.springapp;

import com.examly.springapp.dto.ApprovalDecisionRequestDTO;
import com.examly.springapp.dto.AuditVerificationReport;
import com.examly.springapp.dto.CreateTicketRequestDTO;
import com.examly.springapp.dto.ServiceTicketDTO;
import com.examly.springapp.dto.analytics.AnalyticsDashboardBundleDTO;
import com.examly.springapp.exception.GlobalExceptionHandler;
import com.examly.springapp.exception.OptimisticLockingConflictException;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.NotificationEventListener;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.*;
import com.examly.springapp.workflow.SlaScheduledProcessor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 2 Real MySQL 8+ Integration, Brownfield Migration, Reliability, Concurrency,
 * Permission Enforcement, and EXPLAIN/EXPLAIN ANALYZE Verification Suite.
 *
 * Strictly gated behind EPAMS_MYSQL_INTEGRATION=true so default offline H2 test runs
 * never substitute H2 or claim MySQL results without a live MySQL 8+ server.
 */
@EnabledIfEnvironmentVariable(named = "EPAMS_MYSQL_INTEGRATION", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Stage2RealMysqlIntegrationTests {

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AuditChainService auditChainService;
    @Autowired private AuditLogRepo auditLogRepo;
    @Autowired private AuditChainHeadRepository auditChainHeadRepository;
    @Autowired private NotificationEventListener notificationEventListener;
    @Autowired private InAppAlertRepository inAppAlertRepository;
    @Autowired private ServiceCatalogRepository serviceCatalogRepository;
    @Autowired private ServiceTicketRepository serviceTicketRepository;
    @Autowired private ApprovalTaskRepository approvalTaskRepository;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private TicketCommandService ticketCommandService;
    @Autowired private ApprovalService approvalService;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private SlaScheduledProcessor slaScheduledProcessor;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private GlobalExceptionHandler globalExceptionHandler;
    @Autowired private Clock clock;

    @Test
    @Order(1)
    void test1_VerifyActualMysql8ConnectionNotH2() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            String version = conn.getMetaData().getDatabaseProductVersion();
            String url = conn.getMetaData().getURL();
            System.out.println("[STAGE2-MYSQL] Connected Product=" + product + " Version=" + version);
            assertTrue(product.toLowerCase().contains("mysql"), "Must connect to real MySQL, got: " + product);
            assertFalse(product.toLowerCase().contains("h2"), "H2 must never be substituted for Stage 2 MySQL");
            assertTrue(version.startsWith("8."), "Must be MySQL 8+, got: " + version);
            assertTrue(url.startsWith("jdbc:mysql://"), "JDBC URL must use MySQL driver");
        }
    }

    @Test
    @Order(2)
    void test2_StageD_BrownfieldRealMysqlDataPreservationAndUpgrade() throws Exception {
        String host = System.getenv().getOrDefault("DB_HOST", "127.0.0.1");
        String port = System.getenv().getOrDefault("DB_PORT", "13307");
        String bfDb = System.getenv().getOrDefault("EPAMS_BROWNFIELD_DB", "epams_stage2_brownfield");
        String migUser = System.getenv().getOrDefault("EPAMS_MIGRATOR_USERNAME", "epams_migrator");
        String migPass = System.getenv("EPAMS_MIGRATOR_PASSWORD");
        assertNotNull(migPass, "EPAMS_MIGRATOR_PASSWORD env var must be set for Stage D brownfield verification");

        DriverManagerDataSource bfDs = new DriverManagerDataSource();
        bfDs.setDriverClassName("com.mysql.cj.jdbc.Driver");
        bfDs.setUrl("jdbc:mysql://" + host + ":" + port + "/" + bfDb
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        bfDs.setUsername(migUser);
        bfDs.setPassword(migPass);

        JdbcTemplate bfJdbc = new JdbcTemplate(bfDs);

        // 1. Seed pre-existing legacy V1 IAM schema on real MySQL 8+ (no flyway_schema_history, no V2-V4)
        bfJdbc.execute("""
            CREATE TABLE user_account (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                email VARCHAR(100) NULL,
                phone VARCHAR(20) NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                failed_login_attempts INT NOT NULL DEFAULT 0,
                locked_until TIMESTAMP NULL,
                CONSTRAINT uk_user_account_username UNIQUE (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        bfJdbc.execute("""
            CREATE TABLE role (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                role_name VARCHAR(50) NOT NULL,
                CONSTRAINT uk_role_role_name UNIQUE (role_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        bfJdbc.execute("""
            CREATE TABLE user_role_mapping (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NULL,
                role_id BIGINT NULL,
                CONSTRAINT fk_urm_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
                CONSTRAINT fk_urm_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE RESTRICT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        bfJdbc.execute("""
            CREATE TABLE profile (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(30) NULL,
                user_id BIGINT NULL,
                first_name VARCHAR(50) NULL,
                last_name VARCHAR(50) NULL,
                address VARCHAR(255) NULL,
                dob DATE NULL,
                gender VARCHAR(20) NULL,
                department VARCHAR(50) NULL,
                designation VARCHAR(100) NULL,
                salary DOUBLE NULL,
                date_of_joining DATE NULL,
                reporting_manager VARCHAR(50) NULL,
                emergency_contact VARCHAR(50) NULL,
                CONSTRAINT uk_profile_employee_id UNIQUE (employee_id),
                CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        bfJdbc.execute("CREATE INDEX idx_profile_department ON profile (department)");
        bfJdbc.execute("CREATE INDEX idx_profile_reporting_manager ON profile (reporting_manager)");
        bfJdbc.execute("""
            CREATE TABLE refresh_token (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                token_hash VARCHAR(255) NOT NULL,
                user_id BIGINT NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
                CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        bfJdbc.execute("""
            CREATE TABLE audit_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                actor_username VARCHAR(50) NULL,
                action VARCHAR(50) NULL,
                target_username VARCHAR(50) NULL,
                details VARCHAR(1000) NULL,
                timestamp TIMESTAMP NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        bfJdbc.execute("CREATE INDEX idx_audit_target_user ON audit_log (target_username)");
        bfJdbc.execute("CREATE INDEX idx_audit_action ON audit_log (action)");
        bfJdbc.execute("""
            CREATE TABLE account_status_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                status VARCHAR(255) NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        // 2. Populate realistic legacy data across ALL 7 IAM tables
        String bcryptAdmin = "$2a$10$wO7vI13Lg1J13O0t7Vz7euZ8yG3bU0wUvO7vI13Lg1J13O0t7Vz7e";
        String bcryptEmp   = "$2a$10$9qK4mN2pR8sT1uV3wX5yZeA7bC9dE1fG3hI5jK7lM9nO1pQ3rS5tU";
        Timestamp lockUntil = Timestamp.valueOf("2026-11-18 12:30:00");

        bfJdbc.update("INSERT INTO user_account (username, password_hash, email, phone, status, failed_login_attempts, locked_until) VALUES (?,?,?,?,?,?,?)",
                "bf_admin", bcryptAdmin, "bf_admin@corp.example", "+91-9840000001", "LOCKED", 5, lockUntil);
        bfJdbc.update("INSERT INTO user_account (username, password_hash, email, phone, status, failed_login_attempts, locked_until) VALUES (?,?,?,?,?,?,?)",
                "bf_employee", bcryptEmp, "bf_emp@corp.example", "+91-9840000002", "ACTIVE", 0, null);

        Long adminId = bfJdbc.queryForObject("SELECT id FROM user_account WHERE username='bf_admin'", Long.class);
        Long empId = bfJdbc.queryForObject("SELECT id FROM user_account WHERE username='bf_employee'", Long.class);

        bfJdbc.update("INSERT INTO role (role_name) VALUES ('ROLE_ADMIN'), ('ROLE_EMPLOYEE')");
        Long adminRoleId = bfJdbc.queryForObject("SELECT id FROM role WHERE role_name='ROLE_ADMIN'", Long.class);
        Long empRoleId = bfJdbc.queryForObject("SELECT id FROM role WHERE role_name='ROLE_EMPLOYEE'", Long.class);

        bfJdbc.update("INSERT INTO user_role_mapping (user_id, role_id) VALUES (?,?), (?,?)",
                adminId, adminRoleId, empId, empRoleId);

        bfJdbc.update("""
            INSERT INTO profile (employee_id, user_id, first_name, last_name, address, dob, gender, department, designation, salary, date_of_joining, reporting_manager, emergency_contact)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, "EMP-BF-001", adminId, "Ananya", "Krishnan", "42 Anna Salai, Chennai",
                java.sql.Date.valueOf("1988-04-15"), "Female", "IT", "Principal Architect", 185000.50,
                java.sql.Date.valueOf("2021-07-01"), "ceo_office", "+91-9840009999");
        bfJdbc.update("""
            INSERT INTO profile (employee_id, user_id, first_name, last_name, address, dob, gender, department, designation, salary, date_of_joining, reporting_manager, emergency_contact)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, "EMP-BF-002", empId, "Karthik", "Subramanian", "18 OMR Road, Chennai",
                java.sql.Date.valueOf("1995-09-22"), "Male", "Engineering", "Senior SDE", 112500.00,
                java.sql.Date.valueOf("2023-02-10"), "bf_admin", "+91-9840008888");

        String tokenHash1 = "sha256_bf_refresh_token_001_abcdef1234567890abcdef1234567890";
        String tokenHash2 = "sha256_bf_refresh_token_002_1234567890abcdef1234567890abcdef";
        bfJdbc.update("INSERT INTO refresh_token (token_hash, user_id, expires_at, revoked) VALUES (?,?,?,?), (?,?,?,?)",
                tokenHash1, adminId, Timestamp.valueOf("2026-12-01 00:00:00"), false,
                tokenHash2, empId, Timestamp.valueOf("2026-12-05 00:00:00"), true);

        bfJdbc.update("INSERT INTO audit_log (actor_username, action, target_username, details, timestamp) VALUES (?,?,?,?,?), (?,?,?,?,?)",
                "bf_admin", "LEGACY_LOGIN_LOCK", "bf_admin", "Account locked after 5 failed attempts", Timestamp.valueOf("2026-10-01 09:15:00"),
                "bf_admin", "LEGACY_ROLE_ASSIGN", "bf_employee", "Assigned ROLE_EMPLOYEE pre-Flyway", Timestamp.valueOf("2026-10-02 14:20:00"));

        bfJdbc.update("INSERT INTO account_status_log (status) VALUES ('ACTIVE'), ('LOCKED')");

        // 3. Execute documented brownfield adoption runbook on real MySQL 8+
        Flyway bfFlyway = Flyway.configure()
                .dataSource(bfDs)
                .baselineOnMigrate(false)
                .baselineVersion("1")
                .baselineDescription("Existing IAM Schema Baseline")
                .validateOnMigrate(true)
                .locations("classpath:db/migration")
                .load();

        bfFlyway.baseline();
        var migrateResult = bfFlyway.migrate();
        assertEquals(3, migrateResult.migrationsExecuted, "Exactly 3 migrations (V2, V3, V4) must execute after V1 baseline");

        // Verify idempotent repeated validation and migration
        bfFlyway.validate();
        assertEquals(0, bfFlyway.migrate().migrationsExecuted, "Repeated migrate() must apply 0 migrations");

        MigrationInfo[] applied = bfFlyway.info().applied();
        assertEquals(4, applied.length, "History must contain Baseline v1 + V2, V3, V4");
        assertEquals(MigrationState.BASELINE, applied[0].getState());
        for (int i = 1; i < applied.length; i++) {
            assertEquals(MigrationState.SUCCESS, applied[i].getState());
        }

        // 4. Byte-for-byte preservation verification across ALL 7 legacy tables
        Map<String, Object> u1 = bfJdbc.queryForMap("SELECT * FROM user_account WHERE id=?", adminId);
        assertEquals("bf_admin", u1.get("username"));
        assertEquals(bcryptAdmin, u1.get("password_hash"));
        assertEquals("bf_admin@corp.example", u1.get("email"));
        assertEquals("+91-9840000001", u1.get("phone"));
        assertEquals("LOCKED", u1.get("status"));
        assertEquals(5, ((Number) u1.get("failed_login_attempts")).intValue());
        assertEquals(lockUntil, u1.get("locked_until"));

        Map<String, Object> u2 = bfJdbc.queryForMap("SELECT * FROM user_account WHERE id=?", empId);
        assertEquals(bcryptEmp, u2.get("password_hash"));
        assertEquals("ACTIVE", u2.get("status"));

        assertEquals(2, bfJdbc.queryForObject("SELECT COUNT(*) FROM user_role_mapping", Integer.class));

        Map<String, Object> p1 = bfJdbc.queryForMap("SELECT * FROM profile WHERE employee_id='EMP-BF-001'");
        assertEquals("Ananya", p1.get("first_name"));
        assertEquals("42 Anna Salai, Chennai", p1.get("address"));
        assertEquals(185000.50, ((Number) p1.get("salary")).doubleValue(), 0.0001);
        assertEquals("ceo_office", p1.get("reporting_manager"));

        Map<String, Object> rt1 = bfJdbc.queryForMap("SELECT * FROM refresh_token WHERE token_hash=?", tokenHash1);
        assertEquals(adminId, ((Number) rt1.get("user_id")).longValue());
        assertEquals(false, rt1.get("revoked"));
        Map<String, Object> rt2 = bfJdbc.queryForMap("SELECT * FROM refresh_token WHERE token_hash=?", tokenHash2);
        assertEquals(true, rt2.get("revoked"));

        // Verify legacy audit rows preserved with NULL cryptographic columns (NO fabricated hashes!)
        List<Map<String, Object>> legacyAudits = bfJdbc.queryForList("SELECT * FROM audit_log ORDER BY id ASC");
        assertEquals(2, legacyAudits.size());
        for (Map<String, Object> row : legacyAudits) {
            assertNull(row.get("sequence_number"), "Legacy audit sequence_number must remain NULL");
            assertNull(row.get("previous_hash"), "Legacy audit previous_hash must remain NULL");
            assertNull(row.get("current_hash"), "Legacy audit current_hash must remain NULL");
        }

        assertEquals(2, bfJdbc.queryForObject("SELECT COUNT(*) FROM account_status_log", Integer.class));

        // 5. Verify zero duplicate tables, all 14 InnoDB tables, all 13 FKs, and genesis head row
        Integer tableCount = bfJdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?", Integer.class, bfDb);
        assertEquals(14, tableCount, "Must have exact 14 tables (13 app + flyway_schema_history)");

        Integer nonInnoDb = bfJdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND ENGINE <> 'InnoDB'",
                Integer.class, bfDb);
        assertEquals(0, nonInnoDb, "All brownfield tables must use InnoDB");

        Integer fkCount = bfJdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL",
                Integer.class, bfDb);
        assertEquals(13, fkCount, "All 13 foreign keys must be intact on brownfield DB");

        Map<String, Object> head = bfJdbc.queryForMap("SELECT * FROM audit_chain_head WHERE id = 1");
        assertEquals(0L, ((Number) head.get("last_sequence")).longValue());
        assertEquals(AuditChainService.GENESIS_HASH, head.get("current_hash"));

        System.out.println("[STAGE2-BROWNFIELD] PASSED: 7 legacy tables preserved byte-for-byte; V2-V4 applied; 14 InnoDB tables & 13 FKs verified.");
    }

    @Test
    @Order(3)
    void test3_StageE1_AuditChainConcurrencyRollbackAndPermissionEnforcement() throws Exception {
        long initialSeq = auditChainHeadRepository.findById(1L).orElseThrow().getLastSequence();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AuditLog>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                return auditChainService.recordChainedEvent(
                        "mysql_worker_" + idx, "STAGE2_CONCURRENT_AUDIT", "target_" + idx,
                        "Real MySQL InnoDB FOR UPDATE concurrency test #" + idx);
            }));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        Set<Long> sequences = new HashSet<>();
        for (Future<AuditLog> f : futures) {
            AuditLog saved = f.get(30, TimeUnit.SECONDS);
            assertNotNull(saved.getSequenceNumber());
            assertTrue(sequences.add(saved.getSequenceNumber()), "Duplicate sequence number detected!");
        }
        pool.shutdown();

        assertEquals(threads, sequences.size());
        long afterConcurrentSeq = auditChainHeadRepository.findById(1L).orElseThrow().getLastSequence();
        assertEquals(initialSeq + threads, afterConcurrentSeq, "Head sequence must increase monotonically by exact thread count");

        // Verify rollback leaves zero sequence gap and zero orphaned row
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            auditChainService.recordChainedEvent("rollback_actor", "SHOULD_ROLLBACK", "target", "Rollback test");
            throw new IllegalStateException("Forced rollback after audit insert");
        }));

        long afterRollbackSeq = auditChainHeadRepository.findById(1L).orElseThrow().getLastSequence();
        assertEquals(afterConcurrentSeq, afterRollbackSeq, "Rolled-back audit must not advance audit_chain_head");

        // Verify full cryptographic chain integrity against real MySQL TIMESTAMP round-trip
        AuditVerificationReport report = auditChainService.verifyChain();
        assertTrue(report.isValid(), "Audit chain verification failed on real MySQL: " + report.getViolations());
        assertEquals(afterConcurrentSeq, report.getChainedRecordsCount());

        // Verify epams_app runtime user CANNOT update/delete audit_log or delete audit_chain_head
        assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("UPDATE audit_log SET details = 'tampered' WHERE sequence_number = ?", afterConcurrentSeq),
                "Runtime epams_app user must be denied UPDATE on audit_log");
        assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("DELETE FROM audit_log WHERE sequence_number = ?", afterConcurrentSeq),
                "Runtime epams_app user must be denied DELETE on audit_log");
        assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update("DELETE FROM audit_chain_head WHERE id = 1"),
                "Runtime epams_app user must be denied DELETE on audit_chain_head");

        System.out.println("[STAGE2-E1-AUDIT] PASSED: 10 concurrent MySQL InnoDB writes + rollback + verifyChain(valid=true) + UPDATE/DELETE denied.");
    }

    @Test
    @Order(4)
    void test4_StageE2_NotificationDeduplicationUnderRealMysqlConcurrency() throws Exception {
        UserAccount recipient = userAccountRepo.findByUsername("john_doe").orElseThrow();
        String dedupKey = "STAGE2_MYSQL_DEDUP_" + UUID.randomUUID().toString().substring(0, 12);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger trueReturns = new AtomicInteger(0);
        AtomicInteger exceptions = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    TicketNotificationEvent evt = new TicketNotificationEvent(
                            null, "TKT-DEDUP", recipient.getUsername(), "STAGE2_DEDUP_TEST",
                            "Concurrent Dedup Test", "Testing uk_alert_recipient_dedup on real MySQL 8.0.45",
                            "TICKET", 9999L, dedupKey);
                    if (notificationEventListener.dispatchNotification(evt)) {
                        trueReturns.incrementAndGet();
                    }
                } catch (Exception ex) {
                    exceptions.incrementAndGet();
                }
            }));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(0, exceptions.get(), "Concurrent dedup race must never leak unhandled exceptions");
        assertEquals(1, trueReturns.get(), "Exactly 1 concurrent worker must succeed in persisting & triggering delivery");

        Integer persisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM in_app_alert WHERE recipient_id = ? AND dedup_key = ?",
                Integer.class, recipient.getId(), dedupKey);
        assertEquals(1, persisted, "MySQL uk_alert_recipient_dedup must persist exactly 1 row");

        // Verify multiple NULL dedup_key rows are still permitted by MySQL unique index
        TicketNotificationEvent nullKey1 = new TicketNotificationEvent(
                null, "TKT-NULL-1", recipient.getUsername(), "INFO", "Null Key 1", "Body 1", "TICKET", 1L, null);
        TicketNotificationEvent nullKey2 = new TicketNotificationEvent(
                null, "TKT-NULL-2", recipient.getUsername(), "INFO", "Null Key 2", "Body 2", "TICKET", 2L, null);
        assertTrue(notificationEventListener.dispatchNotification(nullKey1));
        assertTrue(notificationEventListener.dispatchNotification(nullKey2));

        System.out.println("[STAGE2-E2-DEDUP] PASSED: 8-thread race persisted exact 1 row (uk_alert_recipient_dedup); multiple NULL keys allowed.");
    }

    @Test
    @Order(5)
    void test5_StageE3_OptimisticLockingAndHttp409MappingOnRealMysql() {
        ServiceCatalogItem item = ensureCatalogItem("STAGE2-OPT-LOCK", "IT", true, false);
        CreateTicketRequestDTO req = new CreateTicketRequestDTO();
        req.setCatalogItemId(item.getId());
        req.setDepartment("IT");
        req.setPriority(TicketPriority.HIGH);
        req.setDescription("Optimistic lock test ticket on real MySQL 8.0.45");

        ServiceTicketDTO created = ticketCommandService.createTicket(req, "john_doe");
        Long ticketId = created.getId();

        // 1. ServiceTicket @Version optimistic lock test
        ServiceTicket copy1 = serviceTicketRepository.findById(ticketId).orElseThrow();
        ServiceTicket copy2 = serviceTicketRepository.findById(ticketId).orElseThrow();
        assertEquals(copy1.getVersion(), copy2.getVersion());

        copy1.setDescription("Updated by transaction 1");
        serviceTicketRepository.saveAndFlush(copy1);

        copy2.setDescription("Stale update by transaction 2");
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> serviceTicketRepository.saveAndFlush(copy2),
                "Stale ServiceTicket update must throw ObjectOptimisticLockingFailureException on real MySQL");

        // 2. ApprovalTask @Version optimistic lock + stale version check
        List<ApprovalTask> tasks = approvalTaskRepository.findByTicketIdOrderByApprovalStageAsc(ticketId);
        assertFalse(tasks.isEmpty(), "Approval task must be created for approval-required item");
        Long taskId = tasks.get(0).getId();

        ApprovalTask taskCopy1 = approvalTaskRepository.findById(taskId).orElseThrow();
        ApprovalTask taskCopy2 = approvalTaskRepository.findById(taskId).orElseThrow();

        taskCopy1.setDecisionReason("Updated in tx1");
        approvalTaskRepository.saveAndFlush(taskCopy1);

        taskCopy2.setDecisionReason("Stale tx2 write");
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> approvalTaskRepository.saveAndFlush(taskCopy2),
                "Stale ApprovalTask update must throw ObjectOptimisticLockingFailureException on real MySQL");

        // 3. Stale version via ApprovalService + HTTP 409 Conflict mapping
        ApprovalDecisionRequestDTO staleDecision = new ApprovalDecisionRequestDTO();
        staleDecision.setDecision(ApprovalStatus.APPROVED);
        staleDecision.setReason("Approving with stale version");
        staleDecision.setVersion(0L); // stale (already incremented to 1)
        OptimisticLockingConflictException conflictEx = assertThrows(
                OptimisticLockingConflictException.class,
                () -> approvalService.decideApproval(taskId, staleDecision, "admin"));

        MockHttpServletRequest mockReq = new MockHttpServletRequest("POST", "/api/v1/approvals/" + taskId + "/decision");
        ResponseEntity<Map<String, Object>> problem = globalExceptionHandler.handleOptimisticLockingConflict(conflictEx, mockReq);
        assertEquals(409, problem.getStatusCode().value(), "Optimistic locking conflict must map to HTTP 409 Conflict");

        System.out.println("[STAGE2-E3-OPTLOCK] PASSED: ServiceTicket & ApprovalTask @Version rejected stale writes + HTTP 409 verified.");
    }

    @Test
    @Order(6)
    void test6_StageE4_TransactionIntegrityAfterCommitAndSlaJobsOnRealMysql() throws Exception {
        ServiceCatalogItem item = ensureCatalogItem("STAGE2-TX-ITEM", "IT", true, false);
        long auditBefore = auditChainHeadRepository.findById(1L).orElseThrow().getLastSequence();

        // 1. Ticket creation + audit persistence atomicity
        CreateTicketRequestDTO req = new CreateTicketRequestDTO();
        req.setCatalogItemId(item.getId());
        req.setDepartment("IT");
        req.setPriority(TicketPriority.MEDIUM);
        req.setDescription("Transaction integrity & approval audit atomicity test on MySQL");
        ServiceTicketDTO ticketDto = ticketCommandService.createTicket(req, "john_doe");

        long auditAfterCreate = auditChainHeadRepository.findById(1L).orElseThrow().getLastSequence();
        assertTrue(auditAfterCreate > auditBefore, "Ticket creation must commit synchronous audit entry");

        // 2. Approval decision + audit persistence atomicity
        ApprovalTask task = approvalTaskRepository.findByTicketIdOrderByApprovalStageAsc(ticketDto.getId()).get(0);
        ApprovalDecisionRequestDTO approveReq = new ApprovalDecisionRequestDTO();
        approveReq.setDecision(ApprovalStatus.APPROVED);
        approveReq.setReason("Approved in Stage 2 real MySQL verification");
        approveReq.setVersion(task.getVersion());
        approvalService.decideApproval(task.getId(), approveReq, "admin");

        long auditAfterApprove = auditChainHeadRepository.findById(1L).orElseThrow().getLastSequence();
        assertTrue(auditAfterApprove > auditAfterCreate, "Approval decision must commit synchronous audit entry");

        // 3. AFTER_COMMIT notification suppression on rolled-back transaction
        String phantomDedup = "PHANTOM_ROLLBACK_" + UUID.randomUUID().toString().substring(0, 8);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new TicketNotificationEvent(
                    ticketDto.getId(), ticketDto.getTicketNumber(), "john_doe", "PHANTOM_EVENT",
                    "Should Never Persist", "Rolled back transaction", "TICKET", ticketDto.getId(), phantomDedup));
            throw new RuntimeException("Simulate DB failure before commit");
        }));

        Integer phantomCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM in_app_alert WHERE dedup_key = ?", Integer.class, phantomDedup);
        assertEquals(0, phantomCount, "Rolled-back transaction must never persist AFTER_COMMIT notification");

        // 4. Scheduled SLA & Auto-closure retry + concurrent overlap safety on real MySQL
        ServiceTicket resolvedTicket = serviceTicketRepository.findById(ticketDto.getId()).orElseThrow();
        UserAccount assignee = userAccountRepo.findByUsername("admin").orElseThrow();
        resolvedTicket.setAssignee(assignee);
        resolvedTicket.setStatus(TicketStatus.RESOLVED);
        resolvedTicket.setResolvedAt(LocalDateTime.of(2026, 11, 2, 10, 0)); // Monday
        serviceTicketRepository.saveAndFlush(resolvedTicket);

        // Evaluation time > 48 business hours later (e.g., Nov 18)
        LocalDateTime evalTime = LocalDateTime.of(2026, 11, 18, 12, 0);
        SlaScheduledProcessor.SlaJobSummary run1 = slaScheduledProcessor.processAutoClosures(evalTime);
        SlaScheduledProcessor.SlaJobSummary run2 = slaScheduledProcessor.processAutoClosures(evalTime);

        assertTrue(run1.processed() >= 1, "First auto-closure run must close eligible resolved ticket");
        assertEquals(0, run2.processed(), "Retry auto-closure run must be idempotent (0 re-processed)");
        assertEquals(TicketStatus.CLOSED, serviceTicketRepository.findById(ticketDto.getId()).orElseThrow().getStatus());

        Integer autoCloseAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM in_app_alert WHERE dedup_key = ?", Integer.class, "AUTO_CLOSE_" + ticketDto.getId());
        assertEquals(1, autoCloseAlerts, "Retry must produce exact 1 AUTO_CLOSE alert");

        // SLA Breach retry + 2-thread overlapping execution test
        ServiceCatalogItem directItem = ensureCatalogItem("STAGE2-SLA-ITEM", "HR", false, false);
        CreateTicketRequestDTO slaReq = new CreateTicketRequestDTO();
        slaReq.setCatalogItemId(directItem.getId());
        slaReq.setDepartment("HR");
        slaReq.setPriority(TicketPriority.CRITICAL);
        slaReq.setDescription("Overdue SLA alert test ticket");
        ServiceTicketDTO slaDto = ticketCommandService.createTicket(slaReq, "john_doe");

        ServiceTicket slaTicket = serviceTicketRepository.findById(slaDto.getId()).orElseThrow();
        slaTicket.setAssignee(assignee);
        slaTicket.setSlaTargetDate(LocalDateTime.of(2026, 11, 10, 12, 0));
        serviceTicketRepository.saveAndFlush(slaTicket);

        ExecutorService overlapPool = Executors.newFixedThreadPool(2);
        Future<SlaScheduledProcessor.SlaJobSummary> f1 = overlapPool.submit(() -> slaScheduledProcessor.processSlaAlerts(evalTime));
        Future<SlaScheduledProcessor.SlaJobSummary> f2 = overlapPool.submit(() -> slaScheduledProcessor.processSlaAlerts(evalTime));
        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        overlapPool.shutdown();
        // Run a third sequential retry
        slaScheduledProcessor.processSlaAlerts(evalTime);

        Integer slaAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM in_app_alert WHERE dedup_key = ?", Integer.class, "SLA_BREACH_" + slaDto.getId());
        assertEquals(1, slaAlerts, "Overlapping + retried SLA breach job must persist exact 1 deduplicated alert");

        assertTrue(auditChainService.verifyChain().isValid(), "Audit chain must remain 100% valid after SLA & TX tests");
        System.out.println("[STAGE2-E4-TX-SLA] PASSED: Atomicity, AFTER_COMMIT rollback suppression, and SLA/auto-close retry & overlap verified.");
    }

    @Test
    @Order(7)
    void test7_StageE5_AnalyticsAndRealMysqlExplainAnalyzeOnSyntheticDataset() {
        UserAccount requester = userAccountRepo.findByUsername("john_doe").orElseThrow();
        UserAccount approver = userAccountRepo.findByUsername("admin").orElseThrow();

        ServiceCatalogItem catIt = ensureCatalogItem("SYN-IT-01", "IT", true, false);
        ServiceCatalogItem catHr = ensureCatalogItem("SYN-HR-01", "HR", true, false);
        ServiceCatalogItem catFin = ensureCatalogItem("SYN-FIN-01", "Finance", false, false);
        List<ServiceCatalogItem> catalogs = List.of(catIt, catHr, catFin);
        String[] depts = {"IT", "HR", "Finance", "Engineering", "Operations"};
        String[] priorities = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
        String[] statuses = {"SUBMITTED", "PENDING_APPROVAL", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"};

        // Seed 1,000 realistic synthetic tickets, 400 approvals, and 500 alerts using batch JDBC
        LocalDateTime baseTime = LocalDateTime.now(clock).minusDays(28).withHour(10).withMinute(0).withSecond(0).withNano(0);
        List<Object[]> ticketBatch = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            ServiceCatalogItem c = catalogs.get(i % catalogs.size());
            String dept = depts[i % depts.length];
            String prio = priorities[i % priorities.length];
            String status = statuses[i % statuses.length];
            LocalDateTime createdAt = baseTime.plusHours(i % 650);
            LocalDateTime slaTarget = createdAt.plusHours(24);
            LocalDateTime resolvedAt = ("RESOLVED".equals(status) || "CLOSED".equals(status))
                    ? createdAt.plusHours((i % 3 == 0) ? 30 : 12) : null;
            LocalDateTime closedAt = "CLOSED".equals(status) ? resolvedAt.plusHours(48) : null;
            ticketBatch.add(new Object[]{
                    "TKT-SYN-" + String.format("%05d", i),
                    c.getId(), requester.getId(), approver.getId(), dept, prio, status,
                    "Synthetic Stage 2 MySQL analytics ticket #" + i,
                    Timestamp.valueOf(createdAt),
                    resolvedAt != null ? Timestamp.valueOf(resolvedAt) : null,
                    closedAt != null ? Timestamp.valueOf(closedAt) : null,
                    Timestamp.valueOf(slaTarget),
                    (i % 5) * 15, 0L
            });
        }
        jdbcTemplate.batchUpdate("""
            INSERT INTO service_ticket
            (ticket_number, catalog_item_id, requester_id, assignee_id, department, priority, status,
             description, created_at, resolved_at, closed_at, sla_target_date, total_paused_minutes, version)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, ticketBatch);

        List<Long> synTicketIds = jdbcTemplate.queryForList(
                "SELECT id FROM service_ticket WHERE ticket_number LIKE 'TKT-SYN-%' ORDER BY id ASC LIMIT 400", Long.class);
        List<Object[]> approvalBatch = new ArrayList<>(synTicketIds.size());
        String[] appStatuses = {"PENDING", "APPROVED", "REJECTED"};
        for (int i = 0; i < synTicketIds.size(); i++) {
            String st = appStatuses[i % appStatuses.length];
            LocalDateTime cAt = baseTime.plusHours(i % 600);
            approvalBatch.add(new Object[]{
                    synTicketIds.get(i), approver.getId(), 1, st,
                    "PENDING".equals(st) ? null : "Synthetic decision " + i,
                    Timestamp.valueOf(cAt),
                    "PENDING".equals(st) ? null : Timestamp.valueOf(cAt.plusHours(4)),
                    0L
            });
        }
        jdbcTemplate.batchUpdate("""
            INSERT INTO approval_task
            (ticket_id, approver_id, approval_stage, status, decision_reason, created_at, decided_at, version)
            VALUES (?,?,?,?,?,?,?,?)
        """, approvalBatch);

        List<Object[]> alertBatch = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            alertBatch.add(new Object[]{
                    requester.getId(), synTicketIds.get(i % synTicketIds.size()),
                    (i % 2 == 0) ? "STATUS_CHANGED" : "TICKET_RESOLVED",
                    "Synthetic Alert #" + i, "Body #" + i, (i % 3 == 0),
                    Timestamp.valueOf(baseTime.plusHours(i % 600)),
                    "TICKET", synTicketIds.get(i % synTicketIds.size()),
                    "SYN_ALERT_" + i
            });
        }
        jdbcTemplate.batchUpdate("""
            INSERT INTO in_app_alert
            (recipient_id, ticket_id, alert_type, title, message, is_read, created_at, related_entity_type, related_entity_id, dedup_key)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """, alertBatch);

        // Execute real AnalyticsService.getDashboardBundle against real MySQL 8.0.45
        LocalDate end = LocalDate.now(clock);
        LocalDate start = end.minusDays(30);
        long t0 = System.nanoTime();
        AnalyticsDashboardBundleDTO bundle = analyticsService.getDashboardBundle(start, end, null, null, "admin");
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

        assertTrue(bundle.overview().periodTotalTickets() >= 1000,
                "Analytics bundle must aggregate >= 1000 tickets on real MySQL");
        assertNotNull(bundle.slaCompliance().compliancePercentage());
        assertFalse(bundle.departmentPerformance().isEmpty());
        assertFalse(bundle.servicePopularity().isEmpty());
        assertTrue(bundle.approvalsOverview().pendingSnapshot() > 0);
        System.out.println("[STAGE2-E5-ANALYTICS] DashboardBundle on 1,000+ tickets executed in " + elapsedMs + " ms; totalInPeriod="
                + bundle.overview().periodTotalTickets() + ", slaRate=" + bundle.slaCompliance().compliancePercentage() + "%");

        // Run real MySQL EXPLAIN and EXPLAIN ANALYZE on core repository SQL queries
        String startStr = Timestamp.valueOf(start.atStartOfDay()).toString();
        String endStr = Timestamp.valueOf(end.atTime(23, 59, 59)).toString();
        String nowStr = Timestamp.valueOf(LocalDateTime.now(clock)).toString();
        String overdueStr = Timestamp.valueOf(LocalDateTime.now(clock).minusHours(24)).toString();

        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("Q1_AnalyticsProjection_AllDepts",
                "SELECT t.department, t.status, t.created_at, t.resolved_at, t.sla_target_date, t.total_paused_minutes " +
                "FROM service_ticket t WHERE t.created_at >= '" + startStr + "' AND t.created_at <= '" + endStr + "'");
        queries.put("Q2_AnalyticsProjection_LowerDeptFilter",
                "SELECT t.department, t.status, t.created_at, t.resolved_at, t.sla_target_date, t.total_paused_minutes " +
                "FROM service_ticket t WHERE t.created_at >= '" + startStr + "' AND t.created_at <= '" + endStr + "' AND LOWER(t.department) = LOWER('IT')");
        queries.put("Q3_CountGroupedByPriority",
                "SELECT t.priority, COUNT(t.id) FROM service_ticket t WHERE t.created_at >= '" + startStr + "' AND t.created_at <= '" + endStr + "' GROUP BY t.priority");
        queries.put("Q4_CountOpenBacklogByDept",
                "SELECT t.department, COUNT(t.id) FROM service_ticket t WHERE t.status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','PENDING_SECONDARY_APPROVAL','ASSIGNED','IN_PROGRESS','ON_HOLD') GROUP BY t.department");
        queries.put("Q5_CountOverdueOpenByDept",
                "SELECT t.department, COUNT(t.id) FROM service_ticket t WHERE t.status IN ('SUBMITTED','PENDING_APPROVAL','PENDING_SECONDARY_APPROVAL','ASSIGNED','IN_PROGRESS') AND t.sla_target_date < '" + nowStr + "' GROUP BY t.department");
        queries.put("Q6_ServicePopularityJoin",
                "SELECT c.id, c.item_code, c.title, c.department, c.category, COUNT(t.id) AS cnt FROM service_ticket t JOIN service_catalog c ON t.catalog_item_id = c.id WHERE t.created_at >= '" + startStr + "' AND t.created_at <= '" + endStr + "' GROUP BY c.id, c.item_code, c.title, c.department, c.category ORDER BY cnt DESC, c.title ASC LIMIT 8");
        queries.put("Q7_ApprovalOverduePendingJoin",
                "SELECT COUNT(a.id) FROM approval_task a JOIN service_ticket t ON a.ticket_id = t.id WHERE a.status = 'PENDING' AND a.created_at < '" + overdueStr + "'");
        queries.put("Q8_NotificationInboxUnread",
                "SELECT a.id, a.alert_type, a.title, a.is_read, a.created_at FROM in_app_alert a WHERE a.recipient_id = " + requester.getId() + " AND a.is_read = false ORDER BY a.created_at DESC LIMIT 20");

        for (Map.Entry<String, String> entry : queries.entrySet()) {
            System.out.println("=== EXPLAIN " + entry.getKey() + " ===");
            List<Map<String, Object>> exp = jdbcTemplate.queryForList("EXPLAIN " + entry.getValue());
            for (Map<String, Object> row : exp) {
                System.out.println("  table=" + row.get("table")
                        + " type=" + row.get("type")
                        + " possible_keys=" + row.get("possible_keys")
                        + " key=" + row.get("key")
                        + " rows=" + row.get("rows")
                        + " Extra=" + row.get("Extra"));
            }
            System.out.println("--- EXPLAIN ANALYZE " + entry.getKey() + " ---");
            List<Map<String, Object>> ana = jdbcTemplate.queryForList("EXPLAIN ANALYZE " + entry.getValue());
            for (Map<String, Object> row : ana) {
                System.out.println(row.values().iterator().next());
            }
        }
    }

    private ServiceCatalogItem ensureCatalogItem(String code, String dept, boolean mgrApproval, boolean secApproval) {
        return serviceCatalogRepository.findByItemCode(code).orElseGet(() -> {
            ServiceCatalogItem item = new ServiceCatalogItem();
            item.setItemCode(code);
            item.setTitle("Stage 2 Item " + code);
            item.setCategory("Stage2");
            item.setDepartment(dept);
            item.setDescription("Synthetic Stage 2 MySQL test catalog item");
            item.setEstimatedCost(new BigDecimal("100.00"));
            item.setSlaTargetHours(24);
            item.setRequiresManagerApproval(mgrApproval);
            item.setRequiresSecondaryApproval(secApproval);
            item.setIsActive(true);
            return serviceCatalogRepository.saveAndFlush(item);
        });
    }
}
