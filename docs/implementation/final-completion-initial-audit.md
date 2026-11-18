# EPAMS — Final P7 + P8 + P9 Completion Initial Read-Only Audit

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** Section 4 — Initial Read-Only Audit (Pre-Stage 1 Changes)  
**Date:** 2026-11-18  

---

## 1. Repository & Git State

- **Git Repository Check:** `git status --short` / `git branch --show-current` → `fatal: not a git repository (or any of the parent directories): .git` (workspace is a standalone directory tree, not Git-initialized).
- **Verified Runtime & Dependency Versions (`spring-boot-dependencies:3.4.12`):**
  - **Java Runtime / Compiler Target:** OpenJDK `25.0.2` (`<java.version>25</java.version>`)
  - **Spring Boot:** `3.4.12`
  - **Spring Framework:** `6.2.14`
  - **Spring Security:** `6.4.13`
  - **Hibernate ORM:** `6.6.36.Final`
  - **Flyway:** `10.20.1`
  - **Byte Buddy:** `1.15.11` *(predates official JDK 25 class-file v69 support — key `COMPAT-01` finding)*
  - **Tomcat:** `10.1.49`, **JUnit Jupiter:** `5.11.4`, **Mockito:** `5.14.2`, **H2:** `2.3.232`, **JJWT:** `0.11.5`
  - **Frontend:** React `18.2.0`, React Router `6.11.1`, Axios `1.4.0`, `react-scripts 5.0.1`

---

## 2. Verified Current Test Baseline (Pre-Stage 1 Edits)

Directly verified from the latest clean executions (`task-3788`, `task-3745`, `task-3758`):
- **Backend Maven Clean Suite:** **205 / 205 passing** across **17 Surefire XML reports** (`0` failures, `0` errors, `0` skipped, `02:15 min`, exit code `0`).
- **Frontend Jest Suite:** **34 / 34 passing** across **3 suites** (`0` failures, `0` skipped, `30.36 s`, exit code `0`).
- **Frontend Production Build:** **Compiled successfully** (`0` errors, `0` ESLint warnings; `main.js` `93.97 kB`, `AnalyticsDashboard` `4.78 kB`, `AuditLedger` `2.40 kB` gzip).

---

## 3. Environment & External Tool Availability Audit

| Tool / Infrastructure | Direct Probe Result (`2026-11-18`) | Stage Gate Impact |
| :--- | :--- | :--- |
| **MySQL 8.0 (`127.0.0.1:3306`)** | `mysql.exe 8.0.45` installed; port `3306` reachable; rejects unauthenticated `root` and default `rootpassword` (`ERROR 1045 (28000)`). No test credentials supplied. | Stage 2 MySQL `BLOCKED` |
| **Docker Engine** | `docker.exe` installed; daemon stopped (`open //./pipe/docker_engine: The system cannot find the file specified`). | Blocks Testcontainers / Docker Mailpit / Docker MySQL |
| **Playwright (`@playwright/test`)** | Not installed in `node_modules` or global PATH; offline sandbox blocks npm registry downloads. | Stage 1 §5.1 Chromium E2E `BLOCKED` |
| **JaCoCo (`jacoco-maven-plugin`)** | Absent in local `.m2` repository (`$env:USERPROFILE\.m2\repository\org\jacoco` → `False`); offline sandbox cannot download Maven plugins. | Stage 1 §5.3 JaCoCo `BLOCKED` |
| **Mailpit / Trivy / OWASP Dep-Check** | None installed on host PATH; offline sandbox blocks advisory database/binary downloads. | Stage 1 §5.2 CVE Scan `BLOCKED`; §5.4 real external Mailpit daemon `BLOCKED` (in-process RFC 5321 TCP SMTP harness available) |

---

## 4. Actionable Code Gaps Identified for Immediate Stage 1 Remediation

1. **`TECH-P8-01` (MEDIUM — Legacy `DashboardController` N+1):** `GET /api/dashboard/metrics` (actively consumed by `Dashboard.js` at `/admin/metrics`) loads `userAccountRepo.findAll()` and invokes `riskAssessmentService.assessRisk(user.getId())` in a loop — executing **$3N + 2$ SQL queries** per load. Can be eliminated to **3 constant SQL queries** ($O(1)$).
2. **`GAP-CAL-2027` (MEDIUM — Holiday Calendar Beyond 2026):** `BusinessCalendar` hardcodes only 4 holidays for `2026` with no externalized property configuration (`epams.calendar.holidays`) or removal API for multi-year maintenance.
3. **`GAP-SMTP-HARDEN` (MEDIUM — SMTP Adapter Enterprise Readiness):** `SmtpEmailDeliveryAdapter` lacks optional `AUTH PLAIN` authentication (for authenticated SMTP relays), UTF-8 MIME Base64 subject encoding for Unicode, and independent email-level idempotency tracking.
4. **`COMPAT-01` (LOW/INFO — Runtime Compatibility Decision):** Needs formal component-by-component compatibility matrix documenting Byte Buddy `1.15.11` / Spring Boot `3.4.12` vs Java `25.0.2` and controlled migration criteria.
