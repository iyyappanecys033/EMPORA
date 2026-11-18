# EPAMS REST API Contracts & Endpoint Specification (v1)

**Specification Version:** 1.0.0  
**Phase:** Phase 2 — Testing Foundation & Workflow Orchestration  
**Task:** Task 07E — REST API Contracts, Security Hardening & DTO Validation  
**Date:** November 2026  
**Status:** Approved & Enforced  

---

## 1. Overview & Standard Conventions

All modern EPAMS service management operations are exposed under the `/api/v1` namespace. Legacy authentication and user management endpoints are maintained under `/api/auth` and `/api/users` for backwards compatibility with Phase 1 IAM contracts.

### 1.1 Base URL & Content Negotiation
- **Base URL:** `http://localhost:8080/api/v1`
- **Request Content-Type:** `application/json;charset=UTF-8`
- **Response Content-Type:** `application/json;charset=UTF-8`
- **Error Content-Type:** `application/problem+json` (RFC 7807)

### 1.2 Authentication & Authorization Headers
Requests must supply credentials using either:
1. **HttpOnly Cookie:** `Cookie: accessToken=<JWT_TOKEN>` (accompanied by `X-XSRF-TOKEN` header for state-mutating requests `POST`, `PUT`, `DELETE`).
2. **Authorization Header:** `Authorization: Bearer <JWT_TOKEN>`.

### 1.3 Pagination Standards
All collection queries support standardized, zero-indexed, bounded pagination:
- `page`: Integer $\ge 0$ (default: `0`)
- `size`: Integer bounded between 1 and 100 (default: `20`, clamped if input $< 1$ or $> 100$)
- `sort`: String format `field,direction` (e.g. `createdAt,desc`)

---

## 2. Global Error Format (RFC 7807 Problem Details)

All error responses from `/api/v1/*` conform strictly to RFC 7807 `application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for request payload",
  "instance": "/api/v1/tickets",
  "timestamp": "2026-11-15T20:45:00Z",
  "errors": {
    "catalogItemId": "catalogItemId is mandatory",
    "justification": "justification must not be blank"
  }
}
```

### Standard HTTP Status Codes
| HTTP Status | Title | Description / Trigger Scenario |
|:---|:---|:---|
| **200 OK** | Success | Request succeeded; payload returned in response body. |
| **201 Created** | Created | Resource successfully created; returns resource DTO and `Location` header. |
| **204 No Content** | No Content | Action succeeded; no body content returned. |
| **400 Bad Request** | Bad Request | DTO validation failure, malformed JSON, or missing parameters. |
| **401 Unauthorized** | Unauthorized | Missing, expired, or invalid JWT authentication token. |
| **403 Forbidden** | Forbidden | Insufficient permissions, cross-department breach, or anti-self-approval violation. |
| **404 Not Found** | Not Found | Target resource ID, catalog item, or user not found. |
| **409 Conflict** | Conflict | Optimistic locking failure (concurrent edit) or invalid state transition. |

---

## 3. Service Catalog API (`/api/v1/catalog`)

