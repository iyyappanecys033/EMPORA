# EPAMS — P7 + P8 + P9 Final Consolidated Gap Register (Post-Remediation)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Date:** 2026-11-18  

---

## Consolidated Findings, Dispositions & Verification Evidence

| Gap ID | Phase | Severity | Area | Finding & Remediation | Verification Evidence | Final Disposition |
| :---: | :---: | :---: | :--- | :--- | :--- | :---: |
| **GAP-P7-01** | P7 | **MEDIUM** | Frontend (`CreateTicketModal.js`) | `preselectedItem?.targetDepartment \|\| 'IT'` ignored backend `department` field on preselected HR/Facilities catalog items. Fixed to prefer `preselectedItem?.department`. | `P9FrontendQa.test.js` (`P9-FE-06`), `npm run build` (0 warnings) | **`COMPLETED` (`FIXED`)** |
| **GAP-P8-01** | P8 | **HIGH** | Scheduled SLA & Auto-Closure | Implemented `SchedulingConfig` + `SlaScheduledProcessor` (`processAutoClosures` using `BusinessCalendar` + `TicketStateMachine` + `SYSTEM_AUTO_CLOSURE` audit + deduped alert; `processSlaAlerts` with `LEFT JOIN FETCH t.assignee` and `ON_HOLD` exclusion). | `P7P9ConsolidatedAcceptanceTests.scenario4` (205/205 Maven green) | **`COMPLETED` (`FIXED`)** |
| **GAP-P8-02** | P8 | **HIGH** | External Email Delivery (`NFR-013`) | Implemented provider-neutral `EmailDeliveryAdapter` + zero-dependency RFC 5321 `SmtpEmailDeliveryAdapter` (Mailpit-compatible, no hardcoded secrets, CRLF sanitization, duplicate-safe, isolated from in-app failures). | `P7P9ConsolidatedAcceptanceTests.scenario5` (real localhost TCP SMTP `ServerSocket` + outage test) | **`COMPLETED` (`FIXED`)** |
| **GAP-P8-03** | P8/P9 | **MEDIUM** | Production Cookie & Env Config | Added explicit `app.security.cookie.secure=${COOKIE_SECURE:true}`, `same-site`, `epams.scheduling.*`, and `epams.email.*` env defaults in `application-mysql.properties`. | Inspected `application-mysql.properties`; test profile isolated | **`COMPLETED` (`FIXED`)** |
| **GAP-P9-01** | P9 | **LOW** | Lifecycle Clock ADR | Formally documented Clock 1 (SLA), Clock 2 (inclusive 48-business-hour reopen), and Clock 3 (exclusive auto-closure) in `ADR-016`. | `docs/architecture/architecture-decision-records.md` (`ADR-016`) | **`COMPLETED` (`FIXED`)** |
| **GAP-INT-01** | P7–P9 | **MEDIUM** | Cross-Phase Acceptance Suite | Added unified `P7P9ConsolidatedAcceptanceTests.java` covering all 6 cross-phase scenarios (Section 7). | `P7P9ConsolidatedAcceptanceTests` (5/5 methods covering Scenarios 1–6) | **`COMPLETED` (`FIXED`)** |
| **FUNC-P9-01** | P9.1 | **LOW** | 48-Business-Hour Reopen Window | Replaced wall-clock `plusHours(48)` with exact `BusinessCalendar.calculateReopenDeadline` (`Asia/Kolkata`, Mon–Fri `09:00–18:00`, weekends & holidays excluded, `Duration` precision, inclusive deadline). | `BusinessCalendarReopenWindowTests` (10/10) + `TicketWorkflowTests.test5` | **`COMPLETED` (`FIXED`)** |
| **SEC-P9-01..06** | P9 | **CRITICAL/HIGH** | Security & Live SPA CSRF | Fixed audit chain reset guard, internal comment spoofing, legacy IAM RBAC/PII redaction, approval BOLA, pagination bounds, and Spring Security 6 stateless SPA CSRF (`NullAuthenticatedSessionStrategy`). | `P9SecurityAndWorkflowQaTests` (10/10) + `P9LiveServerE2EIntegrationTests` (2/2) | **`COMPLETED` (`FIXED`)** |
| **BLK-01** | P9 | **HIGH** | Real MySQL 8.0+ Validation | Host MySQL `8.0.45` (`127.0.0.1:3306`) rejects unauthenticated/default credentials (`ERROR 1045`); Docker Desktop daemon stopped (`//./pipe/docker_engine` unavailable). | Read-only probe logged (`p9-final-database-validation-report.md`) | **`BLOCKED`** (P10 Gate 1) |
| **BLK-02** | P9 | **HIGH** | Real Browser E2E (Playwright) | `@playwright/test` & Chromium unavailable in offline sandbox; suite authored (`playwright.config.js`, `e2e/epams.spec.js`), live HTTP E2E verified via Tomcat `RANDOM_PORT`. | `p9-final-e2e-report.md` | **`BLOCKED`** (P10 Gate 2) |
| **BLK-03** | P9 | **MEDIUM** | Online CVE Dependency Scan | Offline sandbox cannot reach NVD or npm registry advisory endpoints; offline BOM review completed. | `p9-final-security-assessment.md` | **`BLOCKED`** (P10 Gate 3) |
| **BLK-04** | P9 | **LOW** | JaCoCo Code Coverage Plugin | `org.jacoco:jacoco-maven-plugin` absent in local `.m2`; cannot fetch offline. | `p9-final-coverage-report.md` | **`BLOCKED`** (P10 Gate 4) |
| **TECH-P8-01** | P8 | **LOW** | Legacy `DashboardController` N+1 | Phase 1 `GET /api/dashboard/metrics` (admin-only) retains per-user risk loop; isolated from operational paths. | `p8-performance-final-report.md` | **`ACCEPTED RISK`** |
| **COMPAT-01** | P7–P9 | **INFO** | Java 25 + Spring Boot 3.4.12 | Spring Boot 3.4.x officially certifies Java 17–23; builds/passes 205/205 locally; Spring Boot 3.5.x/4.x upgrade planned separately. | `p7-p9-final-repository-audit.md` | **`ACCEPTED RISK`** |
