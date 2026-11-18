# Task 07C: Schema-to-Entity Mapping Inventory & Persistence Architecture Blueprint

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Role:** Principal Java Architect, Senior Spring Boot Engineer & DDD Specialist  
**Date:** 2026-11-15  
**Baseline Verification:** Flyway V1, V2, V3 on MySQL 8.0+ / H2 (Compatibility Mode)  
**Task Scope:** Domain Entities & Persistence Foundation for Service Catalog, Service Tickets, and Ticket Comments  

---

## 1. Executive Summary & Reconnaissance

This document defines the authoritative schema-to-entity mapping for the EPAMS Service Management module, reconciling the physical Flyway migrations (`V1__init_iam_schema.sql`, `V2__service_catalog_and_tickets.sql`, `V3__approvals_and_audit_chain.sql`) with the approved domain architecture (`LLD.md`, `database-design.md`, `ADR-004`, `ADR-008`).

### Core Architectural Guardrails:
1. **Flyway Source of Truth:** Schema evolution is strictly managed via Flyway. JPA entities must conform to the existing tables; no DDL auto-generation (`spring.jpa.hibernate.ddl-auto=validate`).
2. **Explicit Table Naming:** In accordance with Task 07B.2 findings, the service catalog table is `service_catalog` and the Java domain entity is `ServiceCatalogItem`. Explicit `@Table(name = "service_catalog")` is mandatory.
3. **No Unintended Cascades:** Deleting a ticket or comment must never cascade to delete or alter IAM accounts (`user_account`), profiles (`profile`), or catalog definitions (`service_catalog`).
4. **Optimistic Locking:** Concurrency-sensitive entities with a physical `version` column (`service_ticket`) map to JPA `@Version`.
5. **No Separate Ticket Assignment Table:** In `V2`, ticket assignment is modeled via `service_ticket.assignee_id` referencing `user_account(id)`. There is no separate assignment table.
6. **No Premature Business Logic:** Workflow state engines, multi-stage approval orchestration, SLA calculators, and notifications remain deferred to subsequent tasks.

---

## 2. Actual Database Schema vs. Entity Mapping Inventory

