# EPAMS â€” Stage 2 Real MySQL 8+ Integration Final Completion Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration, Migration Verification & Database Reliability
**Baseline:** Unified Git Repository (`origin/main`), Java `25.0.2+10-LTS`, Spring Boot `3.5.16`, Hibernate `6.6.53.Final`, Flyway `11.7.2`, MySQL Community Server `8.0.45`
**Date:** 2026-11-18
**Final Verdict:** **`APPROVED â€” REAL MYSQL INTEGRATION VERIFIED.`**

---

## 1. Executive Summary & Acceptance Gate Sign-Off

Real MySQL `8.0.45` Community Server compatibility, fresh `V1`â€“`V4` migrations, brownfield data preservation, Hibernate `ddl-auto=validate`, least-privilege database role separation, concurrent audit-chain integrity, concurrent notification deduplication, `@Version` optimistic locking, transactional integrity, scheduled SLA/auto-closure jobs, and actual `EXPLAIN` / `EXPLAIN ANALYZE` query plans have all been **empirically verified against an isolated disposable real MySQL `8.0.45` server** (`127.0.0.1:13307`).

Zero Flyway `V1`â€“`V4` migrations and zero application business logic files were modified. Zero H2 test results were substituted as MySQL evidence.

| Stage 2 Acceptance Criterion | Status | Real MySQL `8.0.45` Verification Evidence |
| :--- | :---: | :--- |
| Actual MySQL 8+ execution verified (not H2) | **VERIFIED** | `Stage2RealMysqlIntegrationTests.test1` (`MySQL 8.0.45`, `jdbc:mysql://...`) + live packaged JAR runs (`task-4073`, `task-4081`) |
| Fresh `V1`â€“`V4` migrations succeed | **VERIFIED** | `epams_stage2_fresh`: 4/4 migrations `success=1` in `1.809 s`, 14 InnoDB/utf8mb4 tables, 13 FKs, 9 unique indexes, genesis `audit_chain_head` |
| Brownfield migration preserves existing data | **VERIFIED** | `epams_stage2_brownfield`: all 7 legacy IAM tables preserved byte-for-byte (BCrypt hashes, roles, profiles, refresh tokens, `NULL`-hash legacy audit rows, status history); `V2`â€“`V4` applied cleanly |
| Hibernate `ddl-auto=validate` succeeds | **VERIFIED** | Passed on real MySQL `8.0.45` with Hibernate `6.6.53.Final` across all 13 JPA entities (both `epams_migrator` and `epams_app` startups) |
| Audit-chain concurrency & tamper resistance correct | **VERIFIED** | 10-thread `SELECT ... FOR UPDATE` concurrency + rollback test + `verifyChain(valid=true)` + `ERROR 1142` denial on `UPDATE`/`DELETE` `audit_log` |
| Notification deduplication works under concurrency | **VERIFIED** | 8-thread race on `uk_alert_recipient_dedup` persisted exact `1` row with `0` leaked exceptions; multiple `NULL` keys allowed |
| Optimistic locking (`@Version`) correct | **VERIFIED** | Stale writes on both `ServiceTicket` and `ApprovalTask` rejected on MySQL + HTTP `409 Conflict` ProblemDetail mapping verified |
| Analytics queries verified using MySQL `EXPLAIN` / `EXPLAIN ANALYZE` | **VERIFIED** | 8 core queries analyzed on `1,003` tickets, `402` approvals, `500+` alerts (`DashboardBundle` `508 ms`; covering index scans & `0.532 ms` reverse inbox scan) |
| Database privileges appropriately restricted | **VERIFIED** | Separate `epams_migrator` vs. least-privilege `epams_app` (`0` global privileges; 6/6 negative `audit_log` + DDL checks returned MySQL `ERROR 1142`) |
| Full regression passes (MySQL + H2 + Frontend) | **VERIFIED** | Real MySQL `7/7` + H2 Backend `208/208` + Frontend Jest `34/34` + React Build green |
| Zero unresolved critical/high DB defects | **VERIFIED** | Zero critical/high defects; 2 non-blocking P10 optimization notes recorded below |

---

## 2. Stage G Full Regression Results (Strict Real MySQL vs. H2 Separation)

### 2.1 Real MySQL `8.0.45` Verification Results (`VERIFIED` â€” Zero H2)

