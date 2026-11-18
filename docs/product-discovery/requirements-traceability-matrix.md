# Requirements Traceability Matrix (RTM 1.0)

**Document ID:** EPAMS-RTM-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved  
**Author:** Senior Quality Assurance Lead & Enterprise Architect  

---

## 1. Overview & Purpose

The Requirements Traceability Matrix (RTM) establishes bidirectional traceability across business objectives, functional specifications, system use cases, non-functional constraints, and quality assurance verification methods. This ensures that every line of code implemented in EPAMS satisfies a validated product requirement, and every requirement is verifiable.

---

## 2. Bidirectional Traceability Ledger

| Req ID | Requirement Title | Business Objective / Pain Point Addressed | Primary Persona | Linked Use Case | Linked NFRs | Verification Strategy | Priority Phase |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`FR-001`** | Stateless JWT Authentication | Eliminate insecure plaintext/basic auth; provide secure single-sign-on experience. | All Users | `UC-00` (Login) | `NFR-001`, `NFR-004`, `NFR-005` | MockMvc Integration Test (`JwtAuthenticationTests`, `ApiSecurityTests.test1_UnauthenticatedAccess_Returns401`, `test18_CookieMutationWithoutCsrf_Forbidden`, `test19_BearerMutation_SucceedsWithoutCookieCsrf`) | **P0 (MVP)** |
| **`FR-002`** | Brute-Force Defense & Lockout | Protect corporate accounts against automated password guessing and credential stuffing. | System Admin | `UC-00` (Login) | `NFR-006` | Automated unit tests simulating 5+ failed attempts verifying 15-minute lock. | **P0 (MVP)** |
| **`FR-003`** | Silent Session Refresh | Eliminate disruptive user logouts while keeping access tokens short-lived (15 min). | All Users | `UC-00` (Session) | `NFR-001`, `NFR-008` | Axios interceptor end-to-end test with expired access token. | **P0 (MVP)** |
| **`FR-004`** | Session Revocation on Logout | Ensure complete invalidation of tokens upon logout, preventing replay attacks. | All Users | `UC-00` (Logout) | `NFR-006` | Verification that revoked refresh tokens in DB return 401 on refresh. | **P0 (MVP)** |
| **`FR-005`** | Administrative Account Unlock | Empower administrators to resolve user lockouts without database surgery. | System Admin | `UC-07` (Admin) | `NFR-001`, `NFR-006` | Spring Security MockMvc test with `ROLE_ADMIN` vs `ROLE_EMPLOYEE` (403). | **P0 (MVP)** |
| **`FR-006`** | Employee Master Record | Centralize employee personal and employment information in a single database. | HR Manager | `UC-03` (Profile) | `NFR-002`, `NFR-007` | Repository integration test validating uniqueness of `employeeId`. | **P0 (MVP)** |
| **`FR-007`** | Paginated Directory Search | Allow employees to discover colleagues without loading full database into browser memory. | All Users | `UC-04` (Directory) | `NFR-001`, `NFR-002`, `NFR-010` | Database pagination benchmark query test across 1,000+ mock records. | **P0 (MVP)** |
| **`FR-008`** | Self-Service Profile View | Eliminate redundant HR inquiry emails for basic employee details. | Employee | `UC-03` (Profile) | `NFR-001`, `NFR-010` | REST API `/api/profiles/me` validation against logged-in JWT context. | **P0 (MVP)** |
| **`FR-009`** | Sensitive Field Masking (Salary) | Safeguard employee compensation privacy under DPDP and internal compliance. | Employee, HR | `UC-03` (Profile) | `NFR-006`, `NFR-007` | Security test ensuring non-HR users receive null/masked salary fields. | **P0 (MVP)** |
| **`FR-010`** | Service Catalog Browsing | Provide clear discovery of requestable internal services across HR, IT, Facilities. | Employee | `UC-01` (Catalog) | `NFR-010`, `NFR-011` | React frontend test, `TicketPersistenceTests`, & REST API test `ApiSecurityTests.test8_ServiceCatalog_PublicAccess_Returns200` | **P0 (MVP)** |
| **`FR-011`** | Service Ticket Submission | Replace chaotic email/chat requests with structured, categorized forms. | Employee | `UC-01` (Ticket) | `NFR-001`, `NFR-011` | Integration test: `TicketPersistenceTests`, `ApiSecurityTests.test9_TicketCreation_Success_Returns201`, `test4_TicketCreation_InvalidPayload_Returns400_RFC7807` | **P0 (MVP)** |
| **`FR-012`** | Ticket Lifecycle State Machine | Enforce deterministic workflow stages from submission to closure. | All Users | `UC-01`, `UC-02` | `NFR-006`, `NFR-009` | Finite State Machine tests: `TicketWorkflowTests` (test1–test5), `BusinessCalendarReopenWindowTests` (10 deterministic 48-business-hour boundary scenarios, P9.1), & REST API test `ApiSecurityTests.test13_AssignedResolverCanUpdateStatus` | **P0 (MVP)** |
| **`FR-013`** | Department Queue Filtering | Enable department resolvers to prioritize tickets by SLA urgency and status. | IT/HR Specialist | `UC-05` (Queue) | `NFR-001`, `NFR-002` | Queue isolation tests: `TicketWorkflowTests.test11`, `TicketPersistenceTests.test9`, `ApiSecurityTests.test11_DepartmentQueue_PermittedForDepartmentResolver`, `test12_CrossDepartmentQueue_Blocked_Returns403` | **P0 (MVP)** |
| **`FR-014`** | Resolver Ticket Assignment | Establish clear ownership of internal service tasks, preventing duplicate handling. | IT/HR Specialist | `UC-05` (Queue) | `NFR-001` | Service workflow test: `TicketWorkflowTests.test1`, `test10`, & `ApiSecurityTests.test13_AssignedResolverCanUpdateStatus` | **P0 (MVP)** |
| **`FR-015`** | Threaded Discussion Comments | Keep request communication contextualized on the ticket, ending scattered chat. | Employee, Resolver | `UC-06` (Comments) | `NFR-001`, `NFR-010` | Comment audit and internal-flag visibility test: `TicketWorkflowTests.test17`, `ApiSecurityTests.test14_InternalComments_HiddenFromRequester` | **P0 (MVP)** |
| **`FR-016A`** | Default Single-Level Routing | Ensure routine business requests receive timely direct manager sign-off. | Reporting Manager | `UC-01`, `UC-02` | `NFR-001`, `BR-001` | Integration test: `TicketWorkflowTests.test6_SingleLevelApprovalWorkflow`, `ApiSecurityTests.test9` | **P0 (MVP)** |
| **`FR-016B`** | Conditional Two-Level Escalation | Ensure capital hardware and high-value expenses receive Department Head review. | Department Head | `UC-01`, `UC-02` | `NFR-001`, `BR-001`, `BR-008`, `BR-009` | Integration test: `TicketWorkflowTests.test8_TwoStageApprovalWorkflow` | **P0 (MVP)** |
| **`FR-017`** | Approval Decisions & Reasoning | Provide one-click approval/rejection with mandatory reasoning ($\ge 15$ chars). | Reporting Manager, Dept Head | `UC-02` (Approval) | `NFR-001`, `NFR-010`, `BR-007` | Anti-self-approval and rejection tests: `TicketWorkflowTests.test7`, `test9`, `ApiSecurityTests.test6_ApprovalDecision_InvalidShortNotes_Returns400` | **P0 (MVP)** |
| **`FR-018`** | Approval Audit Trail | Ensure complete regulatory auditability for all managerial decisions. | System Admin | `UC-02`, `UC-08` | `NFR-006`, `NFR-007`, `BR-002` | Cryptographic SHA-256 chain verification: `TicketWorkflowTests.test14`, `test15`, `ApiSecurityTests.test15_AuditorCanVerifyChain_Returns200`, `test16_RegularEmployeeCannotAccessAuditVerify_Returns403` | **P0 (MVP)** |
| **`FR-019`** | In-App Notifications | Keep users informed of pending approvals and status changes without inbox clutter. | All Users | `UC-01`, `UC-02` | `NFR-001`, `NFR-010` | Post-commit transactional event persistence: `TicketWorkflowTests.test16_PostCommitNotificationPersistence` | **P0 (MVP)** |
| **`FR-020`** | Transactional Email Dispatch | Proactively alert external stakeholders of urgent approval requests. | All Users | `UC-01`, `UC-02` | `NFR-008`, `NFR-013` | Mock JavaMailSender integration test and Mailpit container inspection. | **P1 (Post-MVP)** |
| **`FR-021`** | SLA Breach Warning Engine | Prevent tickets from falling through the cracks; maintain accountability. | Department Lead | `UC-05` (Queue) | `NFR-008` | SLA calendar & pause tracking: `TicketWorkflowTests.test12_BusinessCalendarSlaCalculations` & `test13_SlaPauseAndResumeTracking` | **P1 (Post-MVP)** |
| **`NFR-012`** | Optimistic Concurrency Control | Prevent lost updates and race conditions during concurrent ticket modifications. | All Actors | `UC-02`, `UC-05` | Data Consistency | JPA `@Version` concurrency test asserting token increment (`TicketPersistenceTests.test4`) & HTTP 409 test (`ApiSecurityTests.test17_OptimisticLockingFailure_Returns409Conflict`). | **P0 (MVP)** |
| **`NFR-013`** | Mailpit Testing Infrastructure | Isolate email testing in local dev without external SMTP dependencies. | Developers / QA | `UC-01`, `UC-02` | Reliability | Docker Compose integration test verifying Mailpit port 1025 captures outbound emails. | **P0 (MVP)** |