| Database Table | Java Entity | Physical Columns | Primary Key | Foreign Keys | Constraints & Indexes |
|---|---|---|---|---|---|
| `service_catalog` | `ServiceCatalogItem` | `id` (BIGINT AUTO_INCREMENT)<br>`item_code` (VARCHAR 50)<br>`title` (VARCHAR 100)<br>`category` (VARCHAR 50)<br>`department` (VARCHAR 50)<br>`description` (TEXT)<br>`estimated_cost` (DECIMAL 10,2)<br>`sla_target_hours` (INT)<br>`requires_manager_approval` (BOOLEAN)<br>`requires_secondary_approval` (BOOLEAN)<br>`is_active` (BOOLEAN) | `id` (PK) | *None* | `uk_service_catalog_item_code` (UNIQUE `item_code`)<br>`idx_catalog_department`<br>`idx_catalog_category` |
| `service_ticket` | `ServiceTicket` | `id` (BIGINT AUTO_INCREMENT)<br>`ticket_number` (VARCHAR 32)<br>`catalog_item_id` (BIGINT)<br>`requester_id` (BIGINT)<br>`assignee_id` (BIGINT NULL)<br>`department` (VARCHAR 50)<br>`priority` (VARCHAR 20)<br>`status` (VARCHAR 30)<br>`description` (TEXT)<br>`created_at` (TIMESTAMP)<br>`resolved_at` (TIMESTAMP NULL)<br>`closed_at` (TIMESTAMP NULL)<br>`sla_target_date` (TIMESTAMP)<br>`total_paused_minutes` (INT)<br>`last_paused_at` (TIMESTAMP NULL)<br>`version` (BIGINT) | `id` (PK) | `catalog_item_id` &rarr; `service_catalog(id)`<br>`requester_id` &rarr; `user_account(id)`<br>`assignee_id` &rarr; `user_account(id)` | `uk_ticket_number` (UNIQUE `ticket_number`)<br>`fk_ticket_catalog`<br>`fk_ticket_requester`<br>`fk_ticket_assignee`<br>`idx_ticket_dept_status`<br>`idx_ticket_requester_created`<br>`idx_ticket_status_sla`<br>`idx_ticket_resolved_status` |
| `ticket_comment` | `TicketComment` | `id` (BIGINT AUTO_INCREMENT)<br>`ticket_id` (BIGINT)<br>`author_id` (BIGINT)<br>`comment_text` (TEXT)<br>`is_internal` (BOOLEAN)<br>`created_at` (TIMESTAMP) | `id` (PK) | `ticket_id` &rarr; `service_ticket(id)` (ON DELETE CASCADE)<br>`author_id` &rarr; `user_account(id)` | `fk_comment_ticket`<br>`fk_comment_author`<br>`idx_comment_ticket_created` |
| `user_account` | `UserAccount` *(Existing)* | `id` (BIGINT AUTO_INCREMENT)<br>`username` (VARCHAR 50)<br>`password_hash` (VARCHAR 255)<br>`email` (VARCHAR 100)<br>`phone` (VARCHAR 20)<br>`status` (VARCHAR 20)<br>`failed_login_attempts` (INT)<br>`locked_until` (TIMESTAMP) | `id` (PK) | *None* | `uk_user_account_username` (UNIQUE `username`) |
| `profile` | `Profile` *(Existing)* | `id` (BIGINT AUTO_INCREMENT)<br>`employee_id` (VARCHAR 30)<br>`user_id` (BIGINT)<br>`first_name` (VARCHAR 50)<br>`last_name` (VARCHAR 50)<br>`address` (VARCHAR 255)<br>`dob` (DATE)<br>`gender` (VARCHAR 20)<br>`department` (VARCHAR 50)<br>`designation` (VARCHAR 100)<br>`salary` (DOUBLE)<br>`date_of_joining` (DATE)<br>`reporting_manager` (VARCHAR 50)<br>`emergency_contact` (VARCHAR 50) | `id` (PK) | `user_id` &rarr; `user_account(id)` (ON DELETE CASCADE) | `uk_profile_employee_id` (UNIQUE `employee_id`)<br>`fk_profile_user`<br>`idx_profile_department`<br>`idx_profile_reporting_manager` |
| `audit_log` | `AuditLog` *(Existing)* | `id` (BIGINT AUTO_INCREMENT)<br>`actor_username` (VARCHAR 50)<br>`action` (VARCHAR 50)<br>`target_username` (VARCHAR 50)<br>`details` (VARCHAR 1000)<br>`timestamp` (TIMESTAMP)<br>`sequence_number` (BIGINT NULL)<br>`previous_hash` (CHAR 64 NULL)<br>`current_hash` (CHAR 64 NULL) | `id` (PK) | *None* | `idx_audit_target_user`<br>`idx_audit_action`<br>`uk_audit_log_sequence` (UNIQUE `sequence_number`)<br>`idx_audit_log_curr_hash` |
| `audit_chain_head` | `AuditChainHead` *(Anchor)* | `id` (BIGINT)<br>`current_hash` (CHAR 64)<br>`last_sequence` (BIGINT)<br>`updated_at` (TIMESTAMP) | `id` (PK) | *None* | Primary Key on `id` (singleton row `id=1`) |

---

## 3. Existing Entities Reused & Relationship Boundaries

### 3.1 Existing Entities Reused
1. **`UserAccount` (`com.examly.springapp.model.UserAccount`):**
   * Reused directly as the JPA target for:
     * `ServiceTicket.requester` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requester_id", nullable = false)`)
     * `ServiceTicket.assignee` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id")`)
     * `TicketComment.author` (`@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id", nullable = false)`)
   * **Boundary Rule:** No sensitive credential fields (`passwordHash`) or lock metadata are modified or exposed through ticket operations.