| Verification Run | Exact Command / Scope | Exit Code | Counts / Result | Duration |
| :--- | :--- | :---: | :--- | :---: |
| **Fresh `V1`â€“`V4` Migration + Startup (`epams_migrator`)** | `java -jar springapp-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql` â†’ `GET /api/v1/health` | `0` | `V1`â€“`V4` applied (`1.809 s`), `ddl-auto=validate` pass, **HTTP `200` `{"status":"UP","database":"UP"}`**, `SECRET_LEAK_IN_LOG=False` | `12.61 s` |
| **Runtime Startup Under Least-Privilege `epams_app`** | `java -jar springapp-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql` (`DB_USERNAME=epams_app`) â†’ `GET /api/v1/health` | `0` | 4 migrations validated (`0.042 s`), 0 applied, `ddl-auto=validate` pass, **HTTP `200`**, `SECRET_LEAK_IN_LOG=False` | `9.09 s` |
| **Stage 2 Real MySQL Integration Suite** (`task-4105`) | `EPAMS_MYSQL_INTEGRATION=true mvn test -o -Dtest=Stage2RealMysqlIntegrationTests -f springapp/pom.xml` | `0` | **`7 / 7` passed** (`0` fail, `0` err, `0` skip) â€” Real MySQL check, Brownfield, Audit Concurrency, Dedup Concurrency, OptLock + 409, TX/SLA/Overlap, Analytics + `EXPLAIN ANALYZE` | `01:50 min` (`71.7 s` suite) |
| **Negative Privilege Enforcement (`epams_app`)** | `mysql.exe` 6 negative tests (`UPDATE`/`DELETE` `audit_log`, `DELETE` `audit_chain_head`, `CREATE`/`ALTER`/`DROP`) | `1` each | **6 / 6 denied** with `ERROR 1142 (42000)` | `~9 s` |

### 2.2 Separate H2 & Frontend Regression Results (Explicitly Separated from MySQL)

| Regression Run | Database / Runtime | Exact Command | Exit Code | Counts / Result | Duration |
| :--- | :--- | :--- | :---: | :--- | :---: |
| **Full Backend Suite (H2 Default)** (`task-4114`) | H2 `2.3.232` *(not MySQL)* | `mvn test -o "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml` | `0` | **208 passed, 0 failed, 0 errors** (`1` class skipped: `Stage2RealMysqlIntegrationTests` gated by `EPAMS_MYSQL_INTEGRATION=true`) | `04:28 min` |
| **Frontend Jest Suite** (`task-4115`) | Node `v22.23.2` / Jest | `Set-Location "springapp/springbackend"; npm test -- --watchAll=false` | `0` | **34 / 34 passed** across 3 suites | `57.48 s` |
| **Frontend Production Build** (`task-4115`) | React `18.2.0` | `Set-Location "springapp/springbackend"; npm run build` | `0` | **Compiled successfully** (`0` errors, `0` warnings) | `~10 s` |

---

## 3. Non-Blocking Observations for P10 Hardening (No Critical/High Defects)

1. **JDBC TLS Hardening (`P10`):** `application-mysql.properties` currently uses `useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true` for local/staging convenience. For P10 production deployment, externalize TLS parameters (`sslMode=VERIFY_IDENTITY`) and remove `allowPublicKeyRetrieval=true` and `createDatabaseIfNotExist=true`.
2. **`LOWER(t.department)` Analytics Filter (`P10` Optional Scale Optimization):** Real MySQL `EXPLAIN ANALYZE` (`Q2`) confirmed `2.14 ms` on `1,003` rows, but wrapping `t.department` in `LOWER()` prevents B-tree pushdown on `department` even though MySQL `utf8mb4` collation is case-insensitive. Consider removing `LOWER()` on `department` predicates when scaling beyond `100k+` tickets.
3. **Hibernate Informational Warning:** `HHH90000025: MySQLDialect does not need to be specified explicitly` â€” benign in Hibernate `6.6.53.Final`, optional cleanup in `application-mysql.properties`.
4. **Environment Note:** Verification ran against a native isolated disposable MySQL Community Server `8.0.45` instance (`mysqld.exe` on `127.0.0.1:13307`, cleanly shut down after completion) because Docker Desktop daemon was inactive on the host.

---

## 4. Stage 2 Deliverables Index

1. [`docs/mysql/stage2-initial-database-audit.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-initial-database-audit.md)
2. [`docs/mysql/stage2-mysql-environment-report.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-mysql-environment-report.md)
3. [`docs/mysql/stage2-fresh-migration-report.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-fresh-migration-report.md)
4. [`docs/mysql/stage2-brownfield-migration-report.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-brownfield-migration-report.md)
5. [`docs/mysql/stage2-concurrency-and-reliability-report.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-concurrency-and-reliability-report.md)
6. [`docs/mysql/stage2-security-and-permissions-report.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-security-and-permissions-report.md)
7. [`docs/mysql/stage2-final-completion-report.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/mysql/stage2-final-completion-report.md)

**Stop Boundary Confirmed:** Disposable MySQL `8.0.45` daemon cleanly terminated. P10 production deployment, cloud infrastructure, DNS, and TLS configuration have **not** been started.
