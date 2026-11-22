# EPAMS 2.0 — Workstream A: Existing Product Discovery & Capability Assessment

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream A)  
**Audited Git Baseline:** `6205bd5a9bc06a6dafbb259f73e8215325537f2e` (`main`)  
**Audit Date:** 2026-11-18  
**Method:** Direct read-only source code, schema, API, frontend, and QA artifact inspection (zero modifications).

---

## 1. Executive Reality Check (`VERIFIED FACT`)

EPAMS today is a **well-engineered, QA-hardened internal Employee Service & Governance Portal (modular monolith)** built on Java `25`, Spring Boot `3.5.16`, Spring Security `6.5.x`, Hibernate `6.6.x`, MySQL `8+` (Flyway `V1`–`V4`), and React `18` (`JavaScript`).  

However, **EPAMS is NOT yet a differentiated commercial product (`VERIFIED FACT` / `ASSUMPTION-CHECKED`)**:
- Its core runtime features (IAM, profiles, service catalog, 11-state tickets, comments, 2-stage approvals, business-calendar SLA, notifications, descriptive analytics, audit log) are **established industry table stakes**, except for its unusually rigorous **cryptographic SHA-256 tamper-evident audit chain with DB-level least-privilege enforcement** and **single-JAR self-hostable footprint**.
- Several enterprise administration gaps exist in the live codebase today (read-only Service Catalog REST API with no admin CRUD UI, hardcoded 2-stage approval booleans, zero Knowledge Base, zero multi-department onboarding orchestration, zero SSO/OIDC/SAML, legacy boilerplate `README.md`, and 10 legacy pre-P9.5 CRUD components coexisting alongside the modern P9.5 workspace).

---

## 2. Verified Architecture & Codebase Inventory (`VERIFIED FACT`)

- **Backend (`springapp/src/main/java/com/examly/springapp/`):**
  - `18` REST/SPA controllers (`17` in `controller/` + `AuthController` in `security/`)
  - `13` JPA entities (`UserAccount`, `Role`, `UserRoleMapping`, `Profile`, `RefreshToken`, `AuditLog`, `AuditChainHead`, `AccountStatusLog`, `ServiceCatalogItem`, `ServiceTicket`, `TicketComment`, `ApprovalTask`, `InAppAlert`) + `4` enums (`AccountStatus`, `TicketPriority`, `TicketStatus`, `ApprovalStatus`)
  - `13` Spring Data JPA repositories, `19` service classes, `3` workflow classes (`TicketStateMachine`, `BusinessCalendar`, `SlaScheduledProcessor`), `4` notification classes (`NotificationEventListener`, `EmailDeliveryAdapter`, `SmtpEmailDeliveryAdapter`, `TicketNotificationEvent`), `8` security classes, `7` exception classes, `32` DTOs (`23` core + `9` analytics)
  - `4` immutable Flyway migrations (`V1` IAM, `V2` Catalog & Tickets, `V3` Approvals & Audit Chain, `V4` Notifications & Analytics Indexes)
- **Frontend (`springapp/springbackend/src/`):**
  - `37` modern P9.5/P9.6 workspace files (`analytics/`, `approvals/`, `audit/`, `catalog/`, `common/`, `dashboard/`, `layout/`, `notifications/`, `profile/`, `queue/`, `tickets/`, `services/`)
  - `10` legacy pre-P9.5 CRUD files at `src/components/` root (`AuditLogs.js`, `Dashboard.js`, `ProfileForm.js`, `ProfileList.js`, `RoleForm.js`, `RoleList.js`, `UserForm.js`, `UserList.js`, `UserRoleMapping.js`, `UserRoleMappingForm.js`)
- **Verified QA Baseline (`6205bd5a`, `VERIFIED FACT`):**
  - Backend: `229` default H2 tests + `7` opt-in real MySQL `8.0.45` tests = `236 / 236` passed (`86.40%` lines / `66.62%` branches).
  - Frontend: `70 / 70` Jest passed (`73.09%` aggregate lines / `84.80%` modern workspace lines; `67.02%` / `70.40%` branches); `CI=true` build `0` warnings.
  - Cross-Browser & Accessibility: `30 / 30` (Chrome `157.0.8103.79`, Firefox `144.0.2`, WebKit `26.0`), `32 / 32` Light & Dark `axe-core 4.11.4` scans (`0` violations, `0` overflow), `11 / 11` packaged JAR SPA deep-link routes HTTP `200`.

---

## 3. Existing Capability Inventory & Verification Classification

