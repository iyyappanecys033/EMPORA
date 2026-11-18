# EPAMS P9 — Repository & Quality Assurance Baseline Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification (Phase A Reconnaissance)  
**Date:** 2026-11-17  
**Mode:** Strictly Read-Only Baseline Inspection (Zero Code Modifications)  
**Classification:** `[VERIFIED]`  

---

## 1. Verified Runtime & Technology Stack

| Component | Verified Value | Evidence Source |
| :--- | :--- | :--- |
| **Java Runtime** | OpenJDK 25.0.2 (`<java.version>25</java.version>`) | `springapp/pom.xml:23`, Maven execution logs |
| **Spring Boot** | `3.4.12` | `springapp/pom.xml:12` |
| **Persistence / ORM** | Spring Data JPA / Hibernate 6.6.x (`ddl-auto=validate` in MySQL & test profiles) | `application-mysql.properties:8`, `src/test/resources/application.properties:13` |
| **Database Migrations** | Flyway V1–V4 (`V1__init_iam_schema.sql`, `V2__service_catalog_and_tickets.sql`, `V3__approvals_and_audit_chain.sql`, `V4__notifications_and_analytics_indexes.sql`) | `springapp/src/main/resources/db/migration/` |
| **Frontend Runtime** | React `18.2.0`, React Router `6.11.1`, Axios `1.4.0`, CRA `5.0.1` | `springapp/springbackend/package.json` |
| **Browser E2E Tooling** | Playwright: `NOT INSTALLED`; Cypress: `NOT INSTALLED` (Chrome/Edge binaries present on host, offline sandbox blocks external npm registry install) | Host tool probe (`task-3385`) |
| **MySQL 8.0+ Daemon** | `mysql.exe` binary installed on host, but TCP network access blocked in sandboxed test environment | Host tool probe (`task-3385`) |

---

## 2. Reconciled P8 Baseline Execution Evidence

| Verification Suite | Command | Exit Code | Total Tests | Failures / Errors / Skipped | Notes |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Backend Maven Suite** | `mvn clean test "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml` | `0` | **177** (12 classes) | `0 / 0 / 0` | 152 P7 + 25 P8 tests (`03:40 min`) |
| **Frontend Jest Suite** | `cd springapp/springbackend; npm test -- --watchAll=false` | `0` | **28** (2 suites) | `0 / 0 / 0` | `App.test.js` (21) + `P8Features.test.js` (7) (`9.031 s`) |
| **Frontend Prod Build** | `cd springapp/springbackend; npm run build` | `0` | N/A | `0 warnings / 0 errors` | `main.js` 93.96 kB, `AnalyticsDashboard` 4.78 kB, `AuditLedger` 2.40 kB gzip |
| **Browser E2E Suite** | Not installed | N/A | **0** | N/A | `NOT TESTED` at P8 baseline |
| **Live MySQL 8.0+ Test** | Blocked by sandbox network boundary | N/A | **0** | N/A | `BLOCKED` at P8 baseline |

---

## 3. Initial Critical & High-Risk Gaps Identified During Phase A Source Inspection

Independent line-by-line inspection of all 16 backend controllers, security configuration, and workflow services revealed **6 genuine security and correctness gaps** missed by prior unit/component tests:

| Finding ID | Severity | Component & Location | Verified Finding |
| :---: | :---: | :--- | :--- |
| **SEC-P9-01** | **HIGH** | `AuditChainService.java:86` | `resetChainToGenesis()` is a public unrestricted method on a production `@Service` bean capable of deleting chained audit rows in production. |
| **SEC-P9-02** | **HIGH** | `TicketCommandService.java:305` | `addComment()` validates `canViewTicket()`, but does **not** restrict `isInternal = true` to Admins/Department Resolvers—any requester can submit an internal specialist note. |
| **SEC-P9-03** | **CRITICAL** | `ProfileController.java:39-113`, `UserAccountController.java:67-77`, `UserRoleMappingController.java:32-79`, `AccountStatusLogController.java:13-47`, `RoleController.java:33-64` | Multiple legacy Phase 1 IAM endpoints lack `@PreAuthorize`: any authenticated employee can read all `Profile` entities (exposing `salary`, `dob`, `address`, `emergencyContact`), enumerate all `UserAccount` records, view role mappings, and perform full CRUD on `/api/accountStatusLogs`. |
| **SEC-P9-04** | **HIGH** | `ApprovalController.java:32-36` & `ApprovalService.java:237` | `GET /api/v1/approvals/tickets/{ticketId}` performs zero authorization or ownership verification (BOLA/IDOR): any authenticated employee can read approval history and decision reasons for another employee's ticket. |
| **SEC-P9-05** | **MEDIUM** | `ProfileController.java:89`, `RoleController.java:61` | Unbounded pagination `size` parameter allows requesting arbitrarily large page sizes (DoS / resource consumption risk). |
| **FUNC-P9-01** | **LOW** | `TicketStateMachine.java:80` & `BusinessCalendar.java:156` | Dispute/reopen window uses `resolvedAt.plusHours(48)` (48 wall-clock hours) rather than 48 business hours. Retained intentionally to preserve existing regression contract (`TicketWorkflowTests.test5`), documented transparently. |

---

## 4. Environment & Static Analysis Notes

- **Coverage Tools:** JaCoCo is not configured in `springapp/pom.xml`; coverage percentages will not be fabricated.
- **Dependency Scanners:** OWASP Dependency-Check / `npm audit` require external network vulnerability databases unavailable in offline sandbox mode.
- **Phase N Remediation Scope:** All confirmed `CRITICAL`, `HIGH`, and `MEDIUM` security findings (`SEC-P9-01` through `SEC-P9-05`) are strictly in-scope for P9 remediation with full regression tests.

