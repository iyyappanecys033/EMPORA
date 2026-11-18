# EPAMS — P7 + P8 + P9 Final Consolidated Repository Audit (Phase A)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** Consolidated P7 + P8 + P9 Final Completion — Phase A Read-Only Reconnaissance  
**Date:** 2026-11-18  
**Mode:** Strictly Read-Only Inspection Prior to Code Remediation  

---

## 1. Verified Runtime & Dependency Inventory (Source-of-Truth BOM)

Verified directly from `springapp/pom.xml`, `spring-boot-dependencies-3.4.12.pom`, and `springapp/springbackend/package.json`:

| Component | Verified Exact Version | Notes & Compatibility Evaluation |
| :--- | :--- | :--- |
| **Java Runtime & Target** | OpenJDK `25.0.2` (`<java.version>25</java.version>`) | Compiles & executes all tests locally with `javac --release 25`. |
| **Spring Boot** | `3.4.12` | **Caveat (§2.1):** Spring Boot 3.4.x officially lists Java 17–23 support; Java 25 runs cleanly locally without runtime errors, but official Java 25 certification requires a planned future migration to Spring Boot 3.5.x / 4.x (intentionally NOT performed as unrelated cleanup in P7–P9). |
| **Spring Security** | `6.4.13` | Stateless JWT cookies + `CookieCsrfTokenRepository` + `SpaCsrfTokenRequestHandler` + `NullAuthenticatedSessionStrategy`. |
| **Hibernate ORM / JPA** | `6.6.36.Final` | `ddl-auto=validate` enforced in both `application-mysql.properties` and `src/test/resources/application.properties`. |
| **Flyway** | `10.20.1` | Migrations `V1`–`V4` preserved unmodified (`flyway-core` + `flyway-mysql`). |
| **H2 Test Engine** | `2.3.232` (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`) | Used by automated test suite; does NOT replace live MySQL 8.0+ verification. |
| **MySQL Host Client** | `mysql.exe` `8.0.45` (listening on `127.0.0.1:3306`) | Rejects unauthenticated `root` and default `rootpassword` (`ERROR 1045 (28000)`); no test DB credentials supplied in environment. |
| **Docker Engine** | `docker.exe` installed; daemon NOT running | `open //./pipe/docker_engine: The system cannot find the file specified`. |
| **Frontend Runtime** | React `18.2.0`, React Router `6.11.1`, Axios `1.4.0`, `react-scripts` `5.0.1` | Clean 0-warning production bundle with `React.lazy` code splitting. |
| **Offline Tool Cache Check** | `jacoco-maven-plugin`: absent in `.m2`; `spring-boot-starter-mail`: absent in `.m2`; `@playwright/test`: absent in `node_modules` | Offline sandbox prevents downloading external plugins/registries. |

---

## 2. Verified Pre-Remediation Execution Baseline (P9.1 Freeze)

Executed immediately prior to consolidated P7–P9 gap remediation (`task-3656`, `task-3657`, `task-3665`):

| Verification Suite | Exact Command | Exit Code | Total Tests | Failures / Errors / Skipped | Runtime / Output |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Backend Maven Suite** | `mvn clean test "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml` | `0` | **200** (16 classes) | `0 / 0 / 0` | `01:56 min`, `BUILD SUCCESS` |
| **Frontend Jest Suite** | `Set-Location "springapp/springbackend"; npm test -- --watchAll=false` | `0` | **33** (3 suites) | `0 / 0 / 0` | `22.7 s` (`App.test.js` 21, `P8Features.test.js` 7, `P9FrontendQa.test.js` 5) |
| **Frontend Prod Build** | `Set-Location "springapp/springbackend"; npm run build` | `0` | N/A | `0 errors / 0 warnings` | `main.js` `93.96 kB`, `AnalyticsDashboard` `4.78 kB`, `AuditLedger` `2.40 kB` gzip |

*(Note: Historical phase counts — P7 152, P8 177, P9 190, P9.1 200 — are cumulative milestones of the same suite, never summed together.)*

---

## 3. Verified Database Schema & Table Mapping

Verified from `V1`–`V4` SQL migrations and JPA entities:
- `V1__init_iam_schema.sql`: `user_account`, `role`, `profile`, `user_role_mapping`, `account_status_log`, `audit_log`, `refresh_token`.
- `V2__service_catalog_and_tickets.sql`: `service_catalog` (mapped by `ServiceCatalogItem`), `service_ticket` (`@Version`), `ticket_comment`.
- `V3__approvals_and_audit_chain.sql`: `approval_task`, `audit_chain_head`, `in_app_alert`, and hash-chain columns on `audit_log`.
- `V4__notifications_and_analytics_indexes.sql`: `in_app_alert` extensions (`read_at`, `related_entity_type`, `related_entity_id`, `dedup_key`), unique constraint `uk_alert_recipient_dedup(recipient_id, dedup_key)`, and 4 composite analytics indexes (`idx_ticket_created_dept_status`, `idx_ticket_priority_dept`, `idx_ticket_catalog_created`, `idx_approval_status_created`).

---

## 4. Deep Source-Code Findings Identified in Phase A

1. **Missing Scheduled SLA & Auto-Closure Processor (`GAP-P8-01`, HIGH):** `HLD.md` §5.3 Clock 3 and `FR-021` require hourly automatic closure of `RESOLVED` tickets strictly past 48 business hours (`SYSTEM_AUTO_CLOSURE`) plus SLA breach/warning scanning. Neither `@EnableScheduling` nor any `@Scheduled` SLA processor exists in `springapp/src/main/java`.
2. **Missing Provider-Neutral Email Delivery Adapter (`GAP-P8-02`, HIGH):** `NFR-013` specifies outbound notification email delivery testable against local Mailpit/SMTP without hardcoded credentials and isolated from in-app persistence failures. No email delivery adapter exists yet.
3. **Frontend Preselected Catalog Department Drift (`GAP-P7-01`, MEDIUM):** `CreateTicketModal.js` lines 9 & 18 read `preselectedItem?.targetDepartment || 'IT'`, ignoring backend `ServiceCatalogItemDTO.department`, causing non-IT catalog requests (HR/Facilities) to default to `'IT'` until manually re-selected.
4. **Production MySQL Profile Cookie & Runtime Defaults (`GAP-P8-03`, MEDIUM):** `application-mysql.properties` lacked explicit `app.security.cookie.secure=${COOKIE_SECURE:true}`, `app.security.cookie.same-site=${COOKIE_SAME_SITE:Lax}`, `epams.scheduling.enabled`, and `epams.email.*` environment-driven settings.
5. **Missing Explicit ADR for Three Independent Clocks Relationship (`GAP-P9-01`, LOW):** ADR-001..ADR-015 did not formally codify the inclusive/exclusive relationship between Clock 1 (Resolution SLA), Clock 2 (48-business-hour inclusive Dispute/Reopen Window), and Clock 3 (Auto-Closure strictly `now > deadline`).
6. **Environment-Blocked Verifications (`BLK-01`..`BLK-04`):** Live MySQL 8.0+ migration test (host MySQL requires password not present in env; Docker daemon stopped), real Chromium Playwright execution (`@playwright/test` unavailable offline), JaCoCo plugin (not cached in `.m2`), and online CVE database scanning (offline sandbox).
