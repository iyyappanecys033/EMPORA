# EPAMS P7 — Master REST API Contract Matrix

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P7 — Complete Frontend Integration & Contract Remediation  
**Date:** 2026-11-17  
**Status:** FULLY RECONCILED & VERIFIED `[VERIFIED]`  

---

## 1. Authentication & Session Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Auth & Permissions | CSRF Protection |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST /api/auth/login` | `AuthController.java` | `LoginRequest` (`username`, `password`) | `AuthResponse` + Set-Cookie (`accessToken`, `refreshToken`) (200 OK) | Public | Not required |
| `POST /api/auth/refresh` | `AuthController.java` | Cookie: `refreshToken` | `AuthResponse` + renewed Set-Cookie (200 OK / 401 Unauthorized) | Public / Cookie | Not required |
| `POST /api/auth/logout` | `AuthController.java` | Cookie: `refreshToken` | `AuthResponse` + clear cookie headers (200 OK) | Authenticated | `X-XSRF-TOKEN` |
| `GET /api/users/me` | `UserAccountController.java` | None | `UserResponseDTO` (`id`, `username`, `roles`, `department`, `fullName`, `employeeId`, `designation`) (200 OK) | Authenticated | None (Safe GET) |

---

## 2. Service Catalog Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Auth & Permissions |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/catalog` | `ServiceCatalogController.java`| Query: `department`, `page`, `size` | `Page<ServiceCatalogItemDTO>` (`id`, `itemCode`, `title`, `category`, `department`, `description`, `estimatedCost`, `slaTargetHours`, `requiresManagerApproval`, `requiresSecondaryApproval`, `isActive`) (200 OK) | Authenticated |
| `GET /api/v1/catalog/{id}` | `ServiceCatalogController.java`| Path: `id` | `ServiceCatalogItemDTO` (200 OK / 404 Not Found) | Authenticated |
| `GET /api/v1/catalog/code/{code}` | `ServiceCatalogController.java`| Path: `code` | `ServiceCatalogItemDTO` (200 OK / 404 Not Found) | Authenticated |

---

## 3. Service Ticket Lifecycle Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Auth & Permissions |
| :--- | :--- | :--- | :--- | :--- |
| `POST /api/v1/tickets` | `TicketController.java` | `CreateTicketRequestDTO` (`catalogItemId`, `department`, `priority`, `description`) | `ServiceTicketDTO` (201 Created / 400 Bad Request) | Authenticated Requester |
| `GET /api/v1/tickets/my` | `TicketController.java` | Query: `status`, `page`, `size` | `Page<ServiceTicketDTO>` (200 OK) | Authenticated Requester |
| `GET /api/v1/tickets/{id}` | `TicketController.java` | Path: `id` | `ServiceTicketDTO` (`id`, `ticketNumber`, `catalogItemId`, `catalogItemTitle`, `requesterId`, `requesterUsername`, `assigneeId`, `assigneeUsername`, `department`, `priority`, `status`, `description`, `createdAt`, `resolvedAt`, `closedAt`, `slaTargetDate`, `totalPausedMinutes`, `version`, `slaStatus`) (200 OK / 403 Forbidden / 404) | Requester / Resolver / Admin |
| `POST /api/v1/tickets/{id}/assign` | `TicketController.java` | Body: `TicketAssignRequestDTO` (`assigneeId`) | `ServiceTicketDTO` (200 OK / 403 Forbidden) | Department Resolver / Admin |
| `POST /api/v1/tickets/{id}/status` | `TicketController.java` | Body: `TicketStatusUpdateRequestDTO` (`status`, `reason`, `version`) | `ServiceTicketDTO` (200 OK / 400 Bad Request / 409 Conflict) | Assignee / Specialist / Admin |
| `POST /api/v1/tickets/{id}/cancel` | `TicketController.java` | Body: `TicketActionRequestDTO` (`reason`, `version`) | `ServiceTicketDTO` (200 OK / 400 / 409) | Requester / Admin |
| `POST /api/v1/tickets/{id}/reopen` | `TicketController.java` | Body: `TicketActionRequestDTO` (`reason`, `version`) | `ServiceTicketDTO` (200 OK / 400 / 409) | Requester / Admin |

---

## 4. Contextual Comments Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Privacy Boundary |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/tickets/{id}/comments` | `TicketController.java` | Path: `id` | `List<TicketCommentDTO>` (`id`, `ticketId`, `authorId`, `authorUsername`, `commentText`, `isInternal`, `createdAt`) | Requesters receive `isInternal == false` only. Specialists receive all. |
| `POST /api/v1/tickets/{id}/comments` | `TicketController.java` | Body: `TicketCommentRequestDTO` (`commentText`, `isInternal`) | `TicketCommentDTO` (200 OK / 400 / 403) | Ticket Viewers / Specialists |

---

## 5. Department Queue Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Scope Boundary |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/departments/{department}/queue` | `DepartmentQueueController.java` | Path: `department`, Query: `status`, `page`, `size` | `Page<ServiceTicketDTO>` (200 OK / 403 Forbidden) | Limited to Specialists belonging to `{department}` or Admin. Cross-dept blocked. |

---

## 6. Managerial Approval Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Business Rules |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/approvals/pending` | `ApprovalController.java` | None | `List<ApprovalTaskDTO>` (`id`, `ticketId`, `ticketNumber`, `approvalStage`, `status`, `decisionReason`, `approverUsername`, `requesterUsername`, `newTicketStatus`, `createdAt`, `version`) | Scoped to authenticated approver's assigned tasks. |
| `GET /api/v1/approvals/tickets/{ticketId}` | `ApprovalController.java` | Path: `ticketId` | `List<ApprovalTaskDTO>` | Audit trail of all approval steps for ticket. |
| `POST /api/v1/approvals/{taskId}/decide` | `ApprovalController.java` | Body: `ApprovalDecisionRequestDTO` (`decision`, `reason`, `version`) | `ApprovalTaskDTO` (200 OK / 400 Bad Request / 409 Conflict) | Rejection requires `reason` length $\ge 15$ chars. Anti-self-approval enforced. |

---

## 7. Cryptographic Audit Ledger Contracts

| HTTP Method & Path | Controller Class | Request Payload / Params | Response Payload / Status | Authorization |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/audit/logs` | `AuditApiController.java` | Query: `page`, `size` | `Page<AuditLogDTO>` (`id`, `sequenceNumber`, `previousHash`, `currentHash`, `actorUsername`, `action`, `targetUsername`, `details`, `timestamp`) | `ROLE_ADMIN`, `ROLE_AUDITOR` |
| `GET /api/v1/audit/verify` | `AuditApiController.java` | None | `AuditVerificationReport` (`valid`, `totalChainedRecords`, `totalUnchainedRecords`, `lastSequence`, `headHash`, `violations`) | `ROLE_ADMIN`, `ROLE_AUDITOR` |

---

## 8. RFC 7807 Standard Error Details

Every error response returned by Spring Boot follows RFC 7807 Problem Details:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for request",
  "timestamp": "2026-11-17T20:00:00Z",
  "fieldErrors": [
    { "field": "description", "message": "Description is required" }
  ]
}
```
Client parser `formatApiError()` in `src/services/api.js` extracts `title`, `detail`, `status`, and `errors[]`, visualized through `src/components/common/Alert.js`.