### 3.1 List Active Catalog Items
- **Method:** `GET`
- **Path:** `/api/v1/catalog`
- **Access:** Authenticated (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_RESOLVER`, `ROLE_DEPT_HEAD`, `ROLE_ADMIN`, `ROLE_AUDITOR`)
- **Query Parameters:** None
- **Response Status:** `200 OK`
- **Response Body:** Array of `ServiceCatalogItemDTO`

```json
[
  {
    "id": 1,
    "itemCode": "IT-LAPTOP-DEV",
    "name": "Developer Laptop High-Performance",
    "category": "Hardware",
    "targetDepartment": "IT",
    "description": "32GB RAM, 1TB SSD Developer Workstation",
    "baseCost": 1850.00,
    "slaHours": 24,
    "requiresManagerApproval": true,
    "requiresDepartmentHeadApproval": false,
    "active": true
  }
]
```

### 3.2 Get Catalog Item by ID
- **Method:** `GET`
- **Path:** `/api/v1/catalog/{id}`
- **Access:** Authenticated
- **Response Status:** `200 OK` (or `404 Not Found`)

---

## 4. Ticket Lifecycle API (`/api/v1/tickets`)

### 4.1 Submit New Service Ticket
- **Method:** `POST`
- **Path:** `/api/v1/tickets`
- **Access:** Authenticated Employee (`hasRole('EMPLOYEE')` or any higher role)
- **Request Body:** `CreateTicketRequestDTO`
  ```json
  {
    "catalogItemId": 1,
    "targetDepartment": "IT",
    "priority": "HIGH",
    "justification": "Primary workstation hardware failure requires immediate replacement"
  }
  ```
- **Validation Rules:**
  - `catalogItemId`: `@NotNull(message = "catalogItemId is mandatory")`
  - `targetDepartment`: `@NotBlank(message = "targetDepartment is mandatory")`, `@Size(max = 64)`
  - `priority`: `@NotNull(message = "priority is mandatory")` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
  - `justification`: `@NotBlank(message = "justification must not be blank")`, `@Size(max = 2000)`
- **Response Status:** `201 Created`
- **Response Body:** `ServiceTicketDTO`

### 4.2 Get Ticket by ID
- **Method:** `GET`
- **Path:** `/api/v1/tickets/{id}`
- **Access:** `@PreAuthorize("@ticketSecurityEvaluator.canAccessTicket(#id, authentication)")`
  - Requester who created the ticket
  - Assigned resolver
  - Resolver belonging to ticket's target department (`ROLE_RESOLVER`, `ROLE_IT_SUPPORT`, `ROLE_FACILITIES_CREW`, `ROLE_HR_SPECIALIST`)
  - Reporting manager with pending approval task
  - Global `ROLE_ADMIN` or `ROLE_AUDITOR`
- **Response Status:** `200 OK` / `403 Forbidden` / `404 Not Found`

### 4.3 Update Ticket Status
- **Method:** `POST`
- **Path:** `/api/v1/tickets/{id}/status`
- **Access:** `@PreAuthorize("@ticketSecurityEvaluator.canManageTicket(#id, authentication)")`
- **Request Body:** `TicketStatusUpdateRequestDTO`
  ```json
  {
    "newStatus": "IN_PROGRESS",
    "reason": "Commencing diagnostics on workstation hardware",
    "version": 1
  }
  ```
- **Validation Rules:**
  - `newStatus`: `@NotNull(message = "newStatus is mandatory")`
  - `reason`: `@Size(max = 1000)`
  - `version`: `@NotNull(message = "version is mandatory for optimistic concurrency control")`
- **Response Status:** `200 OK` / `400 Bad Request` / `403 Forbidden` / `409 Conflict`

### 4.4 Assign Ticket
- **Method:** `POST`
- **Path:** `/api/v1/tickets/{id}/assign`
- **Access:** Resolver / Dept Head / Admin belonging to target department
- **Request Body:** `TicketAssignRequestDTO`
  ```json
  {
    "assigneeUserId": 5
  }
  ```
- **Validation Rules:**
  - `assigneeUserId`: `@NotNull(message = "assigneeUserId is mandatory")`
- **Response Status:** `200 OK`

### 4.5 Cancel Ticket
- **Method:** `POST`
- **Path:** `/api/v1/tickets/{id}/cancel`
- **Access:** Requester or Admin
- **Request Body:** `TicketActionRequestDTO`
  ```json
  {
    "reason": "Duplicate request submitted in error"
  }
  ```
- **Response Status:** `200 OK`

### 4.6 Reopen Ticket
- **Method:** `POST`
- **Path:** `/api/v1/tickets/{id}/reopen`
- **Access:** Requester or Admin (within 48 accumulated business hours from `RESOLVED` status in `Asia/Kolkata`, Mon–Fri 09:00–18:00, excluding weekends and configured holidays; exact deadline instant inclusive, `now <= reopenDeadline`)
- **Request Body:** `TicketActionRequestDTO`
- **Response Status:** `200 OK` / `400 Bad Request` / `403 Forbidden`

---

## 5. Comments & Contextual Collaboration API

### 5.1 Add Ticket Comment
- **Method:** `POST`
- **Path:** `/api/v1/tickets/{id}/comments`
- **Access:** `@PreAuthorize("@ticketSecurityEvaluator.canAccessTicket(#id, authentication)")`
- **Request Body:** `TicketCommentRequestDTO`
  ```json
  {
    "commentText": "Replacement hardware part has been dispatched by logistics vendor",
    "isInternal": true
  }
  ```
- **Validation Rules:**
  - `commentText`: `@NotBlank(message = "commentText is mandatory")`, `@Size(max = 4000)`
  - `isInternal`: Boolean flag (requesters cannot set internal comments)
- **Response Status:** `200 OK`

### 5.2 List Ticket Comments
- **Method:** `GET`
- **Path:** `/api/v1/tickets/{id}/comments`
- **Access:** `@PreAuthorize("@ticketSecurityEvaluator.canAccessTicket(#id, authentication)")`
- **Behavior:** Requesters receive only public comments (`isInternal == false`). Resolvers, Managers, and Admins receive both public and internal comments.
- **Response Status:** `200 OK`

---

## 6. Department Queue & Dispatch API (`/api/v1/departments`)

### 6.1 View Department Ticket Queue
- **Method:** `GET`
- **Path:** `/api/v1/departments/{department}/queue`
- **Access:** `@PreAuthorize("@ticketSecurityEvaluator.canAccessDepartmentQueue(#department, authentication)")`
  - Restricts access strictly to resolvers assigned to the requested department or `ROLE_ADMIN`.
- **Query Parameters:**
  - `status`: Optional filter (`NEW`, `ASSIGNED`, `IN_PROGRESS`, etc.)
  - `page`: Page index (default: `0`)
  - `size`: Page size (default: `20`, max: `100`)
- **Response Status:** `200 OK` / `403 Forbidden`
- **Response Body:** Spring Data `Page<ServiceTicketDTO>`

---

## 7. Approval Workflow Engine API (`/api/v1/approvals`)

### 7.1 List My Pending Approval Tasks
- **Method:** `GET`
- **Path:** `/api/v1/approvals/pending`
- **Access:** Authenticated Manager, Dept Head, or Admin (`ROLE_MANAGER`, `ROLE_DEPT_HEAD`, `ROLE_ADMIN`)
- **Query Parameters:** None
- **Response Status:** `200 OK`
- **Response Body:** Array of `ApprovalTaskDTO`

### 7.2 Submit Approval Decision
- **Method:** `POST`
- **Path:** `/api/v1/approvals/{taskId}/decide`
- **Access:** Assigned Approver or Admin
- **Request Body:** `ApprovalDecisionRequestDTO`
  ```json
  {
    "decision": "APPROVED",
    "decisionNotes": "Approved within Q4 hardware operational budget allowance"
  }
  ```
- **Validation Rules:**
  - `decision`: `@NotNull(message = "decision is mandatory")` (`APPROVED`, `REJECTED`)
  - `decisionNotes`: `@Size(max = 1000)`
- **Security Check:** Anti-Self-Approval guard rejects decisions where the approver equals the ticket requester (`403 Forbidden`).
- **Response Status:** `200 OK` / `403 Forbidden` / `404 Not Found`

---

## 8. Cryptographic Audit Verification API (`/api/v1/audit`)

### 8.1 Verify Complete Cryptographic Audit Chain
- **Method:** `GET`
- **Path:** `/api/v1/audit/verify`
- **Access:** Dedicated Auditor or Admin (`@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")`)
- **Response Status:** `200 OK` / `403 Forbidden`
- **Response Body:** `AuditVerificationReport`
  ```json
  {
    "valid": true,
    "totalChainedRecords": 42,
    "totalUnchainedRecords": 105,
    "lastSequence": 42,
    "headHash": "b3f8a42c90e...",
    "violations": []
  }
  ```