2. **`Profile` (`com.examly.springapp.model.Profile`):**
   * Accessible via `user.getId()` join in service/repository layers when employee department/designation verification is needed. Not directly coupled via foreign key in `service_ticket` (foreign key in DDL points to `user_account(id)`).

### 3.2 Relationships & Ownership Matrix

| Relationship | Owning Entity | Target Entity | Foreign Key Column | Fetch Type | Cascade Type | Delete Behavior |
|---|---|---|---|---|---|---|
| Ticket &rarr; Catalog | `ServiceTicket` | `ServiceCatalogItem` | `catalog_item_id` | `LAZY` | *None* | Restrict (DB prevents deleting catalog item with tickets) |
| Ticket &rarr; Requester | `ServiceTicket` | `UserAccount` | `requester_id` | `LAZY` | *None* | Restrict (DB prevents deleting user with tickets) |
| Ticket &rarr; Assignee | `ServiceTicket` | `UserAccount` | `assignee_id` | `LAZY` | *None* | Nullable; Restrict on user deletion |
| Comment &rarr; Ticket | `TicketComment` | `ServiceTicket` | `ticket_id` | `LAZY` | *None* | Cascaded by Database (`ON DELETE CASCADE`) |
| Comment &rarr; Author | `TicketComment` | `UserAccount` | `author_id` | `LAZY` | *None* | Restrict |

---

## 4. Schema Discrepancies & Reconciliation Analysis

### 4.1 Service Catalog Table Naming
* **Observation:** Design document mentions `ServiceCatalogItem` as domain class, while physical table in Flyway V2 is `service_catalog`.
* **Reconciliation:** Java class is named `ServiceCatalogItem` and explicitly annotated with `@Table(name = "service_catalog")`.

### 4.2 Ticket Assignment Table
* **Observation:** Prompt specifies inspecting "any ticket assignment table actually present".
* **Reconciliation:** Inspection of Flyway `V2__service_catalog_and_tickets.sql` confirms that assignments are implemented as a direct nullable foreign key column: `assignee_id BIGINT NULL REFERENCES user_account(id)`. There is **no separate assignment table** in V2. In V3, multi-stage approvals use `approval_task`, which is out of scope for 07C.

### 4.3 Optimistic Locking on Tickets
* **Observation:** Flyway V2 defines `version BIGINT NOT NULL DEFAULT 0` on `service_ticket`.
* **Reconciliation:** Annotated with `@Version private Long version;` on `ServiceTicket`. This ensures Hibernate handles concurrent ticket updates with `OptimisticLockException` prevention.

### 4.4 Enumerations vs. String Columns
* **Priority:** DDL column `priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'`. Modeled as Java enum `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, `URGENT`) with `@Enumerated(EnumType.STRING)`.
* **Status:** DDL column `status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED'`. Modeled as Java enum `TicketStatus` with `@Enumerated(EnumType.STRING)`. The 11 canonical EPAMS ticket states are:
  `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `IN_PROGRESS`, `PENDING_INFO`, `RESOLVED`, `CLOSED`, `CANCELLED`.

---

## 5. Implementation Directives & Next Stages

1. **Stage B:** Implement `ServiceCatalogItem` and supporting enums.
2. **Stage C:** Implement `ServiceTicket` with `@Version`, enums `TicketPriority` and `TicketStatus`, and lazy user/catalog associations.
3. **Stage D:** Implement `TicketComment` with lazy ticket and author associations.
4. **Stage E:** Validate relationships and verify zero cascade deletions affecting IAM accounts.
5. **Stage F:** Implement Spring Data JPA repositories:
   * `ServiceCatalogRepository`
   * `ServiceTicketRepository`
   * `TicketCommentRepository`
6. **Stage G:** Construct comprehensive persistence test suite verifying `ddl-auto=validate`, foreign key constraints, and optimistic locking, while ensuring the 106 existing tests continue to pass.
