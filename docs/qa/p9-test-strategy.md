# EPAMS P9 — Master Quality Assurance & Verification Test Strategy

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  
**Status:** Active Verification Strategy  

---

## 1. Quality Objectives & Evidence Governance

The P9 test strategy replaces assumption-based sign-off with independent, evidence-driven validation across all functional, security, concurrency, data-integrity, reliability, and performance dimensions established in P0–P8.

Every verification claim in P9 is strictly classified under one of six evidence states:

| Classification | Definition |
| :--- | :--- |
| **`VERIFIED`** | Directly executed and observed in code/tests/logs during P9 with zero fabrication. |
| **`REPORT-ONLY`** | Claimed in prior P0–P8 documentation without independent live reproduction. |
| **`ASSUMPTION`** | Architectural expectation not yet backed by runtime proof. |
| **`NOT TESTED`** | Valid test scope not executed in this phase. |
| **`BLOCKED`** | Cannot be executed due to explicit environment constraints (e.g., offline sandbox blocking external npm registry or live MySQL TCP daemon). |
| **`DEFERRED`** | Explicitly out of scope for P9 (e.g., P10 deployment, SMTP email delivery, CI/CD pipelines). |

---

## 2. Multi-Layer Test Pyramid & Scope Matrix

| Test Layer | Tooling & Execution Mechanism | Coverage Scope | Environment Status |
| :--- | :--- | :--- | :--- |
| **L1 — Domain Unit & Workflow Invariants** | JUnit 5 + Spring Context (`TicketWorkflowTests`, `BusinessCalendar`, `TicketStateMachine`) | State machine transitions, 48h dispute window, anti-self-approval, 15-char reason boundaries, SLA pause/resume. | `VERIFIED` (H2 MySQL mode) |
| **L2 — Security, RBAC & BOLA/IDOR Integration** | MockMvc + Spring Security Filter Chain (`RbacAuthenticationTests`, `ApiSecurityTests`, `P9SecurityAndWorkflowQaTests`) | 7-role × 14-resource authorization matrix, JWT signature/expiry/suspension rejection, CSRF double-submit enforcement, legacy IAM endpoint hardening, internal comment privacy, BOLA prevention, bounded pagination. | `VERIFIED` |
| **L3 — Concurrency, Reliability & Failure Injection** | Multi-threaded `ExecutorService` + `@SpringBootTest` (`P9SecurityAndWorkflowQaTests`, `NotificationTests`, `AuditLogTests`) | Optimistic locking (`@Version` 409 conflict), 6-thread concurrent audit hash-chain appends, 8-thread atomic notification dedup, `CallerRunsPolicy` executor saturation, transaction rollback safety, malformed JSON 400 handling. | `VERIFIED` |
| **L4 — Live-Server HTTP End-to-End Integration** | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + real Java `HttpClient` + `CookieManager` (`P9LiveServerE2EIntegrationTests`) | Real TCP Tomcat server, `HttpOnly` + `SameSite=Lax` cookies, live `XSRF-TOKEN` 403 block/pass, silent refresh rotation, logout revocation, and full cross-persona Catalog → Ticket → Approval → Specialist Queue → Internal Note Privacy → Resolution → Notification → Analytics → Audit Chain verification. | `VERIFIED` |
| **L5 — Browser UI End-to-End Automation** | Playwright (`springapp/springbackend/playwright.config.js` + `e2e/epams.spec.js`) | Real Chromium DOM rendering, login modal, SPA routing, cookie storage non-leakage, `E2E-01`..`E2E-08`. | `BLOCKED` (Playwright npm package unavailable in offline sandbox; suite authored and ready for P10/CI) |
| **L6 — Frontend Component, Contract & Security Tests** | Jest + React Testing Library (`App.test.js`, `P8Features.test.js`, `P9FrontendQa.test.js`) | Service API contracts, NotificationBell & NotificationsPage UX, AnalyticsDashboard rendering & scoping, `session-expired` AuthContext clearing, `ProtectedRoute` 403 rendering, RFC 7807 error parsing, duplicate-submit protection. | `VERIFIED` |
| **L7 — Database Schema & Migration Validation** | Flyway V1–V4 + Hibernate `ddl-auto=validate` (`SchemaValidationTests`, `BrownfieldPreservationTests`) | Fresh V1–V4 clean migration, brownfield V1-existing + V2–V4 migration, FK/unique constraints, rollback isolation. | H2 MySQL mode `VERIFIED`; Live MySQL 8.0+ `BLOCKED` |
| **L8 — Scale & Concurrent Load Performance** | `PerformanceBenchmarkTests` (P8 120-ticket baseline) + `P9ScalePerformanceTests` (P9 480-ticket 4x scale + 12-thread concurrent load) | Cold vs warm query latency, SQL query count (Hibernate Statistics), N+1 regression guard, 120 concurrent mixed API calls with 0 errors. | `VERIFIED` (H2 in-memory; MySQL query plans `BLOCKED`) |

---

## 3. Non-Negotiable Quality Gates for P9 Sign-Off

1. **Zero Regressions:** All 177 P8 baseline backend tests and 28 P8 baseline frontend tests must pass with zero modifications weakening assertions.
2. **Zero Unresolved Critical/High Security Defects:** Every confirmed CRITICAL/HIGH defect discovered in Phase A (`SEC-P9-01`..`SEC-P9-04`) plus MEDIUM (`SEC-P9-05`) must be remediated in source code and locked down with automated regression tests.
3. **Production Build Cleanliness:** `npm run build` must succeed with 0 errors and 0 ESLint warnings, preserving P8 lazy-loaded chunks (`AnalyticsDashboard`, `AuditLedger`).
4. **Honest Risk & Blocker Accounting:** Any test impossible to execute in the offline sandbox (live MySQL 8.0+ TCP verification, live Playwright Chromium execution, OWASP CVE database scan) must be explicitly marked `BLOCKED` and tracked as mandatory P10 entry gates — never disguised as `VERIFIED`.
5. **No P10 Scope Creep:** Zero Docker/Kubernetes production manifests, CI/CD pipelines, cloud deployments, SMTP integrations, or Flyway V1–V4 historical migration edits.

