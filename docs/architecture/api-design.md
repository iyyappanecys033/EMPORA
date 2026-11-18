# OpenAPI 3.0 RESTful API Specification
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-HLD-API-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved Architecture Baseline  
**Author:** Principal API Architect & Senior Full-Stack Engineer  

---

## 1. REST API Design Standards & Conventions

1. **Base URL & Versioning:** All endpoints are strictly versioned under `/api/v1/`.
2. **Authentication & Transport Security:** All traffic mandates HTTPS. Authentication uses Bearer JWT tokens in the `Authorization: Bearer <token>` header, or silent cookies.
3. **CSRF Protection:** State-changing requests (`POST`, `PUT`, `PATCH`, `DELETE`) require the `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie (`ADR-002`).
4. **Data Formats:** All request and response bodies use `application/json;charset=UTF-8`. All timestamps conform to ISO 8601 (`YYYY-MM-DDTHH:mm:ss.SSSZ`).
5. **Pagination & Sorting:** Collection endpoints support standard Spring Data pagination: `page` (0-indexed, default 0), `size` (default 20, max 100), `sort` (e.g. `createdAt,desc`).
6. **Optimistic Concurrency & Conflict Handling:** When a ticket or approval task version mismatch occurs during mutation, the server immediately returns **HTTP 409 Conflict** (`ADR-008`).

---

## 2. Standardized Error Response Schema (RFC 7807 Compliant)

All error responses return a uniform JSON envelope:

```json
{
  "timestamp": "2026-11-13T12:00:00.000Z",
  "status": 409,
  "error": "Conflict",
  "message": "The ticket was modified by another user. Please refresh to view the latest state before retrying.",
  "path": "/api/v1/tickets/42/status",
  "validationErrors": null
}
```

---

## 3. Detailed Endpoint Specifications

### 3.1 Authentication & Session Management (`core.iam`)

#### `POST /api/v1/auth/login`
* **Description:** Authenticates user credentials, resets failed attempts, returns JWT access token, and sets rotating refresh token cookie (`FR-001`).
* **Authorization:** Anonymous (`permitAll`).
* **Request Body:**
  ```json
  {
    "username": "john.doe",
    "password": "SecurePassword123!"
  }
  ```
* **Validation:** `username` (@NotBlank), `password` (@NotBlank).
* **Response (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "type": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 12,
      "username": "john.doe",
      "email": "john.doe@company.com",
      "roles": ["ROLE_EMPLOYEE"]
    }
  }
  ```
* **Headers Returned:** `Set-Cookie: refreshToken=...; HttpOnly; Path=/api/v1/auth; SameSite=Lax; Secure`
* **Error Codes:** `400 Bad Request`, `401 Unauthorized` (Invalid credentials), `423 Locked` (Account locked after 5 failed attempts per `FR-002`).

#### `POST /api/v1/auth/refresh`
* **Description:** Validates the `refreshToken` cookie, rotates the token in the database, and issues a fresh 15-minute JWT (`FR-003`).
* **Authorization:** Anonymous (Validated via Cookie).
* **Response (200 OK):** Same structure as login response.
* **Error Codes:** `401 Unauthorized` (Token expired, revoked, or missing).

#### `POST /api/v1/auth/logout`
* **Description:** Revokes active refresh token in database and clears cookie (`FR-004`).
* **Authorization:** Authenticated (`ROLE_EMPLOYEE`+).
* **Response (200 OK):** `{"message": "Logged out successfully"}`.

---

### 3.2 User & Profile Management (`core.profile`)

#### `GET /api/v1/profiles/me`
* **Description:** Retrieves self-service profile details of the authenticated employee (`FR-008`).
* **Authorization:** Authenticated (`ROLE_EMPLOYEE`+).
* **Response (200 OK):**
  ```json
  {
    "id": 12,
    "employeeId": "EMP-1042",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@company.com",
    "department": "ENGINEERING",
    "designation": "Software Engineer",
    "salary": 1250000.00,
    "reportingManager": "jane.smith",
    "dateOfJoining": "2024-03-15",
    "emergencyContact": "+91-9876543210"
  }
  ```

#### `GET /api/v1/profiles`
* **Description:** Paginated organization directory search (`FR-007`). Redacts sensitive salary details for non-HR callers (`FR-009`).
* **Authorization:** Authenticated (`ROLE_EMPLOYEE`+).
* **Query Parameters:** `search` (name or employeeId), `department`, `page`, `size`, `sort`.
* **Response (200 OK):**
  ```json
  {
    "content": [
      {
        "id": 14,
        "employeeId": "EMP-1088",
        "firstName": "Alice",
        "lastName": "Vance",
        "department": "IT",
        "designation": "Systems Administrator",
        "salary": null,
        "reportingManager": "bob.wilson"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 245,
    "totalPages": 13
  }
  ```

