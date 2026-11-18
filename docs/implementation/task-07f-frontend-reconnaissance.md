# EPAMS — Task 07F: Frontend Architecture & Backend API Reconnaissance

**Document ID:** EPAMS-RECON-07F-001  
**Phase:** Phase 2 — Testing Foundation & Workflow Orchestration  
**Task:** Task 07F — Complete Frontend Integration, Role-Based User Experience & End-to-End Application Delivery  
**Date:** November 17, 2026  
**Status:** COMPLETE & VERIFIED  
**Authors:** Senior Full-Stack Software Architect, Senior React Engineer, Backend Integration Engineer & QA Specialist  

---

## 1. Executive Summary

This reconnaissance audit reviews the existing React frontend located in `springapp/springbackend` and maps its architecture against the verified Phase 2 backend REST API boundary (`/api/v1/*` and `/api/*`). 

The frontend was originally constructed as an administrative portal for Phase 1 Identity & Access Management (User, Role, Profile, and Mapping CRUD). To deliver a unified, enterprise-grade Employee Lifecycle and Service Management Platform (EPAMS), the frontend must be expanded into a role-aware modular application supporting:
1. **Employee Persona:** Service catalog browsing, ticket creation with validation, "My Tickets" status tracking, contextual public commenting, and ticket cancellation/reopening.
2. **Specialist Persona:** Department queue workspace (`/api/v1/departments/{department}/queue`), ticket assignment, lifecycle transitions (FSM: `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`), and confidential internal discussion notes.
3. **Approver Persona (Managers / Dept Heads):** Multi-stage approval queue (`/api/v1/approvals/pending`), anti-self-approval enforcement, and decision submission with mandatory notes.
4. **Auditor / Admin Persona:** Cryptographic SHA-256 hash-chain verification (`/api/v1/audit/verify`) and immutable audit event ledger inspection (`/api/v1/audit/logs`).

---

## 2. Existing Frontend Architecture & Tooling

### 2.1 Project Metadata & Build Tooling
- **Location:** `springapp/springbackend`
- **Framework:** React 18.2.0 (`react`, `react-dom`)
- **Build Tool:** Create React App (`react-scripts 5.0.1`)
- **Routing:** React Router DOM v6.11.1 (`BrowserRouter`, `Routes`, `Route`, `NavLink`)
- **HTTP Client:** Axios v1.4.0 with HTTP-only cookie support (`withCredentials: true`)
- **Styling:** Vanilla CSS (`App.css`, `Dashboard.css`, `index.css`) with modular inline styles
- **State Management:** React Context API (`AuthContext.js` managing `currentUser`, `loading`, session refresh listener)
- **Maven Packaging Integration:** `frontend-maven-plugin 1.12.1` in `springapp/pom.xml` builds the React bundle via Node v20.16.0 / npm 10.6.0 and copies build artifacts to `${project.build.outputDirectory}/static` for unified monolithic delivery.

### 2.2 Existing Component Inventory
| Component | Path | Responsibility / Purpose | Reuse Assessment |
|:---|:---|:---|:---|
| `App.js` | `src/App.js` | Top-level routing and basic navbar | **Modify:** Upgrade to enterprise layout shell with role-aware sidebar/header. |
| `AuthContext.js` | `src/services/AuthContext.js` | User session context, login, register, logout | **Modify:** Enhance with role-checking helpers (`hasRole`, `isResolver`, `isApprover`, `isAuditor`). |
| `api.js` | `src/services/api.js` | Axios instance with CSRF & 401 refresh interceptors | **Modify:** Add RFC 7807 error extractor and configurable base URL. |
| `Dashboard.js` | `src/components/Dashboard.js` | Admin metric cards (`/dashboard/metrics`) | **Retain / Adapt:** Keep as Admin metrics; create dedicated Employee Dashboard. |
| `LoginModal.js` | `src/components/LoginModal.js` | Modal login/register form | **Retain / Enhance:** Support standalone route and modal presentation. |
| `UserList.js` | `src/components/UserList.js` | IAM User account administration | **Retain:** Under Admin menu. |
| `UserForm.js` | `src/components/UserForm.js` | Create/edit IAM user account | **Retain:** Under Admin menu. |
| `RoleList.js` | `src/components/RoleList.js` | Role administration | **Retain:** Under Admin menu. |
| `RoleForm.js` | `src/components/RoleForm.js` | Create/edit roles | **Retain:** Under Admin menu. |
| `ProfileList.js` | `src/components/ProfileList.js`| Employee profile directory | **Retain:** Under Directory/Admin menu. |
| `ProfileForm.js` | `src/components/ProfileForm.js`| Profile creation/editing | **Retain:** Under Directory/Admin menu. |
| `UserRoleMapping.js` | `src/components/UserRoleMapping.js` | Role assignment table | **Retain:** Under Admin menu. |
| `UserRoleMappingForm.js` | `src/components/UserRoleMappingForm.js` | Assign roles to users | **Retain:** Under Admin menu. |
| `AuditLogs.js` | `src/components/AuditLogs.js` | Legacy unpaginated audit viewer (`/audit`) | **Upgrade:** Modernize to `/api/v1/audit/logs` with pagination and verification trigger. |

