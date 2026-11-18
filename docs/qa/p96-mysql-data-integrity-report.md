# EPAMS — P9.6 Real MySQL 8 Data Integrity & Migration Report (Stage K)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification (Stage K)  
**Verification Date:** 2026-11-18 (`13:08:25` – `13:10:59 +05:30`)  
**Database Engine Verified:** Real isolated **`MySQL 8.0.45`** (`mysqld.exe Ver 8.0.45 for Win64 on x86_64`, `127.0.0.1:33308`, `utf8mb4` / `utf8mb4_unicode_ci`, InnoDB)  
**Flyway Migrations (`V1`–`V4`):** **ZERO MODIFICATIONS** (`git diff -- springapp/src/main/resources/db/migration/` = empty)  
**Verification Verdict:** **`PASSED (7 / 7 Real MySQL 8 Tests + 5 / 5 SQL Invariants + 14 / 14 Backup Restore Verified)`**

---

## 1. Real MySQL `8.0.45` Execution Environment & Role Separation

H2 was **never** substituted for this verification stage. An isolated ephemeral `mysqld.exe 8.0.45` instance was initialized from scratch (`--initialize-insecure`) with strict least-privilege separation mirroring production governance:

| Account | Scope & Privileges | Purpose Verified |
| :--- | :--- | :--- |
| `'epams_migrator'@'127.0.0.1'` | `SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER` on `epams_p96_mysql.*` and `epams_stage2_brownfield.*` | Booted packaged JAR (`springapp-0.0.1-SNAPSHOT.jar`, `--spring.profiles.active=mysql`) to apply Flyway `V1`–`V4` (`migratorBootHealthy: true`, `/api/v1/health == UP`), and executed brownfield `V1` baseline + `V2`–`V4` upgrade |
| `'epams_app'@'127.0.0.1'` | `SELECT, INSERT, UPDATE, DELETE` on 11 operational tables; **`SELECT, INSERT` ONLY on `audit_log`**; **`SELECT, UPDATE` ONLY on `audit_chain_head`**; **`SELECT` ONLY on `flyway_schema_history`**; **zero DDL** | Executed full `Stage2RealMysqlIntegrationTests` (`7 / 7` passed, `0` failures, `0` errors, `0` skipped, `79.313 s`) |

---

## 2. Real MySQL 8 Integration Suite Results (`Stage2RealMysqlIntegrationTests`)

Executed post-P9.6 remediations via `mvn test -o -Dskip.npm=true -Dtest=Stage2RealMysqlIntegrationTests` with `EPAMS_MYSQL_INTEGRATION=true`:

| Order | Test Method | Verification Scope | Result |
| :---: | :--- | :--- | :---: |
| `1` | `test1_VerifyActualMysql8ConnectionNotH2` | Confirms JDBC driver = `jdbc:mysql://`, engine = `MySQL 8.0.45`, never H2 | **PASS** |
| `2` | `test2_StageD_BrownfieldRealMysqlDataPreservationAndUpgrade` | Seeds all 7 legacy IAM tables (`user_account`, `role`, `user_role_mapping`, `profile`, `refresh_token`, `audit_log`, `account_status_log`), runs Flyway `baseline(1)` + `migrate()` (`V2`–`V4`), asserts byte-for-byte preservation, `NULL` cryptographic columns on legacy audit rows, `14` InnoDB tables, `13` FKs, and genesis `audit_chain_head` | **PASS** |
| `3` | `test3_StageE1_AuditChainConcurrencyRollbackAndPermissionEnforcement` | 10-thread concurrent InnoDB `FOR UPDATE` audit chain writes, forced rollback (zero sequence gap), SHA-256 `verifyChain() == true`, and negative permission enforcement (`UPDATE`/`DELETE` on `audit_log` & `DELETE` on `audit_chain_head` denied with `DataAccessException` for `epams_app`) | **PASS** |
| `4` | `test4_StageE2_NotificationDeduplicationUnderRealMysqlConcurrency` | 8-thread race on `uk_alert_recipient_dedup` persists exact `1` row with `0` leaked exceptions; multiple `NULL` dedup keys allowed | **PASS** |
| `5` | `test5_StageE3_OptimisticLockingAndHttp409MappingOnRealMysql` | Stale `@Version` writes rejected on `ServiceTicket` & `ApprovalTask` (`ObjectOptimisticLockingFailureException`), mapped via `GlobalExceptionHandler` to RFC 7807 HTTP `409 Conflict` | **PASS** |
| `6` | `test6_StageE4_TransactionIntegrityAfterCommitAndSlaJobsOnRealMysql` | Ticket & approval audit atomicity, `AFTER_COMMIT` notification suppression on rollback, idempotent SLA auto-closure & 2-thread overlapping SLA breach alerts | **PASS** |
| `7` | `test7_StageE5_AnalyticsAndRealMysqlExplainAnalyzeOnSyntheticDataset` | Seeds `1,000` synthetic tickets + `400` approvals + `500` alerts; executes `AnalyticsService.getDashboardBundle(...)` (`422 ms`, `totalInPeriod=1003`, `slaRate=33.23%`) and runs `EXPLAIN` + `EXPLAIN ANALYZE` across `Q1`–`Q8` | **PASS** |
| **Total** | **7 Real MySQL 8 Tests** | **`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`** | **`BUILD SUCCESS`** |