#### `POST /api/v1/users/{id}/unlock`
* **Description:** Administrative action to unlock an account locked due to excessive failed logins (`FR-005`).
* **Authorization:** `ROLE_ADMIN` only.
* **Response (200 OK):** `{"message": "User account unlocked successfully"}`.
* **Error Codes:** `403 Forbidden`, `404 Not Found`.

---

### 3.3 Service Catalog (`module.ticket`)

#### `GET /api/v1/catalog`
* **Description:** Retrieves all active service catalog items categorized across IT, HR, Facilities, and Finance (`FR-010`).
* **Authorization:** Authenticated (`ROLE_EMPLOYEE`+).
* **Query Parameters:** `category`, `department`.
* **Response (200 OK):**
  ```json
  [
    {
      "id": 4,
      "itemCode": "IT-HW-LAPTOP",
      "title": "Standard Developer Laptop (MacBook / Dell)",
      "category": "HARDWARE",
      "department": "IT",
      "description": "High-performance developer workstation with 32GB RAM.",
      "estimatedCost": 150000.00,
      "slaTargetHours": 48,
      "requiresManagerApproval": true,
      "requiresSecondaryApproval": true
    }
  ]
  ```

---

### 3.4 Service Tickets (`module.ticket`)

#### `POST /api/v1/tickets`
* **Description:** Submits a structured service request and evaluates approval workflow routing (`FR-011`, `FR-012`).
* **Authorization:** Authenticated (`ROLE_EMPLOYEE`+).
* **Request Body:**
  ```json
  {
    "catalogItemId": 4,
    "priority": "HIGH",
    "description": "Requesting upgraded developer workstation for Q4 backend compiler benchmarking."
  }
  ```
* **Validation:** `catalogItemId` (@NotNull), `priority` (@Pattern: LOW|MEDIUM|HIGH|URGENT), `description` (@NotBlank, @Size(min=15, max=2000)).
* **Response (201 Created):**
  ```json
  {
    "id": 42,
    "ticketNumber": "TKT-2026-00042",
    "catalogItemTitle": "Standard Developer Laptop",
    "department": "IT",
    "status": "PENDING_APPROVAL",
    "priority": "HIGH",
    "description": "Requesting upgraded developer workstation...",
    "requesterUsername": "john.doe",
    "assigneeUsername": null,
    "slaTargetDate": "2026-11-17T18:00:00.000Z",
    "createdAt": "2026-11-13T12:00:00.000Z",
    "version": 0
  }
  ```

#### `GET /api/v1/tickets/{id}`
* **Description:** Retrieves ticket details, including current status, assigned resolver, SLA status, and active approval stages.
* **Authorization:** Owner (`ROLE_EMPLOYEE`), Assigned Resolver (`ROLE_RESOLVER`), Approver, or `ROLE_ADMIN`.
* **Response (200 OK):** Full `ServiceTicketDetailDTO`.

#### `POST /api/v1/tickets/{id}/assign`
* **Description:** Assigns or self-claims a ticket in the department queue (`FR-014`). Transitions state to `IN_PROGRESS`.
* **Authorization:** `ROLE_RESOLVER` (belonging to ticket department) or `ROLE_ADMIN`.
* **Request Body:** `{"assigneeId": 18}` (Omit or pass own ID for self-assignment).
* **Response (200 OK):** Updated ticket DTO with `status = "IN_PROGRESS"`.
* **Error Codes:** `403 Forbidden` (Resolver not in department), `409 Conflict` (Version conflict).

#### `POST /api/v1/tickets/{id}/status`
* **Description:** Mutates ticket state according to the 11-State State Machine (`FR-012`, `ADR-007`).
* **Authorization:** `ROLE_RESOLVER` or `ROLE_ADMIN`.
* **Request Body:**
  ```json
  {
    "status": "ON_HOLD",
    "reason": "Awaiting shipment arrival of SSD upgrade from hardware vendor",
    "version": 1
  }
  ```
* **Validation:** `status` (@NotNull), `version` (@NotNull), `reason` (Mandatory for ON_HOLD and REJECTED, min 15 chars per `BR-007`).
* **Response (200 OK):** Updated ticket DTO.
* **Error Codes:** `400 Bad Request` (Illegal state transition), `409 Conflict` (Optimistic locking collision).

