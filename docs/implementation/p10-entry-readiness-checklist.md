# EPAMS — P10 Entry Readiness Checklist

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Target Next Phase:** P10 — Production Packaging, CI/CD, Deployment & Operations  
**Date:** 2026-11-18  

---

## 1. Mandatory Pre-Production Gates (Must Pass at Start of P10)

- [ ] **Gate 1 — Live MySQL 8.0+ Verification (`BLK-01`):** Provision disposable MySQL 8.0+ DB (Docker or credentialed host), run Flyway `V1`–`V4` fresh + brownfield V1 adoption + Hibernate `ddl-auto=validate` + `EXPLAIN` index verification on `in_app_alert`, `service_ticket`, and `approval_task`.
- [ ] **Gate 2 — Headless Chromium Browser E2E (`BLK-02`):** Install `@playwright/test` + Chromium and execute `springapp/springbackend/e2e/epams.spec.js` against running backend + frontend.
- [ ] **Gate 3 — Online Vulnerability Scan (`BLK-03`):** Run `npm audit --audit-level=high` and Maven OWASP Dependency-Check / Trivy in online CI; remediate any confirmed High/Critical CVEs.
- [ ] **Gate 4 — JaCoCo Coverage Baseline (`BLK-04`):** Enable `jacoco-maven-plugin:0.8.13` in CI Maven profile and archive line/branch coverage report.
- [ ] **Gate 5 — Production Secrets & TLS Configuration:** Provide externalized `JWT_SECRET` (256-bit+), `DB_USERNAME`, `DB_PASSWORD`, `COOKIE_SECURE=true` behind HTTPS/TLS termination, and Mailpit/SMTP env vars (`EPAMS_EMAIL_ENABLED`, `EPAMS_EMAIL_HOST`, `EPAMS_EMAIL_PORT`, `EPAMS_EMAIL_FROM`).

## 2. Accepted Limitations & Post-MVP Items (Non-Blocking for P10 Entry)

- **Java 25 + Spring Boot 3.4.12 (`COMPAT-01`):** Plan Spring Boot 3.5.x / 4.x alignment when officially certifying Java 25 LTS in production.
- **Multi-Year Corporate Holiday Administration:** Seeded 2026 holidays in `BusinessCalendar` (extensible via `addHoliday`); externalized DB/config holiday table can be added in a future release.
- **Legacy `DashboardController` N+1 (`TECH-P8-01`):** Admin-only Phase 1 endpoint isolated from operational workloads.
- **Hardware WORM Audit Storage:** Deferred per `ADR-011` (current implementation is an append-only cryptographically hash-chained audit trail; grant MySQL `epams_app` `SELECT, INSERT` only on `audit_log` in P10).
