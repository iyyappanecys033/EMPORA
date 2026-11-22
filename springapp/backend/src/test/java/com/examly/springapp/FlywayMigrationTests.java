package com.examly.springapp;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlywayMigrationTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @Order(1)
    void test1_FreshDatabaseMigrationsAppliedSuccessfully() {
        assertNotNull(flyway, "Flyway bean must be configured and injected");
        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 4, "At least 4 migrations (V1, V2, V3, V4) must be applied");

        for (MigrationInfo info : applied) {
            assertEquals(MigrationState.SUCCESS, info.getState(), 
                    "Migration " + info.getScript() + " must be in SUCCESS state");
        }

        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Current migration must not be null");
        assertEquals("4", current.getVersion().getVersion(), "Current schema version must be 4");
    }

    @Test
    @Order(2)
    void test2_All14ExpectedTablesExistInSchema() throws Exception {
        Set<String> expectedTables = Set.of(
                "flyway_schema_history",
                "user_account",
                "role",
                "user_role_mapping",
                "profile",
                "refresh_token",
                "audit_log",
                "account_status_log",
                "service_catalog",
                "service_ticket",
                "ticket_comment",
                "approval_task",
                "audit_chain_head",
                "in_app_alert"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        for (String expected : expectedTables) {
            assertTrue(actualTables.contains(expected), 
                    "Expected table '" + expected + "' was not found in database. Actual tables: " + actualTables);
        }

        // Verify extended audit_log columns exist in schema (Option A: sequence_number, previous_hash, current_hash)
        Set<String> auditColumns = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "audit_log", "%")) {
                while (rs.next()) {
                    auditColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            if (auditColumns.isEmpty()) {
                try (ResultSet rs = metaData.getColumns(null, null, "AUDIT_LOG", "%")) {
                    while (rs.next()) {
                        auditColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        }
        assertTrue(auditColumns.contains("sequence_number"), "audit_log must contain sequence_number column");
        assertTrue(auditColumns.contains("previous_hash"), "audit_log must contain previous_hash column");
        assertTrue(auditColumns.contains("current_hash"), "audit_log must contain current_hash column");
    }

    private Set<String> getImportedKeyTables(DatabaseMetaData metaData, String tableName) throws Exception {
        Set<String> fks = new HashSet<>();
        try (ResultSet rs = metaData.getImportedKeys(null, null, tableName.toLowerCase())) {
            while (rs.next()) {
                fks.add(rs.getString("PKTABLE_NAME").toLowerCase());
            }
        }
        if (fks.isEmpty()) {
            try (ResultSet rs = metaData.getImportedKeys(null, null, tableName.toUpperCase())) {
                while (rs.next()) {
                    fks.add(rs.getString("PKTABLE_NAME").toLowerCase());
                }
            }
        }
        return fks;
    }

    @Test
    @Order(3)
    void test3_CoreForeignKeysAndConstraintsExist() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Verify UserRoleMapping foreign keys
            Set<String> urmFks = getImportedKeyTables(metaData, "user_role_mapping");
            assertTrue(urmFks.contains("user_account"), "USER_ROLE_MAPPING must reference user_account");
            assertTrue(urmFks.contains("role"), "USER_ROLE_MAPPING must reference role");

            // Verify Profile foreign key
            Set<String> profileFks = getImportedKeyTables(metaData, "profile");
            assertTrue(profileFks.contains("user_account"), "PROFILE must reference user_account");

            // Verify ServiceTicket foreign keys
            Set<String> ticketFks = getImportedKeyTables(metaData, "service_ticket");
            assertTrue(ticketFks.contains("service_catalog"), "SERVICE_TICKET must reference service_catalog");
            assertTrue(ticketFks.contains("user_account"), "SERVICE_TICKET must reference user_account");

            // Verify RefreshToken foreign key
            Set<String> tokenFks = getImportedKeyTables(metaData, "refresh_token");
            assertTrue(tokenFks.contains("user_account"), "REFRESH_TOKEN must reference user_account");

            // Verify TicketComment foreign keys
            Set<String> commentFks = getImportedKeyTables(metaData, "ticket_comment");
            assertTrue(commentFks.contains("service_ticket"), "TICKET_COMMENT must reference service_ticket");
            assertTrue(commentFks.contains("user_account"), "TICKET_COMMENT must reference user_account");

            // Verify ApprovalTask foreign keys
            Set<String> approvalFks = getImportedKeyTables(metaData, "approval_task");
            assertTrue(approvalFks.contains("service_ticket"), "APPROVAL_TASK must reference service_ticket");
            assertTrue(approvalFks.contains("user_account"), "APPROVAL_TASK must reference user_account");
        }
    }

    @Test
    @Order(4)
    void test4_AuditChainHeadGenesisRecordInitialized() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM audit_chain_head WHERE id = 1");
        assertFalse(rows.isEmpty(), "Genesis audit chain head record must exist");
        Map<String, Object> head = rows.get(0);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", head.get("current_hash"));
        assertEquals(0L, ((Number) head.get("last_sequence")).longValue());
    }

    @Test
    @Order(5)
    void test5_ExistingDatabaseUpgradePreservesAllData() {
        // Create an isolated disposable database instance to simulate Scenario 2: Existing Brownfield Database Upgrade
        String upgradeDbUrl = "jdbc:h2:mem:upgrade_test_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource upgradeDs = new DriverManagerDataSource();
        upgradeDs.setDriverClassName("org.h2.Driver");
        upgradeDs.setUrl(upgradeDbUrl);
        upgradeDs.setUsername("sa");
        upgradeDs.setPassword("");

        JdbcTemplate upgradeJdbc = new JdbcTemplate(upgradeDs);

        // 1. Manually create pre-existing legacy schema (simulating pre-Flyway Hibernate state)
        upgradeJdbc.execute("""
            CREATE TABLE user_account (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                email VARCHAR(100) NULL,
                phone VARCHAR(20) NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                failed_login_attempts INT NOT NULL DEFAULT 0,
                locked_until TIMESTAMP NULL
            );
        """);

        upgradeJdbc.execute("""
            CREATE TABLE role (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                role_name VARCHAR(50) NOT NULL UNIQUE
            );
        """);

        upgradeJdbc.execute("""
            CREATE TABLE user_role_mapping (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                role_id BIGINT NOT NULL
            );
        """);

        upgradeJdbc.execute("""
            CREATE TABLE profile (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(30) NOT NULL UNIQUE,
                user_id BIGINT NOT NULL UNIQUE,
                first_name VARCHAR(50) NOT NULL,
                last_name VARCHAR(50) NOT NULL,
                address VARCHAR(255) NULL,
                dob DATE NULL,
                gender VARCHAR(20) NULL,
                department VARCHAR(50) NOT NULL,
                designation VARCHAR(100) NOT NULL,
                salary DOUBLE NULL,
                date_of_joining DATE NOT NULL,
                reporting_manager VARCHAR(50) NULL,
                emergency_contact VARCHAR(50) NULL
            );
        """);

        upgradeJdbc.execute("""
            CREATE TABLE refresh_token (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                token_hash VARCHAR(255) NOT NULL UNIQUE,
                user_id BIGINT NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                revoked BOOLEAN NOT NULL DEFAULT FALSE
            );
        """);

        upgradeJdbc.execute("""
            CREATE TABLE audit_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                actor_username VARCHAR(50) NULL,
                action VARCHAR(50) NULL,
                target_username VARCHAR(50) NULL,
                details VARCHAR(1000) NULL,
                timestamp TIMESTAMP NULL
            );
        """);

        upgradeJdbc.execute("""
            CREATE TABLE account_status_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                status VARCHAR(255) NULL
            );
        """);

        // 2. Insert representative legacy records across ALL 6 IAM tables
        String testBcryptHash = "$2a$10$wO7vI13Lg1J13O0t7Vz7euZ8yG3bU0wUvO7vI13Lg1J13O0t7Vz7e";
        Timestamp lockTime = Timestamp.from(Instant.now().plus(15, ChronoUnit.MINUTES));
        upgradeJdbc.update("INSERT INTO user_account (username, password_hash, email, status, failed_login_attempts, locked_until) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                "legacy_admin", testBcryptHash, "admin@legacy.com", "LOCKED", 5, lockTime);
        Long legacyUserId = upgradeJdbc.queryForObject("SELECT id FROM user_account WHERE username = 'legacy_admin'", Long.class);

        upgradeJdbc.update("INSERT INTO role (role_name) VALUES (?)", "ROLE_ADMIN");
        Long adminRoleId = upgradeJdbc.queryForObject("SELECT id FROM role WHERE role_name = 'ROLE_ADMIN'", Long.class);

        upgradeJdbc.update("INSERT INTO user_role_mapping (user_id, role_id) VALUES (?, ?)", legacyUserId, adminRoleId);

        upgradeJdbc.update("INSERT INTO profile (employee_id, user_id, first_name, last_name, department, designation, salary, date_of_joining, reporting_manager, emergency_contact) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "EMP-9999", legacyUserId, "Legacy", "Admin", "IT", "Director", 125000.0, java.sql.Date.valueOf("2022-01-01"), "chief_exec", "9876543210");

        String legacyTokenHash = "legacy_refresh_token_sha256_hash_9876543210abcdef";
        Timestamp tokenExpiry = Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS));
        upgradeJdbc.update("INSERT INTO refresh_token (token_hash, user_id, expires_at, revoked) VALUES (?, ?, ?, ?)",
                legacyTokenHash, legacyUserId, tokenExpiry, false);

        upgradeJdbc.update("INSERT INTO audit_log (actor_username, action, target_username, details, timestamp) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                "legacy_admin", "LEGACY_EVENT", "system", "Legacy audit event that must be preserved");

        upgradeJdbc.update("INSERT INTO account_status_log (status) VALUES (?)", "LOCKED");

        // 3. Configure Flyway for Controlled Baseline Adoption (Scenario 2: baselineOnMigrate=false, baselineVersion=1)
        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(upgradeDs)
                .baselineOnMigrate(false)
                .baselineVersion("1")
                .baselineDescription("Existing IAM Schema Baseline")
                .locations("classpath:db/migration")
                .load();

        // 4. Execute explicit baseline adoption runbook:
        // Step A: Explicitly stamp baseline at version 1
        upgradeFlyway.baseline();

        // Step B: Apply pending migrations (V2 and V3)
        upgradeFlyway.migrate();

        // 5. Deep post-migration verification across all preserved records
        // A. UserAccount preservation
        Map<String, Object> preservedUser = upgradeJdbc.queryForMap("SELECT * FROM user_account WHERE id = ?", legacyUserId);
        assertEquals("legacy_admin", preservedUser.get("username"));
        assertEquals(testBcryptHash, preservedUser.get("password_hash"), "BCrypt hash must remain identical");
        assertEquals("admin@legacy.com", preservedUser.get("email"));
        assertEquals("LOCKED", preservedUser.get("status"));
        assertEquals(5, ((Number) preservedUser.get("failed_login_attempts")).intValue());
        assertNotNull(preservedUser.get("locked_until"));

        // B. Role and UserRoleMapping preservation
        Map<String, Object> preservedRole = upgradeJdbc.queryForMap("SELECT * FROM role WHERE id = ?", adminRoleId);
        assertEquals("ROLE_ADMIN", preservedRole.get("role_name"));

        Integer mappingCount = upgradeJdbc.queryForObject(
                "SELECT COUNT(*) FROM user_role_mapping WHERE user_id = ? AND role_id = ?", 
                Integer.class, legacyUserId, adminRoleId);
        assertEquals(1, mappingCount, "User-role mapping relationship must be intact after migration");

        // C. Profile preservation
        Map<String, Object> preservedProfile = upgradeJdbc.queryForMap("SELECT * FROM profile WHERE user_id = ?", legacyUserId);
        assertEquals("EMP-9999", preservedProfile.get("employee_id"));
        assertEquals("Legacy", preservedProfile.get("first_name"));
        assertEquals("Admin", preservedProfile.get("last_name"));
        assertEquals("IT", preservedProfile.get("department"));
        assertEquals("Director", preservedProfile.get("designation"));
        assertEquals(125000.0, ((Number) preservedProfile.get("salary")).doubleValue(), 0.001);
        assertEquals("chief_exec", preservedProfile.get("reporting_manager"));
        assertEquals("9876543210", preservedProfile.get("emergency_contact"));

        // D. RefreshToken preservation
        Map<String, Object> preservedToken = upgradeJdbc.queryForMap("SELECT * FROM refresh_token WHERE user_id = ?", legacyUserId);
        assertEquals(legacyTokenHash, preservedToken.get("token_hash"));
        assertEquals(false, preservedToken.get("revoked"));
        assertNotNull(preservedToken.get("expires_at"));

        // E. AuditLog preservation & Unchained legacy status
        List<Map<String, Object>> preservedAudit = upgradeJdbc.queryForList("SELECT * FROM audit_log WHERE actor_username = 'legacy_admin'");
        assertFalse(preservedAudit.isEmpty(), "Legacy audit record must be preserved");
        Map<String, Object> auditRecord = preservedAudit.get(0);
        assertEquals("LEGACY_EVENT", auditRecord.get("action"));
        assertEquals("Legacy audit event that must be preserved", auditRecord.get("details"));
        // Confirm legacy record was not assigned fabricated hashes
        assertNull(auditRecord.get("sequence_number"), "Legacy audit event sequence_number must remain NULL");
        assertNull(auditRecord.get("previous_hash"), "Legacy audit event previous_hash must remain NULL");
        assertNull(auditRecord.get("current_hash"), "Legacy audit event current_hash must remain NULL");

        // F. AccountStatusLog preservation
        Integer statusLogCount = upgradeJdbc.queryForObject("SELECT COUNT(*) FROM account_status_log WHERE status = 'LOCKED'", Integer.class);
        assertEquals(1, statusLogCount);

        // 6. Verify that new V2 and V3 tables are created on the upgraded database
        assertDoesNotThrow(() -> upgradeJdbc.execute("SELECT COUNT(*) FROM service_catalog"), 
                "service_catalog table must exist on upgraded database");
        assertDoesNotThrow(() -> upgradeJdbc.execute("SELECT COUNT(*) FROM service_ticket"), 
                "service_ticket table must exist on upgraded database");
        assertDoesNotThrow(() -> upgradeJdbc.execute("SELECT COUNT(*) FROM ticket_comment"), 
                "ticket_comment table must exist on upgraded database");
        assertDoesNotThrow(() -> upgradeJdbc.execute("SELECT COUNT(*) FROM approval_task"), 
                "approval_task table must exist on upgraded database");
        assertDoesNotThrow(() -> upgradeJdbc.execute("SELECT COUNT(*) FROM in_app_alert"), 
                "in_app_alert table must exist on upgraded database");

        // Verify genesis head record exists on the upgraded database
        List<Map<String, Object>> upgradeHead = upgradeJdbc.queryForList("SELECT * FROM audit_chain_head WHERE id = 1");
        assertFalse(upgradeHead.isEmpty(), "Genesis audit chain head record must exist on upgraded database");
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", upgradeHead.get(0).get("current_hash"));
        assertEquals(0L, ((Number) upgradeHead.get(0).get("last_sequence")).longValue());
    }

    @Test
    @Order(6)
    void test6_IdempotentMigrationDoesNotDuplicateData() {
        // Calling migrate() again must not fail or duplicate data
        int applied = flyway.migrate().migrationsExecuted;
        assertEquals(0, applied, "Repeated migration must execute 0 new scripts");
    }

    @Test
    @Order(7)
    void test7_HibernateValidationSucceeds() {
        assertNotNull(entityManagerFactory, "EntityManagerFactory must be active");
        assertTrue(entityManagerFactory.isOpen(), "EntityManagerFactory must be open");
    }

    @Test
    @Order(8)
    void test8_V4NotificationMetadataColumnsExist() throws Exception {
        Set<String> alertColumns = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "in_app_alert", "%")) {
                while (rs.next()) {
                    alertColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            if (alertColumns.isEmpty()) {
                try (ResultSet rs = metaData.getColumns(null, null, "IN_APP_ALERT", "%")) {
                    while (rs.next()) {
                        alertColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }
        }
        assertTrue(alertColumns.contains("read_at"), "in_app_alert must contain read_at column from V4");
        assertTrue(alertColumns.contains("related_entity_type"), "in_app_alert must contain related_entity_type column from V4");
        assertTrue(alertColumns.contains("related_entity_id"), "in_app_alert must contain related_entity_id column from V4");
        assertTrue(alertColumns.contains("dedup_key"), "in_app_alert must contain dedup_key column from V4");
    }
}