| Feature | User | Problem Solved | Implementation Evidence | Classification | Maturity | Current Limitation | Business Value |
| :--- | :--- | :--- | :--- | :---: | :---: | :--- | :--- |
| **Identity & Access Management (JWT + HttpOnly Cookies + CSRF + Lockout)** | All users, Admins | Secure authentication, session lifecycle, brute-force protection | `AuthController`, `JwtFilter`, `SecurityConfig`, `RefreshToken`, `RbacAuthenticationTests` | **VERIFIED IN SOURCE** | Complete (local auth) | Local password auth only — **zero SSO (OIDC/SAML/Entra ID/Google)**, zero MFA, no self-service password reset email flow | Essential security baseline |
| **Role-Based & Department-Scoped Access (`11` roles)** | Admins, Managers, Specialists, Auditors | Prevents unauthorized cross-department & cross-user data access | `SecurityConfig`, `@PreAuthorize`, `P96RbacFieldSecurityIdorTests` (`11` IDOR/BOLA vectors) | **VERIFIED IN SOURCE** | Complete | Static enum/role-name checks; no custom admin-configurable permission policies | High governance trust |
| **Employee Directory & Profiles (Field-Level PII Redaction)** | Employees, HR, Admins | Employee self-service profile & safe directory lookup without leaking salary/DOB | `ProfileController` (role-aware `sortBy` allowlist `QA-DEF-002`), `EmployeeProfile.js` | **VERIFIED IN SOURCE** | Complete (basic HR record) | Single `Profile` entity; no org-chart hierarchy visualization, no document attachments, no HRIS sync | Protects employee privacy |
| **Service Catalog** | Employees | Structured internal service request menu with SLA & cost visibility | `ServiceCatalogController`, `ServiceCatalogItem`, `ServiceCatalog.js` | **PARTIALLY IMPLEMENTED** | Partial (Read-Only API) | **`ServiceCatalogController` exposes `GET` endpoints ONLY** — no Admin REST API or UI to create/edit/retire catalog items without SQL! | Core portal entry point (currently blocked for non-SQL admins) |
| **11-State Ticket Lifecycle & Comments** | Employees, Specialists, Admins | Structured request tracking with public & internal specialist discussion | `TicketController`, `TicketCommandService`, `TicketStateMachine`, `TicketDetail.js`, `TicketComments.js` | **VERIFIED IN SOURCE** | Complete | Fixed 11-state transitions hardcoded in Java; no file attachments; no parent/child linked tickets; no duplicate detection | Solid operational backbone |
| **Multi-Stage Approvals (Up to 2 Fixed Stages)** | Managers, Dept Heads, Admins | Governance gate on cost/access requests with mandatory rejection reason (BR-007 `>=15` chars) | `ApprovalController`, `ApprovalService`, `ApprovalQueue.js` | **VERIFIED IN SOURCE** | Complete (2-stage fixed) | Fixed `requiresManagerApproval` / `requiresSecondaryApproval` booleans; **cannot configure conditional rules** (e.g., cost > \$1,000 → Finance) or parallel approvals | Prevents unauthorized spend/access |
| **SLA & Business Calendar Engine** | Specialists, Managers, Ops | Business-hour SLA calculation, `ON_HOLD` pause tracking, 48h auto-closure, breach alerts | `BusinessCalendar`, `SlaScheduledProcessor`, `BusinessCalendarReopenWindowTests` | **VERIFIED IN SOURCE** | Complete (single-node) | Holidays configured via static env/defaults; single-JVM `AtomicBoolean` scheduler guard (needs ShedLock for multi-replica); no escalation chains | Drives predictable service delivery |
| **Notifications (In-App + Optional SMTP)** | All users | Real-time workflow updates with deduplication (`uk_alert_recipient_dedup`) & deep links | `NotificationController`, `NotificationEventListener`, `SmtpEmailDeliveryAdapter`, `NotificationsPage.js` | **VERIFIED IN SOURCE** | Complete (In-App) / Partial (External) | Email adapter disabled by default (`epams.email.enabled=false`); **no Slack/Teams/Webhook channels**; no per-user notification preference granularity | Keeps requesters & approvers informed |
| **Descriptive Operational Analytics** | Admins, Managers, Auditors, Specialists | Volume, SLA compliance, department backlog, popularity & approval counts | `AnalyticsController`, `AnalyticsService`, `AnalyticsDashboard.js` | **VERIFIED IN SOURCE** | Complete (descriptive) | Descriptive charts only — **no bottleneck root-cause diagnosis, no workload-rebalancing suggestions, no CSV/PDF export** | Visibility into past/current metrics |
| **Cryptographic SHA-256 Audit Chain & Verification** | Auditors, Admins | Tamper-evident compliance trail with DB-enforced `audit_log` immutability (`epams_app` denied `UPDATE`/`DELETE`) | `AuditChainService`, `AuditApiController`, `AuditLedger.js`, `Stage2RealMysqlIntegrationTests.test3` | **VERIFIED IN SOURCE** | Complete (Strongest unique asset) | Pessimistic `FOR UPDATE` on singleton `audit_chain_head` serializes audit writes; no external SIEM export | **Genuine governance differentiator** |
| **Account Lifecycle Risk Engine** | Admins | Risk scoring on account status changes | `RiskAssessmentService`, `RiskEngineTests` | **VERIFIED IN SOURCE** | Complete (niche) | Narrowly scoped to account status risk; not connected to employee onboarding/offboarding or SLA prediction | Supports IAM security |
| **Account Status Log** | Legacy IAM | Historical status record | `AccountStatusLog.java` (`id`, `status` ONLY!), `AccountStatusLogController` | **PARTIALLY IMPLEMENTED** | Stub / Legacy | Missing `userId`, `actor`, `timestamp`, `reason` — effectively superseded by `AuditLog` | Technical debt / duplication |
| **Enterprise UI/UX & Accessibility** | All users | Modern responsive Light/Dark workspace, `Ctrl+K` quick search, WCAG 2.2 AA `axe-core` clean | `tokens.css`, `components.css`, `AppLayout`, `Navbar`, `Sidebar`, `GlobalSearchModal` | **VERIFIED IN SOURCE** | Complete (automated) | Manual screen-reader audit not conducted; archived `react-scripts@5.0.1` CRA build toolchain (`100` dev-only npm advisories) | Modern consumer-grade self-hosted UX |
| **Real MySQL 8 Compatibility & Reliability** | Operators | Verified fresh & brownfield Flyway migrations, InnoDB concurrency, `mysqldump` restore | `application-mysql.properties`, `Stage2RealMysqlIntegrationTests` (`7/7`) | **VERIFIED IN SOURCE** | Verified (dev/staging) | Local MySQL URL uses `useSSL=false`; no production Dockerfile/Compose/Helm deployment assets yet (P10 scope) | Low-friction self-hosting foundation |
| **Knowledge Base / Self-Service Deflection** | Employees | Resolve common questions without opening tickets | *Not present in repository* | **NOT VERIFIED (ABSENT)** | Not Implemented | Zero KB entities, APIs, search, or deflection UI | **Major missing table-stakes/wedge capability** |
| **Configurable Workflow Engine / Lifecycle Orchestration** | Admins, HR, IT | No-code cross-department processes (onboarding/offboarding/custom routing) | *Not present in repository* | **NOT VERIFIED (ABSENT)** | Not Implemented | All workflow states & approval logic hardcoded in Java | **Primary EPAMS 2.0 opportunity** |