---

## 3. Existing Backend REST API Inventory

All modern service management capabilities operate under `/api/v1/*`, while legacy IAM services operate under `/api/*`:

| Domain | Method | Actual Verified Controller Route | Request DTO / Payload | Response DTO / Payload | Required Authority |
|:---|:---|:---|:---|:---|:---|
| **IAM Auth** | `POST` | `/api/auth/login` | `{ username, password }` | Set-Cookie: `accessToken`, `refreshToken` | Public |
| **IAM Auth** | `POST` | `/api/auth/register` | `{ username, password, email, phone }` | Set-Cookie: `accessToken`, `refreshToken` | Public |
| **IAM Auth** | `POST` | `/api/auth/refresh` | None (reads cookie) | Refreshed `accessToken` cookie | Public / Cookie |
| **IAM Auth** | `POST` | `/api/auth/logout` | None | Cleared cookies | Authenticated |
| **User Profile**| `GET` | `/api/users/me` | None | `UserResponseDTO` (`id`, `username`, `roles`, `department`, `fullName`) | Authenticated |
| **Service Catalog** | `GET` | `/api/v1/catalog` | Query: `department`, `category`, `page`, `size` | `Page<ServiceCatalogItemDTO>` | Authenticated |
| **Service Catalog** | `GET` | `/api/v1/catalog/{id}` | Path: `id` | `ServiceCatalogItemDTO` | Authenticated |
| **Tickets** | `POST` | `/api/v1/tickets` | `CreateTicketRequestDTO` (`catalogItemId`, `targetDepartment`, `priority`, `justification`) | `ServiceTicketDTO` (HTTP 201) | Authenticated |
| **Tickets** | `GET` | `/api/v1/tickets/{id}` | Path: `id` | `ServiceTicketDTO` | Requester / Resolver / Admin |
| **Tickets** | `GET` | `/api/v1/tickets/my` | Query: `page`, `size` | `Page<ServiceTicketDTO>` | Authenticated Requester |
| **Tickets** | `POST` | `/api/v1/tickets/{id}/assign` | `TicketAssignRequestDTO` (`assigneeId`) | `ServiceTicketDTO` | Resolver / Admin |
| **Tickets** | `POST` | `/api/v1/tickets/{id}/status` | `TicketStatusUpdateRequestDTO` (`newStatus`, `reason`, `version`) | `ServiceTicketDTO` | Assignee / Resolver / Admin |
| **Tickets** | `POST` | `/api/v1/tickets/{id}/cancel` | `TicketActionRequestDTO` (`reason`, `version`) | `ServiceTicketDTO` | Requester / Admin |
| **Tickets** | `POST` | `/api/v1/tickets/{id}/reopen` | `TicketActionRequestDTO` (`reason`, `version`) | `ServiceTicketDTO` | Requester / Admin |
| **Comments** | `GET` | `/api/v1/tickets/{id}/comments` | Path: `id` | `List<TicketCommentDTO>` (internal filtered for requesters) | Ticket Participant |
| **Comments** | `POST` | `/api/v1/tickets/{id}/comments` | `TicketCommentRequestDTO` (`commentText`, `isInternal`) | `TicketCommentDTO` (HTTP 201) | Ticket Participant |
| **Department Queue** | `GET` | `/api/v1/departments/{department}/queue` | Query: `status`, `page`, `size` | `Page<ServiceTicketDTO>` | Department Resolver / Admin |
| **Approvals** | `GET` | `/api/v1/approvals/pending` | None | `List<ApprovalTaskDTO>` | Manager / Dept Head / Admin |
| **Approvals** | `GET` | `/api/v1/approvals/tickets/{ticketId}` | Path: `ticketId` | `List<ApprovalTaskDTO>` | Ticket Participant |
| **Approvals** | `POST` | `/api/v1/approvals/{taskId}/decide` | `ApprovalDecisionRequestDTO` (`decision`, `decisionNotes`) | `ApprovalTaskDTO` | Assigned Approver / Admin |
| **Audit** | `GET` | `/api/v1/audit/logs` | Query: `page`, `size` | `Page<AuditLogDTO>` | Admin / Auditor |
| **Audit** | `GET` | `/api/v1/audit/verify` | None | `AuditVerificationReport` (`valid`, `totalChainedRecords`, `violations`) | Admin / Auditor |

