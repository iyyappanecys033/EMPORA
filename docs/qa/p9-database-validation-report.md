# EPAMS P9 — Database Schema, Migration & Persistence Validation Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  

---

## 1. Execution Status Summary (Strict Evidence Classification)

| Database Target | Migration Scope | Hibernate Validation | Verification Result |
| :--- | :--- | :--- | :---: |
| **H2 2.3 (MySQL Compatibility Mode)** | Flyway `V1`–`V4` clean + brownfield V1 preservation | `spring.jpa.hibernate.ddl-auto=validate` | `VERIFIED` (`SchemaValidationTests`, `BrownfieldPreservationTests`, 15 test suites) |
| **Live MySQL 8.0+ Instance** | Flyway `V1`–`V4` + `EXPLAIN` query plans | `application-mysql.properties` (`ddl-auto=validate`) | **`BLOCKED`** (Sandbox blocks TCP connections to host MySQL daemon) |

> **Honest Boundary Statement:** H2 MySQL compatibility mode verifies JPA entity-to-table structural alignment, Flyway migration ordering, foreign keys, unique constraints, and transactional rollbacks. It does **NOT** certify live MySQL 8.0+ InnoDB behavior (collations, `DATETIME(6)` precision, strict SQL modes, gap locking, or optimizer index selection). Live MySQL 8.0+ verification remains a mandatory P10 pre-deployment gate.

---

## 2. Flyway Migration Inventory & Integrity Audit (V1–V4)

All four historical Flyway migrations were inspected read-only and **not modified** during P9:

| Version | Migration File | Primary Tables & Constraints | P9 Audit Status |
| :---: | :--- | :--- | :---: |
| **V1** | `V1__init_iam_schema.sql` | `user_account`, `role`, `profile`, `user_role_mapping`, `account_status_log`, `audit_log`, `refresh_token` | `VERIFIED` (Unmodified; brownfield baseline preserved) |
| **V2** | `V2__service_catalog_and_tickets.sql` | `service_catalog_item`, `service_ticket` (`version` optimistic lock column, `ticket_number` unique), `ticket_comment` | `VERIFIED` (Unmodified; matches JPA entities under `ddl-auto=validate`) |
| **V3** | `V3__approvals_and_audit_chain.sql` | `approval_task` (unique `(ticket_id, stage)`), `audit_log` hash-chain columns (`previous_hash`, `record_hash`, `chain_sequence`) | `VERIFIED` (Unmodified; tamper-detection & concurrency validated) |
| **V4** | `V4__notifications_and_analytics_indexes.sql` | Additive columns on `in_app_alert` (`read_at`, `related_entity_type`, `related_entity_id`, `dedup_key`), unique constraint `uk_alert_recipient_dedup`, `idx_alert_recipient_type` + composite analytics indexes on `service_ticket` & `approval_task` | `VERIFIED` (Unmodified; atomic DB dedup under 8 threads validated) |

---

## 3. Persistence Integrity Controls Validated in P9

1. **Hibernate Schema Alignment (`ddl-auto=validate`):** Both `src/test/resources/application.properties` and `src/main/resources/application-mysql.properties` enforce `ddl-auto=validate`, ensuring Hibernate never silently mutates tables and fails fast on any column/type drift.
2. **Brownfield Preservation:** `BrownfieldPreservationTests` validates that an existing Phase 1 database containing legacy `V1` IAM records cleanly upgrades through `V2`–`V4` without data loss or orphaned foreign keys.
3. **Optimistic Concurrency Control:** `ServiceTicket.@Version` verified under true concurrent conflicting updates (`P9SecurityAndWorkflowQaTests.test6`) — stale version rejected cleanly with HTTP `409 Conflict` (`OptimisticLockingConflictException`).
4. **Atomic Deduplication Constraint:** `in_app_alert` unique index `uk_alert_recipient_dedup` (`recipient_id`, `dedup_key`) verified under 8 simultaneous threads (`P9SecurityAndWorkflowQaTests.test8`) — exactly 1 row persisted, zero unhandled exceptions or duplicate alerts.
5. **Audit Hash Chain Persistence:** 6 concurrent threads appending chained audit records (`P9SecurityAndWorkflowQaTests.test7`) produced a contiguous, cryptographically valid SHA-256 chain (`AuditChainService.verifyChain().isValid() == true`).
6. **Production Destructive Reset Guard:** `application-mysql.properties` explicitly sets `epams.audit.allow-chain-reset=false` (`SEC-P9-01`).

---

## 4. Reproducible Live MySQL 8.0+ Verification Runbook (Mandatory P10 Gate)

Execute the following steps on a host with MySQL 8.0+ accessible on TCP `3306`:

```powershell
# 1. Create clean verification database
mysql -u root -p -e "DROP DATABASE IF EXISTS epams_p10_verify; CREATE DATABASE epams_p10_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Boot Spring Boot with MySQL profile (executes Flyway V1-V4 + Hibernate ddl-auto=validate)
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/epams_p10_verify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="<password>"
mvn spring-boot:run -f springapp/pom.xml

# 3. Verify Flyway history and composite index usage via EXPLAIN
mysql -u root -p epams_p10_verify -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
mysql -u root -p epams_p10_verify -e "EXPLAIN SELECT * FROM in_app_alert WHERE recipient_id = 1 AND is_read = 0 ORDER BY created_at DESC LIMIT 20;"
mysql -u root -p epams_p10_verify -e "EXPLAIN SELECT status, COUNT(*) FROM service_ticket WHERE created_at BETWEEN '2026-10-01' AND '2026-11-30' GROUP BY status;"
```
