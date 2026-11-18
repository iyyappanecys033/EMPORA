# Task 07D: Baseline Verification & Architectural Reconnaissance Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Role:** Principal Software Architect, Workflow Engine Architect & Security Engineer  
**Date:** 2026-11-15  
**Stage:** Stage A — Baseline Verification & Design Review  
**Status:** **VERIFIED & READY FOR ORCHESTRATION IMPLEMENTATION**  

---

## 1. Executive Summary & Verified Baseline

Prior to implementing the backend orchestration layer (Ticket State Machine, Approvals, Authorization, SLA Engine, Transactional Cryptographic Audit Chain, and Post-Commit Notifications), a comprehensive baseline test run and code reconnaissance was performed.

### Verified Test Baseline:
* **Command:** `mvn test "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml`
* **Maven Exit Code:** `0` (BUILD SUCCESS)
* **Total Time:** `44.067 s`
* **Total Tests Executed:** `116`
* **Passed:** `116` (0 Failures, 0 Errors, 0 Skipped)
* **Suites Verified:**
  * `AccountLifecycleTests`: 36 tests (IAM account lifecycle, lockouts, recovery)
  * `AuditLogTests`: 24 tests (IAM security events, authentication auditing)
  * `FlywayMigrationTests`: 7 tests (Flyway V1–V3 migration pipeline & baseline safety)
  * `JwtAuthenticationTests`: 11 tests (JWT signing, parsing, claims, refresh tokens)
  * `RbacAuthenticationTests`: 15 tests (Role and authority security enforcement)
  * `RiskEngineTests`: 13 tests (Geographic, IP, velocity risk assessments)
  * `TicketPersistenceTests`: 10 tests (Service catalog, ticket & comment JPA persistence)

---

## 2. Actual Repository & Schema Inventory

### 2.1 Framework & Runtime Configuration
* **Runtime:** OpenJDK 25.0.2 LTS
* **Framework:** Spring Boot 3.4.12
* **ORM:** Hibernate 6.6.36.Final
* **Validation Mode:** `spring.jpa.hibernate.ddl-auto=validate`
* **Database Driver:** H2 2.3.232 (in test with `MODE=MySQL`), MySQL Connector/J 9.1.0 (for production profile)
* **Migration:** Flyway 10.x / 11.x applying `V1__init_iam_schema.sql`, `V2__service_catalog_and_tickets.sql`, `V3__approvals_and_audit_chain.sql`

### 2.2 Task 07C Persistence Components Verified
1. **`ServiceCatalogItem`:** Explicitly mapped with `@Table(name = "service_catalog")`, unique item code constraint, 11 fields.
2. **`ServiceTicket`:** Mapped with `@Table(name = "service_ticket")`, `@Version` optimistic concurrency token, lazy relationships to requester, assignee, and catalog item.
3. **`TicketComment`:** Mapped with `@Table(name = "ticket_comment")`, internal/external visibility flag, lazy ticket and author relationships.
4. **`TicketPriority`:** Enum with `LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL`.
5. **`TicketStatus`:** 11-state enum with `DRAFT`, `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`.
6. **Repositories:** `ServiceCatalogRepository`, `ServiceTicketRepository`, `TicketCommentRepository`.

### 2.3 Unmapped Migration Components for Task 07D
Inspection of `V3__approvals_and_audit_chain.sql` reveals two essential database tables that must now be mapped to JPA entities and repositories for workflow orchestration:
1. **`approval_task`:**
   * Columns: `id` (BIGINT PK), `ticket_id` (FK -> `service_ticket`), `approver_id` (FK -> `user_account`), `approval_stage` (INT), `status` (VARCHAR 20), `decision_reason` (TEXT), `created_at` (TIMESTAMP), `decided_at` (TIMESTAMP), `version` (BIGINT).
   * Constraints: `uk_approval_ticket_stage UNIQUE (ticket_id, approval_stage)`.
   * Action: Create `ApprovalTask` entity and `ApprovalTaskRepository`.
2. **`in_app_alert`:**
   * Columns: `id` (BIGINT PK), `recipient_id` (FK -> `user_account`), `ticket_id` (FK -> `service_ticket`), `alert_type` (VARCHAR 50), `title` (VARCHAR 150), `message` (TEXT), `is_read` (BOOLEAN), `created_at` (TIMESTAMP).
   * Action: Create `InAppAlert` entity and `InAppAlertRepository`.