---

## 4. Frontend / Backend Integration Gaps

1. **Service Catalog & Ticket Submission UI Missing:**  
   The existing frontend had zero screens for browsing catalog items or submitting tickets. Users had no UI to request hardware, software access, or facilities services.
2. **Department Resolver Workspace Missing:**  
   Resolvers in IT, Facilities, or HR had no queue view to inspect incoming tickets, self-assign tasks, or transition lifecycle states (`IN_PROGRESS`, `ON_HOLD`, `RESOLVED`).
3. **Manager Approval Workspace Missing:**  
   Managers and Department Heads had no UI to view pending approval cards, review requester justifications, or submit approve/reject decisions.
4. **Contextual Discussion Comments Missing:**  
   There was no discussion timeline on tickets to communicate with requesters or record internal specialist notes.
5. **Route Mapping Mismatch:**  
   As audited in Task 07E.1, documentation had noted `/api/v1/queue/{dept}` and `/api/v1/approvals/{taskId}/decision`. The frontend must strictly use the verified live routes: `/api/v1/departments/{department}/queue` and `/api/v1/approvals/{taskId}/decide`.
6. **In-App Notification API Gap [REPORT-ONLY]:**  
   While `InAppAlert` exists in the backend database and events are emitted on ticket actions, the backend does not expose a REST controller for `/api/v1/notifications`. The frontend will not simulate or fake notification endpoints; it will document this gap and rely on direct ticket status indicators.

---

## 5. Required New Components & Modules

To achieve full Stage B–L implementation, the following components will be created:

### 5.1 Layout & Navigation
- `src/components/layout/Navbar.js` — Role-aware top header with user profile menu, department tag, and logout action.
- `src/components/layout/Sidebar.js` — Collapsible navigation showing accessible sections based on the active user's roles (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_IT_SUPPORT`, `ROLE_FACILITIES_CREW`, `ROLE_HR_SPECIALIST`, `ROLE_ADMIN`, `ROLE_AUDITOR`).
- `src/components/layout/AppLayout.js` — Master container wrapping sidebar and content area with responsive styling.