#### `POST /api/v1/tickets/{id}/cancel`
* **Description:** Allows requester to withdraw their ticket prior to active work (`SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `ON_HOLD`).
* **Authorization:** Requester (`ROLE_EMPLOYEE` owner) only.
* **Request Body:** `{"reason": "No longer required due to team reallocation", "version": 0}`.
* **Response (200 OK):** Status updated to `CANCELLED`.
* **Error Codes:** `400 Bad Request` (Cannot cancel if already `IN_PROGRESS`), `409 Conflict`.

#### `POST /api/v1/tickets/{id}/reopen`
* **Description:** Reopens a `RESOLVED` ticket within the 48-business-hour dispute window (`BR-003`). Transitions ticket directly to `IN_PROGRESS`.
* **Authorization:** Requester (`ROLE_EMPLOYEE` owner) only.
* **Request Body:**
  ```json
  {
    "reason": "The issued laptop crashes immediately on boot with kernel panic error 0x04",
    "version": 3
  }
  ```
* **Validation:** `reason` (@NotBlank, min 15 characters).
* **Response (200 OK):** Status updated to `IN_PROGRESS` with SLA clock resumed and extended.
* **Error Codes:** `400 Bad Request` (Dispute window expired $> 48$ business hours), `409 Conflict`.

---

### 3.5 Contextual Comments (`module.ticket`)

#### `GET /api/v1/tickets/{id}/comments`
* **Description:** Lists chronological comments on a ticket (`FR-015`).
* **Authorization:** Ticket Requester, Resolver, Approvers, or Admin.
* **Response (200 OK):** List of comments.

#### `POST /api/v1/tickets/{id}/comments`
* **Description:** Appends a comment to a ticket thread (`FR-015`).
* **Authorization:** Ticket Requester, Resolver, or Admin.
* **Request Body:**
  ```json
  {
    "commentText": "I have uploaded the system diagnostic report to the shared folder.",
    "isInternal": false
  }
  ```
* **Response (201 Created):** Created comment DTO.

---

### 3.6 Workflow Approvals (`module.workflow`)

#### `GET /api/v1/approvals/pending`
* **Description:** Retrieves pending approval tasks assigned to the authenticated manager or department head (`FR-016A`, `FR-016B`).
* **Authorization:** `ROLE_MANAGER`, `ROLE_DEPT_HEAD`, `ROLE_ADMIN`.
* **Response (200 OK):** List of pending `ApprovalTaskDTO` objects with linked ticket summaries.

#### `POST /api/v1/approvals/{taskId}/decide`
* **Description:** Records an approval or rejection decision with mandatory reason (`FR-017`, `BR-005`, `BR-007`).
* **Authorization:** Assigned Approver only. Strictly enforces Anti-Self-Approval (`BR-010`).
* **Request Body:**
  ```json
  {
    "decision": "APPROVE",
    "reason": "Confirmed hardware upgrade necessary for client project deliverables",
    "version": 0
  }
  ```
* **Validation:** `decision` (@Pattern: APPROVE|REJECT), `reason` (@NotBlank, min 15 chars), `version` (@NotNull).
* **Response (200 OK):**
  ```json
  {
    "taskId": 105,
    "ticketId": 42,
    "decision": "APPROVED",
    "newTicketStatus": "PENDING_SECONDARY_APPROVAL",
    "decidedAt": "2026-11-13T12:30:00.000Z"
  }
  ```
* **Error Codes:** `400 Bad Request` (Anti-self-approval violation: Requester attempted self-approval), `409 Conflict` (Task already decided by another approver or ticket modified).

---

### 3.7 Department Queues (`module.ticket`)

#### `GET /api/v1/departments/{department}/queue`
* **Description:** Fetches actionable ticket queue filtered by department and status (`FR-013`).
* **Authorization:** `ROLE_RESOLVER` (matching department) or `ROLE_ADMIN`.
* **Query Parameters:** `status` (default: `ASSIGNED`), `priority`, `page`, `size`, `sort`.
* **Response (200 OK):** Paginated ticket queue with SLA indicators (`SLA_OK`, `SLA_WARNING`, `SLA_BREACHED`).

---

### 3.8 Cryptographic Audit API (`module.audit`)

#### `GET /api/v1/audit/logs`
* **Description:** Paginated retrieval of append-only audit entries (`FR-018`).
* **Authorization:** `ROLE_ADMIN`, `ROLE_AUDITOR`.
* **Query Parameters:** `actor`, `action`, `startDate`, `endDate`, `page`, `size`.
* **Response (200 OK):** Paginated `AuditLogDTO` list including `sequenceNumber`, `previousHash`, `currentHash`, `payloadHash`.

#### `GET /api/v1/audit/verify`
* **Description:** Executes cryptographic hash-chain verification across all audit records (`ADR-011`).
* **Authorization:** `ROLE_ADMIN`, `ROLE_AUDITOR`.
* **Response (200 OK):**
  ```json
  {
    "status": "VALID",
    "totalRecordsVerified": 1420,
    "latestSequence": 1420,
    "chainHeadHash": "a4b2c1d3e5f6...",
    "tamperingDetected": false,
    "tamperedSequence": null,
    "errorMessage": null,
    "verifiedAt": "2026-11-13T12:45:00.000Z"
  }
  ```

