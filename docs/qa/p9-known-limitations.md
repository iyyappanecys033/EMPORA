# EPAMS P9 — Known Limitations, Blocked Verifications & P10 Prerequisites

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  

---

## 1. Environment-Blocked Verifications (Mandatory P10 Pre-Production Gates)

The following verifications could **NOT** be executed inside the offline sandboxed P9 environment. They are transparently classified as **`BLOCKED`** — never misrepresented as `VERIFIED` — and must be executed as entry gates in P10 staging/CI:

| ID | Verification Scope | Classification | Root Cause in P9 Environment | Mandatory P10 Action |
| :---: | :--- | :---: | :--- | :--- |
| **BLK-P9-01** | **Live MySQL 8.0+ Migration & Runtime Verification** | `BLOCKED` | Sandbox network isolation prevents TCP connections to host MySQL daemon (`localhost:3306`) or Docker daemon. | Execute Flyway V1–V4 + Hibernate `ddl-auto=validate` + `EXPLAIN` index plan checks against a live MySQL 8.0+ instance using the runbook in `docs/qa/p9-database-validation-report.md`. |
| **BLK-P9-02** | **Headless Chromium Browser E2E Execution (Playwright)** | `BLOCKED` | Offline sandbox prevents installing `@playwright/test` and downloading Chromium browser binaries from external registries. | Run `npm install -D @playwright/test && npx playwright install chromium && npx playwright test` against running backend + frontend using the authored suite in `springapp/springbackend/playwright.config.js` and `e2e/epams.spec.js`. *(Note: Live HTTP/cookie/CSRF/multi-persona E2E WAS verified via `P9LiveServerE2EIntegrationTests`.)* |
| **BLK-P9-03** | **Automated Dependency CVE Vulnerability Scanning** | `BLOCKED` | `OWASP Dependency-Check` and `npm audit` require live internet connectivity to NVD / npm advisory APIs. | Integrate `npm audit --audit-level=high` and Maven OWASP Dependency-Check / Trivy into P10 CI pipeline. |
| **BLK-P9-04** | **Line/Branch Code Coverage Tooling (JaCoCo)** | `NOT TESTED` | JaCoCo Maven plugin is not configured in `springapp/pom.xml` (no dependency/plugin changes made to `pom.xml` in P9). | Add JaCoCo plugin in P10 CI to publish automated line/branch coverage metrics without modifying runtime code. |

---

## 2. Documented Functional & Architectural Limitations (Intentionally Preserved)

| ID | Severity | Component | Description & Rationale |
| :---: | :---: | :--- | :--- |
| **FUNC-P9-01** | **RESOLVED (P9.1)** | `TicketStateMachine.java`, `BusinessCalendar.java` | **Remediated in P9.1:** Replaced wall-clock `resolvedAt.plusHours(48)` with `BusinessCalendar.calculateReopenDeadline(resolvedAt)` (48 accumulated business hours in `Asia/Kolkata`, Mon–Fri 09:00–18:00, excluding weekends & configured holidays; inclusive of exact deadline). Verified via 10 deterministic boundary tests (`BusinessCalendarReopenWindowTests`) and updated `TicketWorkflowTests.test5_ReopenWithinDisputeWindow`. See `docs/implementation/p9-1-business-calendar-correction.md`. |
| **TECH-P9-01** | **LOW** | `DashboardController.java` (`GET /api/dashboard/metrics`) | Legacy Phase 1 admin metrics endpoint loads all users and invokes `assessRisk(user.getId())` per user (N+1 pattern). Strictly gated to `ROLE_ADMIN`; does not affect operational ticket, notification, or P8 analytics paths. |
| **SEC-P9-ENV** | **CONFIG** | `AuthController.java` (`app.security.cookie.secure`) | `app.security.cookie.secure` defaults to `false` to support local HTTP development (`localhost`). Must be overridden to `true` behind HTTPS/TLS termination in P10 production deployment. |
| **DEFER-P9-01** | **SCOPE** | External Notification Channels & Scheduled SLA Scanner | SMTP email delivery, push/WebSocket notifications, and distributed scheduled SLA breach scanning remain `DEFERRED` per approved P8/P9 scope boundaries. |

---

## 3. Security Assessment Scope Clarification

P9 conducted deep internal source review, 7-role RBAC verification, BOLA/IDOR remediation (`SEC-P9-01`..`SEC-P9-06`), concurrency stress tests, and live-server HTTP cookie/CSRF testing. This does **not** substitute for an independent external third-party penetration test or formal SOC 2 / ISO 27001 certification.

