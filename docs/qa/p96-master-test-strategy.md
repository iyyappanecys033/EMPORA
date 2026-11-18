# EPAMS — P9.6 Master Test Strategy

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage B: Master Test Strategy
**Baseline Commit:** `a4857679c0bde310e4b705a46d7e7ef701bc3901`

---

## 1. Quality Philosophy & Objectives

The objective of Phase P9.6 is not merely to increase test counts, but to act as an adversarial, independent enterprise QA organization attempting to expose functional, validation, authorization, concurrency, persistence, usability, performance, and reliability failures prior to P10 production deployment engineering.

**Core Principles:**
- **Zero Fabrication:** Every pass, fail, skip, coverage figure, latency metric, and screenshot must come from real execution. Skipped or blocked checks are never counted as passed.
- **Defense-in-Depth Verification:** Frontend guards and HTML constraints are treated strictly as UX conveniences; every business and security rule must be verified at the server API and persistence layers.
- **Deterministic Time & Isolation:** All time-sensitive SLA, reopen-window, auto-close, and lockout logic is tested via deterministic `Clock` injection and explicit timestamps. All databases (H2 and isolated disposable MySQL 8.0) use synthetic test identities only.
- **Minimal Safe Remediation:** Verified defects follow Reproduce → Root Cause → Minimal Fix → Regression Test → Full Retest, with zero Flyway `V1`–`V4` modifications.

---

## 2. Test Pyramid & Layer Responsibilities

| Layer | Technology & Harness | Target Scope | Execution Gate |
| :--- | :--- | :--- | :--- |
| **L1 — Deterministic Unit** | JUnit 5, Mockito, fixed `Clock` | `TicketStateMachine` (all 121 state pairs), `BusinessCalendar`, `ApprovalService`, `AuditChainService`, `AnalyticsService`, `NotificationService`, `RiskAssessmentService`, `AuthService` | Every build (`mvn test`) |
| **L2 — Component & Transaction Integration** | `@SpringBootTest`, H2 MySQL mode, `TransactionTemplate`, `ExecutorService` | Rollback vs. `AFTER_COMMIT` notifications, `REQUIRES_NEW` audit preservation, optimistic locking (`@Version`), concurrent audit chain locking, `SlaScheduledProcessor` idempotency, failure isolation | Every build (`mvn test`) |
| **L3 — Exhaustive API, Validation & Security** | `MockMvc` + Spring Security (`csrf()`, cookies, Bearer JWT) | All 86 REST endpoints, 44 DTO fields + query/path params, RFC 7807 error contracts, 11-role RBAC matrix, field-level access, mass assignment, IDOR/BOLA, XSS/SQLi/415/malformed JSON | Every build (`mvn test`) |
| **L4 — Real MySQL 8 Database Reliability** | Disposable isolated MySQL `8.0.45` (`EPAMS_MYSQL_INTEGRATION=true`) + `mysql`/`mysqldump` | Fresh & brownfield Flyway `V1`–`V4`, Hibernate `validate`, `EXPLAIN` plan inspection, concurrent writes/locking, data invariants, backup & restore | Opt-in CI / Pre-release gate |
| **L5 — Frontend Component & Interaction** | Jest + React Testing Library + `--coverage` | All redesigned P9.5 components, forms, modals, keyboard/Escape, RBAC route guards, toast/error recovery, double-submit prevention | Every build (`npm test`) |
| **L6 — Live Browser E2E, Visual & Accessibility** | Google Chrome `157.0.8103.79` (CDP, isolated profile, full security) + `axe-core 4.11.4` | 12 multi-role business journeys, 6 viewports (`1440×900`–`320×700`), Light/Dark/System themes, zoom/reflow/reduced-motion/large-font | Pre-release / CI browser gate |
| **L7 — Security, Performance & Reliability** | `npm audit`, static checks, Node HTTP load generator, fault simulation | OWASP ASVS / API Top 10, dependency triage, baseline/load/stress/spike/soak latency (`p50`/`p95`/`p99`), backend restart, SMTP/DB outage recovery | Pre-deployment gate |

---

## 3. Entry, Suspension & Exit Criteria

- **Entry Criteria:** Clean `main` baseline (`a4857679`), all P9.5 + P9.5.1 artifacts merged, local Java 25, Maven, Node 22, Chrome 157, and MySQL 8.0 verified.
- **Defect Severity Policy:**
  - **Critical / High:** Mandatory fix + regression test + full retest prior to approval.
  - **Medium:** Fix where safe and low-risk; otherwise document owner, impact, and remediation target.
  - **Low:** Fix or defer with explicit rationale.
- **Exit Criteria:** All 20 acceptance gates in P9.6 §23 evaluated with reproducible evidence, zero unresolved Critical/High defects, JaCoCo & Jest coverage measured, and final honest Release Readiness Verdict issued.
