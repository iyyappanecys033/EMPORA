# EPAMS — P9.6 Field-by-Field Input Validation Inventory

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage F: Complete User-Controlled Field Inventory (`44` DTO Fields + Query/Path Parameters)

---

## 1. Request DTO Field Inventory (`10` Request DTOs, `44` Fields)

| DTO | Field | Type | Required | Server-Side Validation Rule | Frontend Validation | Persistence Constraint |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| `LoginRequest` | `username` | `String` | Yes | `@NotBlank`, `@Size(max=100)` (`QA-DEF-003`) | `required` | `user_account.username` |
| `LoginRequest` | `password` | `String` | Yes | `@NotBlank`, `@Size(max=128)` (`QA-DEF-003`) | `required` | BCrypt hash |
| `RegisterRequest` | `username` | `String` | Yes | `@NotBlank`, `@Size(min=3, max=50)`, unique check | `required` | Unique `VARCHAR(50)` |
| `RegisterRequest` | `password` | `String` | Yes | `@NotBlank`, `@Size(min=4, max=100)` (`QA-DEF-003`) | `required` | BCrypt hash |
| `RegisterRequest` | `email` | `String` | No | `@Email`, `@Size(max=100)` (`QA-DEF-003`) | `type="email"` | `VARCHAR(100)` |
| `RegisterRequest` | `phone` | `String` | No | `@Size(max=30)` (`QA-DEF-003`) | Text input | `VARCHAR(30)` |
| `CreateTicketRequestDTO` | `catalogItemId` | `Long` | Yes | `@NotNull`, FK lookup (`404` if missing) | `required` `<select>` | FK `service_catalog(id)` |
| `CreateTicketRequestDTO` | `department` | `String` | Yes | `@NotBlank`, `@Size(max=50)` | `required` | `VARCHAR(50)` |
| `CreateTicketRequestDTO` | `priority` | `TicketPriority` | Yes | `@NotNull`, 5-tier enum (`LOW`..`CRITICAL`) | `<select>` | `VARCHAR(20)` |
| `CreateTicketRequestDTO` | `description` | `String` | Yes | `@NotBlank`, `@Size(min=10, max=2000)` | `required`, char counter | `TEXT` |
| `TicketStatusUpdateRequestDTO` | `status` | `TicketStatus` | Yes | `@NotNull`, 11-state FSM validation | Action button | `VARCHAR(30)` |
| `TicketStatusUpdateRequestDTO` | `reason` | `String` | Cond. | `@Size(max=1000)`; FSM ≥15 chars for `ON_HOLD`/`REJECTED`/`CANCELLED`/Reopen | Min 15 char check | Audit & log |
| `TicketStatusUpdateRequestDTO` | `version` | `Long` | No | `@Version` optimistic lock check (`409` on mismatch) | Passed from ticket DTO | `BIGINT` `@Version` |
| `TicketActionRequestDTO` | `reason` | `String` | Cond. | `@Size(max=1000)`; FSM ≥15 chars | Min 15 char check | Audit detail |
| `TicketActionRequestDTO` | `version` | `Long` | No | Optimistic locking check (`409`) | Passed from ticket DTO | `BIGINT` `@Version` |
| `TicketAssignRequestDTO` | `assigneeId` | `Long` | Yes | `@NotNull`, user existence check | Resolver selector | FK `user_account(id)` |
| `TicketCommentRequestDTO` | `commentText` | `String` | Yes | `@NotBlank`, `@Size(min=1, max=2000)` | `required` `<textarea>` | `TEXT` |
| `TicketCommentRequestDTO` | `isInternal` | `Boolean` | No | Non-resolvers forced `false` (requester cannot inject internal note) | Hidden for non-resolvers | `BOOLEAN` |
| `ApprovalDecisionRequestDTO` | `decision` | `ApprovalStatus` | Yes | `@NotNull` (`APPROVED`/`REJECTED`) | Approve / Reject buttons | `VARCHAR(20)` |
| `ApprovalDecisionRequestDTO` | `reason` | `String` | Cond. | `@Size(max=1000)`; ≥15 chars mandatory on `REJECTED` (BR-007) | Min 15 char guard on reject | `TEXT` |
| `ApprovalDecisionRequestDTO` | `version` | `Long` | No | Optimistic concurrency token | Passed from task | `BIGINT` |
| `EmployeeRequestDTO` | `username`, `employeeId`, `password`, `email`, `phone`, `role`, `status`, `firstName`, `lastName`, `department`, `designation`, `salary`, `dateOfJoining`, `reportingManager`, `emergencyContact`, `address`, `dob`, `gender` (18 fields) | Mixed | `username` req. | `@NotBlank`/`@Size` on strings, `@Email` on `email`, `@PositiveOrZero` on `salary` (`QA-DEF-003`); `ADMIN`/`HR_MANAGER` only | HR Admin form | `user_account` & `profile` |
| `UserRoleMappingDto` | `id`, `userId`, `username`, `roleId`, `roleName` (5 fields) | Mixed | Role/User IDs | `ADMIN` only; duplicate user+role rejected (`409`) | Admin mapping form | `user_role_mapping` |

---

## 2. Query Parameters & Path Variables Inventory

| Parameter | Endpoints | Validation & Clamping Behavior |
| :--- | :--- | :--- |
| `page` | Catalog, MyTickets, DeptQueue, Notifications, Directory, Audit | Clamped via `Math.max(page, 0)` (negative page never crashes) |
| `size` | Catalog, MyTickets, DeptQueue, Notifications, Directory, Audit | Clamped to `[1, 100]` (`size=99999` safely capped at `100` — prevents DoS/OOM) |
| `sortBy` | `GET /api/profiles/directory` | Strict allowlist (`QA-DEF-002`): non-HR/Admin restricted to 7 non-sensitive fields; `salary` & unknown fields return `400` |
| `direction` | `GET /api/profiles/directory` | `"asc"` vs default `"desc"` (safe enum branch) |
| `query` / `department` / `category` / `status` / `priority` | Directory, Catalog, Queue, Analytics | Bound exclusively as JPA named parameters (`:param`) — immune to SQL injection |
| `startDate` / `endDate` | Analytics endpoints | ISO-8601 `LocalDate`; invalid format returns `400` (`MethodArgumentTypeMismatchException`) |
| `{id}` / `{ticketId}` / `{taskId}` | Resource path variables | Non-numeric strings return `400` (`MethodArgumentTypeMismatchException`); missing records return `404` |
| Multipart Uploads | N/A | **Explicitly Excluded:** No multipart file upload endpoint exists in EPAMS (`EmployeeProfile` honestly uses monogram fallback). |
