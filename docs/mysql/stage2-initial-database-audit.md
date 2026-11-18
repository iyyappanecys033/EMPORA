# EPAMS â€” Stage 2 Initial Read-Only Database Audit (Stage A)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration (Stage A: Read-Only Baseline Audit)
**Runtime Baseline:** Java `25.0.2+10-LTS`, Spring Boot `3.5.16`, Hibernate `6.6.53.Final`, Flyway `11.7.2`, `mysql-connector-j` (managed)
**Date:** 2026-11-18

---

## 1. Build & Persistence Dependencies (`springapp/pom.xml`)

- `spring-boot-starter-data-jpa` (Hibernate ORM `6.6.53.Final`, HikariCP connection pool)
- `com.mysql:mysql-connector-j` (`runtime` scope, Spring Boot `3.5.16` managed)
- `org.flywaydb:flyway-core` + `org.flywaydb:flyway-mysql` (`11.7.2`)
- `com.h2database:h2` (`runtime`/`test` only â€” **not used for Stage 2 MySQL verification**)

---

## 2. Flyway `V1`â€“`V4` Schema Object Inventory

### 2.1 Tables, Engines, Charsets & Columns (`13` Application Tables + `flyway_schema_history`)

| Migration | Table | Engine / Charset | Primary Key | Foreign Keys | Unique Constraints / Indexes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **V1** | `user_account` | InnoDB / `utf8mb4` | `id BIGINT AI` | â€” | `uk_user_account_username (username)` |
| **V1** | `role` | InnoDB / `utf8mb4` | `id BIGINT AI` | â€” | `uk_role_role_name (role_name)` |
| **V1** | `user_role_mapping` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_urm_user â†’ user_account(id) CASCADE`, `fk_urm_role â†’ role(id) RESTRICT` | â€” |
| **V1** | `profile` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_profile_user â†’ user_account(id) CASCADE` | `uk_profile_employee_id (employee_id)`, `idx_profile_department`, `idx_profile_reporting_manager` |
| **V1** | `refresh_token` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_refresh_token_user â†’ user_account(id) CASCADE` | `uk_refresh_token_hash (token_hash)` |
| **V1 + V3** | `audit_log` | InnoDB / `utf8mb4` | `id BIGINT AI` | â€” | `idx_audit_target_user`, `idx_audit_action`; **V3 adds** `sequence_number BIGINT NULL`, `previous_hash CHAR(64) NULL`, `current_hash CHAR(64) NULL`, `uk_audit_log_sequence (sequence_number)`, `idx_audit_log_curr_hash (current_hash)` |
| **V1** | `account_status_log` | InnoDB / `utf8mb4` | `id BIGINT AI` | â€” | â€” |
| **V2** | `service_catalog` | InnoDB / `utf8mb4` | `id BIGINT AI` | â€” | `uk_service_catalog_item_code (item_code)`, `idx_catalog_department`, `idx_catalog_category` |
| **V2 + V4** | `service_ticket` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_ticket_catalog`, `fk_ticket_requester`, `fk_ticket_assignee` | `uk_ticket_number (ticket_number)`, `idx_ticket_dept_status`, `idx_ticket_requester_created`, `idx_ticket_status_sla`, `idx_ticket_resolved_status`; **V4 adds** `idx_ticket_created_dept_status`, `idx_ticket_priority_dept`, `idx_ticket_catalog_created` |
| **V2** | `ticket_comment` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_comment_ticket â†’ service_ticket(id) CASCADE`, `fk_comment_author â†’ user_account(id)` | `idx_comment_ticket_created (ticket_id, created_at)` |
| **V3 + V4** | `approval_task` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_approval_ticket â†’ service_ticket(id) CASCADE`, `fk_approval_approver â†’ user_account(id)` | `uk_approval_ticket_stage (ticket_id, approval_stage)`, `idx_approval_approver_status`; **V4 adds** `idx_approval_status_created` |
| **V3** | `audit_chain_head` | InnoDB / `utf8mb4` | `id BIGINT` (genesis `1`) | â€” | Seeded with `id=1`, `current_hash=64*'0'`, `last_sequence=0` |
| **V3 + V4** | `in_app_alert` | InnoDB / `utf8mb4` | `id BIGINT AI` | `fk_alert_recipient â†’ user_account(id) CASCADE`, `fk_alert_ticket â†’ service_ticket(id) SET NULL` | `idx_alert_recipient_read`; **V4 adds** `read_at`, `related_entity_type`, `related_entity_id`, `dedup_key`, `idx_alert_recipient_type`, `uk_alert_recipient_dedup (recipient_id, dedup_key)` |

---

## 3. Entity Mapping, Transaction, Concurrency & Query Audit