---

## 3. Test Coverage & Verification Matrix Summary

```
Total Tracked Requirements:  24 (22 in P0 MVP, 2 in P1 Fast-Follow)
Automated Integration Tests: 17 requirements covered via Spring Boot Test / MockMvc
Manual / E2E Tests:          7 requirements covered via UI & Cypress / Postman
Traceability Completeness:   100% (No orphan requirements; all mapped to user value)
```

---

## 4. P9.5 Enterprise UI/UX Transformation Traceability Addendum

Added during **Phase P9.5 — Enterprise UI/UX Transformation** (post-Stage 2 MySQL verification, pre-P10 deployment). Preserves all P0–P9 backend business rules, Flyway `V1`–`V4`, JWT/CSRF/RBAC boundaries, and 11-state lifecycle invariants.

| Gap / Req ID | UI/UX Capability | Linked Functional / NFR | Implementation Artifacts | Automated Verification | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`UX-001` / `UX-002`** | Design System Tokens & FOUC-Free Theme Engine (Light / Dark / System) | `NFR-010`, `NFR-011` | `src/styles/design-system/tokens.css`, `components.css`, `public/index.html`, `src/services/PreferencesContext.js` | `P95UiUxTransformation.test.js` (`P95-02`, `P95-03`) | **Verified** |
| **`UX-003`** | Hardcoded Demo Credential Removal & Accessible Login Modal | `FR-001`, `NFR-006` | `src/components/LoginModal.js` | `P95UiUxTransformation.test.js` (`P95-01`), `P7FrontendIntegration.test.js` | **Verified** |
| **`UX-004` / `UX-005`** | Enterprise Application Shell, Collapsible Sidebar, Mobile Drawer & `Ctrl+K` Quick Search | `FR-007`, `FR-010`, `NFR-010` | `AppLayout.js`, `Navbar.js`, `Sidebar.js`, `Breadcrumb.js`, `GlobalSearchModal.js` | `P95UiUxTransformation.test.js` (`P95-02`, `P95-04`) | **Verified** |
| **`UX-006`** | Self-Service Employee Profile & Workspace Preferences (`/profile`, `/preferences`) | **`FR-008`**, `FR-009`, `NFR-010` | `src/components/profile/EmployeeProfile.js`, `PreferencesPage.js`, `Avatar.js` (`GET /api/profiles/me`) | `P95UiUxTransformation.test.js` (`P95-05`, `P95-06`) | **Verified** |
| **`UX-007` / `UX-008`** | Employee Workspace Dashboard, Notifications Preview & Backend-Accurate KPIs | `FR-010`, `FR-011`, `FR-019` | `EmployeeDashboard.js`, `LoadingSkeleton.js`, `ToastContext.js` | `P95UiUxTransformation.test.js` (`P95-08`), `P7FrontendIntegration.test.js` | **Verified** |
| **`UX-009` / `UX-010`** | Service Catalog, Create Ticket Modal, My Tickets Table/Card Switcher & 11-State `StatusTimeline` | `FR-010`, `FR-011`, `FR-012`, `FR-015` | `ServiceCatalog.js`, `CreateTicketModal.js`, `MyTickets.js`, `TicketDetail.js`, `StatusTimeline.js`, `TicketComments.js` | `P95UiUxTransformation.test.js` (`P95-07`), `P7FrontendIntegration.test.js` | **Verified** |
| **`UX-011` / `UX-012`** | Resolver Department Queue, Manager Approval Queue, Analytics & Audit Ledger Modernization | `FR-013`, `FR-014`, `FR-016A/B`, `FR-017`, `FR-018` | `DepartmentQueue.js`, `ApprovalQueue.js`, `AnalyticsDashboard.js`, `AuditLedger.js` | `P7FrontendIntegration.test.js`, `P8FrontendNotificationsAnalytics.test.js`, `P95UiUxTransformation.test.js` | **Verified** |
| **`UX-013` / `UX-014`** | WCAG 2.2 AA Semantics, Reduced Motion, Responsive Breakpoints & Live Visual Verification | `NFR-010`, `NFR-011` | `tokens.css`, `components.css`, `docs/ui-ux/accessibility-audit.md`, `responsive-design-verification.md`, `p95-1-live-browser-visual-acceptance-report.md` | Jest `42/42` + Build (`0` warnings) + Backend `208/208` + Live Chrome `157.0.8103.79` (`43` PNGs, `37` `axe-core 4.11.4` scans with `0` violations) | **Verified (`VIS-01` Approved)** |