---

## 4. Identified Technical Debt, Duplication & Architectural Constraints (`VERIFIED FACT`)

1. **Duplicated Audit & Dashboard Surfaces:** Both legacy `AuditLogController` + `AuditLogs.js` and modern `AuditApiController` + `AuditLedger.js` exist; similarly `Dashboard.js` + `9` legacy CRUD forms (`2%`–`7%` Jest coverage) coexist with modern P9.5 workspace pages.
2. **Legacy `README.md` & Stub `AccountStatusLog`:** `README.md` still contains a 2-line generated UUID placeholder; `AccountStatusLog` is a 2-column stub (`id`, `status`).
3. **Read-Only Service Catalog API:** Operators cannot manage catalog offerings through the UI or REST API — this is an immediate operational blocker for any real customer adoption.
4. **Archived CRA Frontend Toolchain (`react-scripts@5.0.1`):** Causes `100` transitive dev-only `npm audit` findings and blocks React Router `v7` upgrade; migrating to Vite + Vitest should be sequenced cleanly in EPAMS 2.0 foundation work.
5. **No External Identity / Integration Layer:** Real organizations (`100`–`1,000` employees) expect OIDC/SAML SSO (Entra ID / Google Workspace / Okta) and outbound webhooks/Slack/Teams notifications; local-only auth limits real adoption.
6. **Single-JAR Operational Advantage:** Unlike Zammad (Ruby + Postgres + Elasticsearch + Redis) or ServiceNow (cloud-only), EPAMS runs as **one Java 25 JAR + one MySQL 8 database** — a major architectural advantage for lean self-hosted teams.