### 3.1 JPA Entities & Validation Risk Points to Test on Real MySQL
- **13 JPA `@Entity` classes:** `UserAccount`, `Role`, `UserRoleMapping`, `Profile`, `RefreshToken`, `AuditLog`, `AccountStatusLog`, `ServiceCatalogItem`, `ServiceTicket`, `TicketComment`, `ApprovalTask`, `AuditChainHead`, `InAppAlert`.
- **MySQL Type Sensitivity Points (must pass `spring.jpa.hibernate.ddl-auto=validate` on real MySQL):**
  - `AuditLog.previousHash` / `currentHash` & `AuditChainHead.currentHash`: annotated `@JdbcTypeCode(SqlTypes.CHAR)` with `length = 64` matching MySQL `CHAR(64)`.
  - `ServiceTicket.description`, `TicketComment.commentText`, `ApprovalTask.decisionReason`, `InAppAlert.message`, `ServiceCatalogItem.description`: mapped `columnDefinition = "TEXT"` matching MySQL `TEXT`.
  - `Profile.salary`: `Double` mapped against MySQL `DOUBLE`.
  - `BOOLEAN` columns (`refresh_token.revoked`, `service_catalog.requires_*`, `service_catalog.is_active`, `ticket_comment.is_internal`, `in_app_alert.is_read`): MySQL translates `BOOLEAN` to `TINYINT(1)` â€” must verify Hibernate `6.6.53.Final` `MySQLDialect` validates cleanly.
  - `@Version` optimistic locking: `ServiceTicket.version` (`BIGINT NOT NULL DEFAULT 0`) and `ApprovalTask.version` (`BIGINT NOT NULL DEFAULT 0`).

### 3.2 Audit Chain (`AuditChainService`)
- `recordChainedEvent` uses `@Transactional(propagation = REQUIRED, isolation = READ_COMMITTED)` + `AuditChainHeadRepository.findByIdForUpdate(1L)` (`@Lock(LockModeType.PESSIMISTIC_WRITE)` â†’ MySQL InnoDB `SELECT ... FOR UPDATE`).
- Timestamps truncated to `ChronoUnit.SECONDS` before hashing to match MySQL `TIMESTAMP` (0 fractional seconds) round-trip fidelity â€” **must be empirically verified on real MySQL under `serverTimezone=UTC`**.
- `resetChainToGenesis()` is strictly disabled in `application-mysql.properties` (`epams.audit.allow-chain-reset=false`).

### 3.3 Notification Deduplication (`NotificationEventListener`)
- Executes at `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` inside `@Transactional(propagation = REQUIRES_NEW)`.
- Performs pre-check `existsByRecipientIdAndDedupKey` + enforces database-level uniqueness via `uk_alert_recipient_dedup (recipient_id, dedup_key)` catching `DataIntegrityViolationException`.
- MySQL `UNIQUE INDEX` permits multiple `NULL` values for `dedup_key` (non-deduplicated alerts) while strictly enforcing uniqueness for non-null `(recipient_id, dedup_key)` pairs.
- External email delivery runs strictly **after** `saveAndFlush(alert)` succeeds, preventing duplicate emails on concurrent races.

### 3.4 Analytics Queries & Potential MySQL Planner Observations (`ServiceTicketRepository`, `ApprovalTaskRepository`)
- Uses `LOWER(t.department) = LOWER(:dept)` across analytics JPQL queries; in MySQL `utf8mb4` case-insensitive collations (`utf8mb4_0900_ai_ci` / `utf8mb4_unicode_ci`), wrapping an indexed column in `LOWER()` can prevent B-tree range seeks on `department` predicates. Stage E.5 `EXPLAIN` / `EXPLAIN ANALYZE` will capture actual MySQL query plans empirically.
- `SlaScheduledProcessor`: uses `AtomicBoolean` in-process overlap guards + per-ticket `TransactionTemplate(PROPAGATION_REQUIRES_NEW)` + optimistic locking conflict handling (`ObjectOptimisticLockingFailureException`).

### 3.5 MySQL Configuration & Privilege Model (`application-mysql.properties`)
- **Connection Pool:** HikariCP (`maximum-pool-size=10`, `minimum-idle=5`, `idle-timeout=300000`, `connection-timeout=20000`, `max-lifetime=1200000`).
- **Migration Governance:** `spring.flyway.baseline-on-migrate=false`, `baseline-version=1`, `validate-on-migrate=true`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.h2.console.enabled=false`.
- **Timestamp / Timezone Handling:** JDBC URL specifies `serverTimezone=UTC`; application `Clock` bean uses `Asia/Kolkata`.
- **Required Least-Privilege Separation (`ADR-011`):**
  - `epams_migrator`: `CREATE, ALTER, DROP, INDEX, REFERENCES, SELECT, INSERT, UPDATE, DELETE` on target EPAMS schema only (no global privileges).
  - `epams_app`: `SELECT, INSERT, UPDATE, DELETE` on operational tables (`user_account`, `role`, `user_role_mapping`, `profile`, `refresh_token`, `account_status_log`, `service_catalog`, `service_ticket`, `ticket_comment`, `approval_task`, `in_app_alert`); **`SELECT, INSERT` ONLY on `audit_log`** (no `UPDATE`/`DELETE`); **`SELECT, UPDATE` ONLY on `audit_chain_head`** (no `DELETE`/DDL); **`SELECT` ONLY on `flyway_schema_history`** (for `validate-on-migrate=true`).

### 3.6 Existing Migration Test Baseline (`FlywayMigrationTests.java`)
- Contains 8 JUnit tests currently targeting H2 (`MODE=MySQL`) only â€” provides zero real MySQL proof. Stage 2 will execute dedicated verification against real MySQL 8+.
