# EPAMS — P7 Final Frontend & Backend Contract Verification

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P7 — Consolidated Final Contract Audit  
**Date:** 2026-11-18  
**Classification:** `VERIFIED`  

---

## 1. Authoritative REST API & Frontend Field Mapping Audit

Inspected directly against Spring Boot controllers/DTOs and React services/components (`ticketService.js`, `CreateTicketModal.js`, `ServiceCatalog.js`, `EmployeeDashboard.js`, `MyTickets.js`, `TicketDetail.js`, `DepartmentQueue.js`, `ApprovalQueue.js`, `AuditLedger.js`):

| Feature | Backend Endpoint & DTO Contract | Frontend Consumer & Payload / Read Fields | P7.Final Fix / Status |
| :--- | :--- | :--- | :---: |
| **Service Catalog** | `GET /api/v1/catalog` → `Page<ServiceCatalogItemDTO>` (`id`, `itemCode`, `title`, `category`, `department`, `slaTargetHours`, `requiresManagerApproval`, `requiresSecondaryApproval`, `active`) | `ServiceCatalog.js`, `CreateTicketModal.js` (`title`, `department`, `slaTargetHours`) | `VERIFIED` — Fixed `GAP-P7-01`: `CreateTicketModal.js` now reads `preselectedItem?.department` (previously fell back to `'IT'` for HR/Facilities preselected items); locked down by `P9FrontendQa.test.js` (`P9-FE-06`). |
| **Ticket Creation** | `POST /api/v1/tickets` → `CreateTicketRequestDTO` (`catalogItemId`, `department`, `priority`, `description`) → `201 Created` (`ServiceTicketDTO`) | `CreateTicketModal.js` → `ticketService.submitTicket({ catalogItemId, department, priority, description })` | `VERIFIED` — Requester derived strictly server-side from `Principal`; `submitting` state prevents double-submit. |
| **Priority Tiers** | `TicketPriority` enum: `LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL` | `CreateTicketModal.js`, `Badge.js`, `AnalyticsDashboard.js` | `VERIFIED` (All 5 tiers aligned end-to-end). |
| **Status Update** | `POST /api/v1/tickets/{id}/status` → `TicketStatusUpdateRequestDTO` (`status`, `reason`, `version`) | `TicketDetail.js` → `ticketService.updateTicketStatus` sends `{ status, reason, version }` | `VERIFIED` — Optimistic lock (`@Version` → `409 Conflict`) enforced. |
| **Ticket Assignment** | `POST /api/v1/tickets/{id}/assign` → `TicketAssignRequestDTO` (`assigneeId`) | `DepartmentQueue.js`, `TicketDetail.js` → `ticketService.assignTicket(id, currentUser.id)` | `VERIFIED` — Server validates department scope. |
| **Cancel / Reopen** | `POST /api/v1/tickets/{id}/cancel` & `/reopen` → `TicketActionRequestDTO` (`reason`, `version`) | `TicketDetail.js` → `ticketService.cancelTicket` / `reopenTicket` | `VERIFIED` — 15-char reason boundary + 48-business-hour inclusive reopen window enforced. |
| **Comments & Privacy** | `GET/POST /api/v1/tickets/{id}/comments` → `TicketCommentRequestDTO` (`commentText`, `isInternal`) | `TicketComments.js` → `ticketService.addComment` | `VERIFIED` — Non-specialists blocked from `isInternal=true` (`403`, `SEC-P9-02`) and internal notes stripped on read. |
| **Department Queues** | `GET /api/v1/departments/{department}/queue` (`page`, `size`, `status`) | `DepartmentQueue.js` → `ticketService.getDepartmentQueue` | `VERIFIED` — Cross-department access rejected with `403`. |
| **Approval Decisions** | `POST /api/v1/approvals/{taskId}/decide` → `ApprovalDecisionRequestDTO` (`decision`, `reason`) | `ApprovalQueue.js` → `ticketService.decideApproval` sends `{ decision, reason }` | `VERIFIED` — Anti-self-approval + BOLA-protected history (`SEC-P9-04`). |
| **Dashboard Pagination** | `GET /api/v1/tickets/my` → Spring `Page<ServiceTicketDTO>` (`content`, `totalElements`, `totalPages`) | `EmployeeDashboard.js:30` reads `ticketRes.totalElements` | `VERIFIED` |
| **Cookies, CSRF & Auth Refresh** | `accessToken` (`HttpOnly`, `SameSite=Lax`, `Path=/`), `refreshToken` (`HttpOnly`, `Path=/api/auth`), `XSRF-TOKEN` (`HttpOnly=false`) | `api.js` (`withCredentials: true`, `X-XSRF-TOKEN` request interceptor, silent `/auth/refresh`, `session-expired` event) | `VERIFIED` (Live HTTP Tomcat + `SpaCsrfTokenRequestHandler` + `NullAuthenticatedSessionStrategy`, `SEC-P9-06`). |
