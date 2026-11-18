# EPAMS — P9.6 Risk-Based Test Plan

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage B: Risk-Based Prioritization Plan
**Baseline Commit:** `a4857679c0bde310e4b705a46d7e7ef701bc3901`

---

## 1. Risk Scoring Matrix (`Likelihood × Impact`)

| Priority Tier | Risk Area | Concrete Failure Modes | Primary Test Suites & Mitigations |
| :--- | :--- | :--- | :--- |
| **P0 — Critical** | **Authentication, JWT & Session Revocation** | Brute-force bypass, suspended/locked account using old JWT/refresh token, refresh token replay, missing CSRF on cookie mutations, cookie flag misconfiguration | `JwtAuthenticationTests`, `RbacAuthenticationTests`, `P96RbacFieldSecurityIdorTests`, `P96ApiAndFieldValidationTests` |
| **P0 — Critical** | **RBAC, Field-Level PII & IDOR/BOLA** | Cross-user ticket/profile/notification access, cross-department queue leakage, salary exposure or `sortBy=salary` oracle, internal comment leakage to requester, mass-assignment of `roles`/`status`/`assigneeId` | `ApiSecurityTests`, `P96RbacFieldSecurityIdorTests`, `P96ApiAndFieldValidationTests` |
| **P0 — Critical** | **11-State Lifecycle & Approval Governance** | Illegal state skip (e.g., `SUBMITTED → CLOSED`), self-approval bypass, Stage-2 escalation skipped, duplicate approval decision, WIP cancellation bypass, reopen after 48 business hours | `TicketWorkflowTests`, `BusinessCalendarReopenWindowTests`, `P96UnitBusinessLogicTests`, `P96TransactionReliabilityTests` |
| **P0 — Critical** | **Cryptographic Audit Chain & Concurrency** | Sequence gaps, hash chain forks under concurrent writes, rollback corrupting head pointer, tamper undetected, `AuditLog` UPDATE/DELETE bypass | `AuditLogTests`, `P96UnitBusinessLogicTests`, `P96TransactionReliabilityTests`, `Stage2RealMysqlIntegrationTests` |
| **P1 — High** | **Input Validation & Error Hygiene** | Blank registration credentials, invalid `sortBy` HTTP 500, raw SQL/exception leakage in HTTP 500, XSS stored/reflected payloads, SQLi metacharacters, malformed JSON / wrong `Content-Type` (`415`) | `P96ApiAndFieldValidationTests` + remediation of `QA-DEF-002`, `QA-DEF-003`, `QA-DEF-004` |
| **P1 — High** | **Transaction Boundaries & Failure Recovery** | Phantom notification after rolled-back ticket transaction, SMTP failure rolling back ticket creation, duplicate notification creation, optimistic locking silent overwrite, scheduler overlap | `NotificationTests`, `P96TransactionReliabilityTests`, `Stage1RemediationVerificationTests` |
| **P1 — High** | **Real MySQL 8 Persistence & Schema Integrity** | H2-only SQL syntax drift, missing indexes causing full table scans, FK/unique violations, brownfield upgrade breakage, backup/restore data loss | `Stage2RealMysqlIntegrationTests`, MySQL `EXPLAIN` & `mysqldump` restore audit |
| **P2 — Medium** | **SPA Routing, UI Flows & Accessibility** | Standalone JAR deep-link refresh 500 (`QA-DEF-001`), multi-viewport overflow (`320px`–`1440px`), dark-mode contrast regressions, keyboard trap, double submit | `P95UiUxTransformation.test.js`, `P96FrontendComprehensive.test.js`, Chrome 157 + `axe-core 4.11.4` E2E |
| **P2 — Medium** | **Performance & Scalability** | N+1 query regression on dashboard/queues, unbounded pagination (`size=10000`), p95 latency spikes under concurrent users, connection pool exhaustion | `PerformanceBenchmarkTests`, `P9ScalePerformanceTests`, live HTTP load/stress/spike benchmark |

---

## 2. Coverage Targets (Risk-Weighted)

| Domain | Risk Tier | Target Line Coverage | Target Branch Coverage |
| :--- | :---: | :---: | :---: |
| Workflow (`TicketStateMachine`, `BusinessCalendar`, `SlaScheduledProcessor`) | P0 | ≥ 85% | ≥ 80% |
| Ticket & Approval Services (`TicketCommandService`, `TicketQueryService`, `ApprovalService`) | P0 | ≥ 85% | ≥ 80% |
| Security & IAM (`AuthService`, `JwtService`, `JwtFilter`, `RefreshTokenService`, `TicketSecurityEvaluator`) | P0 | ≥ 85% | ≥ 80% |
| Audit & Notifications (`AuditChainService`, `NotificationService`, `NotificationEventListener`) | P0 | ≥ 85% | ≥ 80% |
| Analytics (`AnalyticsService`) | P1 | ≥ 85% | ≥ 80% |
| Overall Backend Application | All | ≥ 75% (measured from baseline) | ≥ 65% (measured from baseline) |
| Frontend React Workspace (Jest) | P1/P2 | ≥ 65% (up from `51.00%` baseline) | ≥ 55% (up from `43.89%` baseline) |