### 5.2 Common UI Components
- `src/components/common/Badge.js` — Colored status pills for ticket states (`SUBMITTED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, etc.) and priority tags (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- `src/components/common/Pagination.js` — Reusable zero-indexed pagination control.
- `src/components/common/LoadingSpinner.js` & `EmptyState.js` — Clean enterprise feedback states.
- `src/components/common/Alert.js` — RFC 7807 error banner displaying field-level validation messages.

### 5.3 Service Catalog & Ticket Submission
- `src/components/catalog/ServiceCatalog.js` — Grid of service offerings with category filters, cost, SLA, and approval badges.
- `src/components/tickets/CreateTicketModal.js` — Form with client-side validation for priority, target department, and justification.

### 5.4 Ticket Tracking & Details
- `src/components/tickets/MyTickets.js` — Employee ticket tracking list with search and status filters.
- `src/components/tickets/TicketDetail.js` — Full ticket overview showing SLA target, assignee, lifecycle action buttons (Cancel, Reopen within 48h), and threaded comments.
- `src/components/tickets/TicketComments.js` — Threaded discussion list with toggleable `isInternal` checkbox for specialists.

### 5.5 Specialist & Approver Workspaces
- `src/components/queue/DepartmentQueue.js` — Specialist department queue board with status transitions, self-assign, and SLA breach indicators.
- `src/components/approvals/ApprovalQueue.js` — Pending managerial tasks with single-click approve and reject-with-notes modal.

### 5.6 Audit & Verification
- `src/components/audit/AuditLedger.js` — Modernized audit viewer consuming `/api/v1/audit/logs` and triggering `/api/v1/audit/verify` with cryptographic violation reports.

---

## 6. Dependencies & Compatibility Concerns

- **React Version:** React 18.2.0 is installed. No upgrade required.
- **Node & NPM:** Running Node v20.16.0 / npm 10.6.0 via Maven plugin.
- **Dependencies:** All necessary libraries (`axios`, `react-router-dom`) are already present in `package.json`. No unapproved third-party packages need to be installed.
- **Backend Compatibility:** The backend runs under `spring.jpa.hibernate.ddl-auto=validate` and Java 25 LTS with all 152 regression tests passing. No backend code or database schemas will be altered.

---

## 7. Files to Be Created or Modified

### 7.1 Existing Files to Be Modified
- `springapp/springbackend/src/services/api.js` (enhance RFC 7807 problem details parsing and baseURL handling)
- `springapp/springbackend/src/services/AuthContext.js` (add role & department helper functions)
- `springapp/springbackend/src/App.js` (upgrade routing matrix to support employee, specialist, approver, and audit views)
- `springapp/springbackend/src/App.css` (add modern enterprise styling, responsive layout tokens, and badge colors)
- `docs/api/task-07e-rest-api-contracts.md` (correct queue and approval route paths to match live controllers)

### 7.2 New Files to Be Created
- `springapp/springbackend/src/services/ticketService.js` (API abstraction for catalog, tickets, comments, queues, approvals, audit)
- `springapp/springbackend/src/components/layout/AppLayout.js`
- `springapp/springbackend/src/components/layout/Navbar.js`
- `springapp/springbackend/src/components/layout/Sidebar.js`
- `springapp/springbackend/src/components/common/Badge.js`
- `springapp/springbackend/src/components/common/Pagination.js`
- `springapp/springbackend/src/components/common/LoadingSpinner.js`
- `springapp/springbackend/src/components/common/EmptyState.js`
- `springapp/springbackend/src/components/catalog/ServiceCatalog.js`
- `springapp/springbackend/src/components/tickets/CreateTicketModal.js`
- `springapp/springbackend/src/components/tickets/MyTickets.js`
- `springapp/springbackend/src/components/tickets/TicketDetail.js`
- `springapp/springbackend/src/components/tickets/TicketComments.js`
- `springapp/springbackend/src/components/queue/DepartmentQueue.js`
- `springapp/springbackend/src/components/approvals/ApprovalQueue.js`
- `springapp/springbackend/src/components/audit/AuditLedger.js`
- `springapp/springbackend/src/components/dashboard/EmployeeDashboard.js`
- `springapp/springbackend/src/App.test.js` (comprehensive frontend test suite)
- `docs/implementation/task-07f-frontend-integration-report.md`

---

## 8. Potential Integration Risks & Mitigation Strategies

| Risk | Severity | Mitigation Strategy |
|:---|:---:|:---|
| **Stale Version Conflicts (OCC / 409)** | Medium | Display clear warning banner when `version` token is outdated; provide single-click "Refresh Ticket" button. |
| **CSRF Rejection on State Mutations** | Medium | Axios interceptor reads `XSRF-TOKEN` cookie and attaches `X-XSRF-TOKEN` header automatically on all POST requests. |
| **Specialist Department Mismatch (403)** | Low | Default specialist queue navigation to user's assigned department in profile (`currentUser.department`). |
| **Anti-Self-Approval Rejection** | Low | Disable approve/reject buttons and display informative tooltip if `task.ticketRequester === currentUser.username`. |
| **Internal Comment Leakage** | Critical | Requesters receive pre-filtered comments from backend; UI reinforces this with clear "Internal Discussion Note" visual pill. |

---

## 9. Conclusion & Next Steps

The reconnaissance phase is complete. The repository is ready to proceed to **Stage B (API Client & Auth Foundation)** and **Stage C (Application Shell)**.

