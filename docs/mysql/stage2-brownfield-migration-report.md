# EPAMS â€” Stage 2 Brownfield MySQL Data Preservation Report (Stage D)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration (Stage D: Brownfield Database Migration & Data Preservation)
**Target Database:** `epams_stage2_brownfield` on isolated MySQL `8.0.45` (`127.0.0.1:13307`)
**Verification Method:** `Stage2RealMysqlIntegrationTests.test2_StageD_BrownfieldRealMysqlDataPreservationAndUpgrade` (real MySQL `8.0.45`, zero H2)
**Date:** 2026-11-18
**Verification Status:** **`PASSED (VERIFIED AGAINST REAL MYSQL 8.0.45)`**

---

## 1. Pre-Migration Legacy Schema & Seeded Dataset

Initialized `epams_stage2_brownfield` (empty `utf8mb4` / `utf8mb4_unicode_ci` database) as `epams_migrator`@`127.0.0.1` with the pre-Flyway 7-table legacy IAM schema (`user_account`, `role`, `user_role_mapping`, `profile`, `refresh_token`, `audit_log` without hash-chain columns, `account_status_log`) and NO `flyway_schema_history` or V2â€“V4 tables.

Seeded representative legacy records across all 7 tables:
- **`user_account` (2 rows):** `bf_admin` (`status='LOCKED'`, `failed_login_attempts=5`, `locked_until='2026-11-18 12:30:00'`, real 60-char BCrypt hash) and `bf_employee` (`status='ACTIVE'`, `failed_login_attempts=0`, distinct BCrypt hash).
- **`role` (2 rows) & `user_role_mapping` (2 rows):** `ROLE_ADMIN` â†’ `bf_admin`, `ROLE_EMPLOYEE` â†’ `bf_employee`.
- **`profile` (2 rows):** `EMP-BF-001` (`Ananya Krishnan`, `IT`, `Principal Architect`, `salary=185000.50`, `dob=1988-04-15`, `date_of_joining=2021-07-01`, `reporting_manager=ceo_office`) and `EMP-BF-002` (`Karthik Subramanian`, `Engineering`, `Senior SDE`, `salary=112500.00`).
- **`refresh_token` (2 rows):** Active token (`revoked=false`) + revoked token (`revoked=true`) with SHA-256 hashes and expiry timestamps.
- **`audit_log` (2 legacy unchained rows):** `LEGACY_LOGIN_LOCK` and `LEGACY_ROLE_ASSIGN`.
- **`account_status_log` (2 rows):** `ACTIVE` and `LOCKED`.

---

## 2. Controlled Flyway Baseline Adoption & Upgrade Execution

Executed the documented production brownfield runbook (`baselineOnMigrate=false`, `baselineVersion="1"`, `validateOnMigrate=true`):
1. `flyway.baseline()` â€” stamped existing IAM schema at version `1` (`MigrationState.BASELINE`, `Existing IAM Schema Baseline`).
2. `flyway.migrate()` â€” executed **exact 3 pending migrations (`V2`, `V3`, `V4`)** against real MySQL `8.0.45`.
3. Repeated `flyway.validate()` + `flyway.migrate()` â€” verified **0** additional migrations executed (100% idempotent).

---

## 3. Byte-for-Byte Data Preservation & Schema Verification (`VERIFIED`)

| Check | Verified Outcome on Real MySQL `8.0.45` |
| :--- | :--- |
| **Account IDs, Credentials & Lockout State** | `bf_admin` and `bf_employee` IDs, BCrypt password hashes, email, phone, `status='LOCKED'`, `failed_login_attempts=5`, and `locked_until` preserved identically. |
| **Role Assignments** | Both `role` rows and both `user_role_mapping` foreign-key associations preserved intact. |
| **Profile Values** | `employee_id`, names, address, `salary` (`185000.50`), `dob`, `date_of_joining`, `reporting_manager`, `emergency_contact` preserved with zero truncation. |
| **Refresh Tokens** | Both `token_hash` values, `user_id` links, `expires_at`, and `revoked` (`false`/`true`) flags preserved verbatim. |
| **Legacy Audit Records (No Fabricated Hashes)** | Both pre-Flyway `audit_log` rows preserved intact with **`sequence_number IS NULL`**, **`previous_hash IS NULL`**, and **`current_hash IS NULL`** â€” zero fabricated hashes applied. |
| **Account Status History** | Both `account_status_log` rows preserved intact. |
| **Schema Structural Integrity** | Exact **14 tables** (`0` duplicate tables), **`0` non-InnoDB tables** (100% InnoDB), all **13 foreign keys** intact, genesis `audit_chain_head` row (`id=1`, `last_sequence=0`, `current_hash=64*'0'`) initialized, and all V2â€“V4 tables (`service_catalog`, `service_ticket`, `ticket_comment`, `approval_task`, `in_app_alert`) ready. |