3. **`audit_chain_head`:**
   * Columns: `id` (BIGINT PK), `current_hash` (CHAR 64), `last_sequence` (BIGINT), `updated_at` (TIMESTAMP).
   * Action: Create `AuditChainHead` entity and `AuditChainHeadRepository` with pessimistic locking query (`SELECT ... FOR UPDATE`).
4. **`audit_log` Extension:**
   * Already extended in V3 with `sequence_number BIGINT NULL`, `previous_hash CHAR(64) NULL`, `current_hash CHAR(64) NULL`.
   * Action: Update `AuditLog` entity to map these columns.

---

## 3. Implementation Plan & Stage Breakdown

1. **Stage B — Ticket Lifecycle State Machine:**
   * Centralized `TicketStatusTransitionValidator` enforcing the approved 11-state transition matrix from `LLD.md`.
   * Mandatory reason validation ($\ge 15$ characters) for `ON_HOLD`, `REJECTED`, `CANCELLED`, and reopening.
   * Work-in-progress unilateral cancellation restriction (`IN_PROGRESS` cannot be cancelled by requester).
   * Dispute window validation for ticket reopening ($\le 48$ business hours).
2. **Stage C — Ticket Approval Workflow:**
   * `ApprovalTask` domain entity with `@Version` and `ApprovalStatus` enum (`PENDING`, `APPROVED`, `REJECTED`).
   * `ApprovalService`: Task creation, primary approval, conditional secondary approval (when `requiresSecondaryApproval` is true or estimated cost exceeds threshold), and anti-self-approval enforcement (`requester_id != approver_id`).
   * Department head escalation when requester is a manager.
3. **Stage D — Role, Ownership & Organizational Authorization:**
   * Security evaluator and expression handlers checking:
     * Resource ownership (`requester.username == actor`).
     * Department resolver scope (`user.department == ticket.department`).
     * Manager and Department Head organizational scope.
     * Auditor read-only boundary.
4. **Stage E — SLA Clock & Business Calendar:**
   * `BusinessCalendar` service with injectable `Clock`.
   * Timezone: `Asia/Kolkata` (09:00–18:00, Monday–Friday, excluding holidays and weekends).
   * Separate tracking for resolution SLA vs. dispute window vs. automatic closure delay.
   * Accurate pause/resume minute accumulation.
5. **Stage F — Transactional Audit Chain Engine:**
   * `AuditChainService` with synchronous transactional execution inside the business mutation.
   * Pessimistic row locking on `audit_chain_head (id=1)` via `SELECT ... FOR UPDATE`.
   * SHA-256 canonical event hashing over `(timestamp + actor + action + target + details + seq + prevHash)`.
   * `AuditVerificationService` verifying mathematical chain integrity and detecting tampering or missing sequences.
6. **Stage G — Post-Commit Notification Orchestration:**
   * Domain events: `TicketSubmittedEvent`, `ApprovalRequiredEvent`, `TicketAssignedEvent`, `TicketStatusChangedEvent`, `TicketResolvedEvent`.
   * Asynchronous delivery via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
   * Persists to `in_app_alert` and mocks external email without blocking or failing committed transactions.
7. **Stage H — Application Services & API Endpoints:**
   * `TicketCommandService` and `TicketQueryService` implementing complete lifecycle orchestration.
   * REST Controllers under `/api/v1/tickets/*`, `/api/v1/approvals/*`, `/api/v1/audit/*`.
   * RFC 7807 problem responses with HTTP 409 on `OptimisticLockException`.
8. **Stage I — Integration, Security & Acceptance Testing:**
   * Dedicated `TicketWorkflowTests` covering all lifecycle flows, approvals, SLA, audit, and security boundaries.
   * Full regression pass ensuring 116 baseline tests + new tests pass cleanly.

---

## 4. Risks & Mitigations

| Identified Risk | Severity | Mitigation Strategy |
|---|:---:|---|
| Concurrency race on audit sequence allocation | HIGH | Explicit pessimistic row lock (`PessimisticLockScope.NORMAL`) on singleton `audit_chain_head (id=1)`. |
| Anti-self-approval bypass | HIGH | Server-side validation comparing authenticated principal ID with ticket requester ID; client-supplied IDs rejected. |
| Cross-department resolver data leakage | MEDIUM | Scope checks validating resolver employee profile department against ticket department. |
| Inadvertent notification rollback | MEDIUM | Strict usage of Spring's `AFTER_COMMIT` transaction phase with decoupled exception handling. |
| Timezone conversion errors in SLA | MEDIUM | Strict use of `ZoneId.of("Asia/Kolkata")` with UTC timestamps persisted in database. |

