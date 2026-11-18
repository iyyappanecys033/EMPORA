# EPAMS P7 — Repository & Integration Baseline Document

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P7 — Complete Frontend Integration, Contract Remediation & Final Acceptance  
**Date:** 2026-11-17  
**Author:** Principal Software Architect & QA Automation Lead  
**Classification:** `[VERIFIED]`  

---

## 1. Actual System Architecture

EPAMS is structured as a **Modular Monolith** backend paired with a **Single Page Application (SPA)** React frontend:
- **Backend:** Java 25 LTS running Spring Boot 3.4.12, Spring Data JPA / Hibernate 6.6.36.Final, Spring Security 6.x, and Flyway 10.20.1.
- **Frontend:** React 18.2.0, Create React App 5.0.1, React Router DOM 6.11.1, Axios 1.4.0, packaged via `frontend-maven-plugin` into `springapp/springbackend`.
- **Database Architecture:** Production MySQL 8.0+ schema governed by Flyway V1–V3 migrations (`V1__init_iam_schema.sql`, `V2__service_catalog_and_tickets.sql`, `V3__approvals_and_audit_chain.sql`). Automated integration tests execute against in-memory H2 in MySQL compatibility mode. Hibernate runs with `ddl-auto=validate`.
- **Authentication & CSRF:** HTTP-Only Cookie-based JWT tokens (`accessToken` on path `/`, `refreshToken` on path `/api/auth`) with `withCredentials: true`. Double-submit CSRF protection using `CookieCsrfTokenRepository.withHttpOnlyFalse()` with client-side header mirroring (`X-XSRF-TOKEN`).

---

## 2. Repository Structure

```text
EPAMS/
├── docs/
│   ├── api/                              # REST API contract specs
│   ├── architecture/                     # ADRs, HLD, LLD
│   ├── database/                         # Database design & schemas
│   ├── implementation/                   # Task reports & baseline audits
│   └── security/                         # Threat models & security reviews
├── springapp/
│   ├── pom.xml                           # Master Maven descriptor (Java 25, Boot 3.4.12)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/examly/springapp/
│   │   │   │   ├── controller/           # 14 REST Controllers (/api/* and /api/v1/*)
│   │   │   │   ├── dto/                  # Request / Response transfer objects
│   │   │   │   ├── event/                # Spring event publishers / listeners
│   │   │   │   ├── exception/            # Global RFC 7807 problem details handlers
│   │   │   │   ├── model/                # JPA Domain Entities & Enums (11 states, 5 priorities)
│   │   │   │   ├── repository/           # Spring Data JPA interfaces (13 repos)
│   │   │   │   ├── security/             # JWT filter, Cookie auth, TicketSecurityEvaluator
│   │   │   │   └── service/              # Workflow, SLA calendar, audit-chain engines
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── application-dev.properties
│   │   │       ├── application-mysql.properties
│   │   │       └── db/migration/         # Flyway V1, V2, V3 migrations
│   │   └── test/java/com/examly/springapp/ # 9 regression test suites (152 tests)
│   └── springbackend/                    # React 18 frontend
│       ├── package.json                  # Dependencies & scripts
│       ├── public/                       # HTML template
│       └── src/
│           ├── components/               # Common atoms, layout, and domain workspaces
│           ├── services/                 # api.js, ticketService.js, AuthContext.js
│           ├── App.js                    # Route guards and master routing
│           ├── App.css                   # Responsive layout and isolated form styles
│           └── App.test.js               # 21 automated component and contract tests
```

---

## 3. Frontend / Backend Boundary Inventory

| Domain Layer | Backend Controller | Base Route | Frontend Service Facade |
| :--- | :--- | :--- | :--- |
| **Authentication** | `AuthController.java` | `/api/auth` | `AuthContext.js` (`/login`, `/refresh`, `/logout`) |
| **User Identity** | `UserAccountController.java` | `/api/users` | `AuthContext.js` (`/users/me`) |
| **Service Catalog** | `ServiceCatalogController.java` | `/api/v1/catalog` | `ticketService.getCatalogItems()` |
| **Service Tickets** | `TicketController.java` | `/api/v1/tickets` | `ticketService.submitTicket()`, `getMyTickets()` |
| **Ticket Actions** | `TicketController.java` | `/api/v1/tickets/{id}/*` | `assignTicket()`, `updateTicketStatus()`, `cancelTicket()`, `reopenTicket()` |
| **Comments** | `TicketController.java` | `/api/v1/tickets/{id}/comments` | `ticketService.getComments()`, `addComment()` |
| **Department Queue** | `DepartmentQueueController.java`| `/api/v1/departments/{dept}/queue` | `ticketService.getDepartmentQueue()` |
| **Approvals** | `ApprovalController.java` | `/api/v1/approvals` | `ticketService.getPendingApprovals()`, `decideApproval()` |
| **Audit Ledger** | `AuditApiController.java` | `/api/v1/audit` | `ticketService.getAuditLogs()`, `verifyAuditChain()` |

---

## 4. Confirmed Defects Remediation Summary

| Defect ID | Component | Root Cause | Remediation Applied | Status |
| :---: | :--- | :--- | :--- | :---: |
| **FN-01** | `CreateTicketModal.js` | Payload keys `targetDepartment` and `justification` mismatched backend `@NotBlank` fields | Remapped payload to `department` and `description` | **RESOLVED & VERIFIED** |
| **FN-02** | `ticketService.js` | Payload key `newStatus` mismatched backend `@NotNull` field | Remapped payload to `status` | **RESOLVED & VERIFIED** |
| **FN-03** | `ticketService.js` | Payload key `decisionNotes` mismatched backend `reason` field ($\ge 15$ chars) | Remapped payload to `reason` | **RESOLVED & VERIFIED** |
| **FN-04** | `ServiceCatalog.js` | Field names `item.name`, `item.targetDepartment`, `item.slaHours` mismatched DTO | Remapped accesses to `title`, `department`, `slaTargetHours`, `estimatedCost` | **RESOLVED & VERIFIED** |
| **FN-05** | `Badge.js` & `CreateTicketModal.js` | `URGENT` priority tier was missing from color palette and dropdown | Added `URGENT` to `PRIORITY_COLORS` and creation dropdown | **RESOLVED & VERIFIED** |
| **FN-06** | `EmployeeDashboard.js` | Used first page `tickets.length` (max 5) instead of total elements | Bound to `ticketRes.totalElements` for authoritative total | **RESOLVED & VERIFIED** |
| **UI-01** | `ApprovalQueue.js` | Checked `task.ticketRequester` instead of DTO field `task.requesterUsername` | Remapped anti-self-approval to `task.requesterUsername` | **RESOLVED & VERIFIED** |

---

## 5. Baseline Execution Evidence

- **Backend Surefire Regression Suite:** 152 / 152 passed across all 9 test classes (0 failures, 0 errors, 0 skipped, Maven exit code 0).
- **Frontend Jest Suite:** 21 / 21 passed across all 5 test suites (0 failures, 0 skipped, 24.08s).
- **Frontend Production Build:** `npm run build` completed cleanly, 0 ESLint warnings, 0 compile errors.