---

## 3. SQL Data Integrity & Invariant Audit (`epams_p96_mysql`)

Executed directly via `mysql.exe` after full integration & synthetic seeding:

| SQL Invariant Check | Expected | Observed | Status |
| :--- | :---: | :---: | :---: |
| `orphan_tickets` (`service_ticket.requester_id` missing in `user_account`) | `0` | **`0`** | **PASS** |
| `orphan_comments` (`ticket_comment.ticket_id` missing in `service_ticket`) | `0` | **`0`** | **PASS** |
| `orphan_approvals` (`approval_task.ticket_id` missing in `service_ticket`) | `0` | **`0`** | **PASS** |
| `dup_alert_dedup_keys` (duplicate non-null `(recipient_id, dedup_key)`) | `0` | **`0`** | **PASS** |
| `flyway_migrations_applied` (`flyway_schema_history WHERE success = 1`) | `4` | **`4`** | **PASS** |

---

## 4. Real MySQL 8 `EXPLAIN` / `EXPLAIN ANALYZE` Query Plan Summary

| Query / Repository Access Pattern | Index Used (`key`) | Access Type | `EXPLAIN ANALYZE` Actual Time | Assessment |
| :--- | :--- | :---: | :---: | :--- |
| `service_ticket` dept + status queue | `idx_ticket_dept_status` | `ref` (`const,const`) | `< 1 ms` (`34` rows) | Optimal composite index lookup |
| `in_app_alert` unread inbox (`Q8`, `LIMIT 20`) | `idx_alert_recipient_read` | `ref` / `Backward index scan` | **`0.349 ms`** | Optimal reverse index scan, zero filesort |
| `approval_task` approver pending queue | `idx_approval_approver_status` | `ref` (`const,const`) | `< 1 ms` (`135` rows) | Optimal composite index lookup |
| `Q4_CountOpenBacklogByDept` | `idx_ticket_dept_status` | Covering index scan | **`1.69 ms`** | Optimal covering index |
| `Q5_CountOverdueOpenByDept` | `idx_ticket_dept_status` | Index scan | **`5.13 ms`** | Fast under `1,003` rows |
| `Q6_ServicePopularityJoin` | `idx_ticket_catalog_created` | Covering index nested loop | **`6.08 ms`** | Fast covering index join |
| `Q7_ApprovalOverduePendingJoin` | `PRIMARY` on `service_ticket` | `eq_ref` covering join | **`1.37 ms`** | Fast (`402` rows); `idx_approval_status_created` available at scale |
| `Q2_AnalyticsProjection_LowerDeptFilter` | `NULL` (`LOWER(department)`) | Table scan (`1,003` rows) | **`1.56 ms`** | Acceptable now; P10 tuning tip: drop `LOWER()` since `utf8mb4_0900_ai_ci` is already case-insensitive |

---

## 5. Isolated Backup & Restore Verification (`mysqldump`)

- **Backup Command:** `mysqldump --no-tablespaces --single-transaction --routines --triggers epams_p96_mysql`
- **Backup Artifact Size:** **`301,203` bytes** (clean logical dump including `1,003` tickets, `400+` approvals, `500+` alerts, and `flyway_schema_history`)
- **Restore Target:** Fresh schema `epams_p96_restore` on MySQL `8.0.45`
- **Table Parity Verification:** `origTables = 14`, `restoredTables = 14`, **`restoreVerified = true`**
