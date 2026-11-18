# Task 07C: Domain Entities & Service Management Persistence Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Role:** Principal Java Architect, Senior Spring Boot Engineer & DDD Specialist  
**Date:** 2026-11-15  
**Runtime:** OpenJDK 25 LTS / Spring Boot 3.4.12 / Hibernate 6.6.36.Final / Flyway 10.x  
**Validation Mode:** `spring.jpa.hibernate.ddl-auto=validate`  
**Execution Verdict:** **100% COMPLETE & PASSING (116/116 Tests Passing)**  

---

## 1. Executive Summary & Scope Completed

In **Task 07C**, the domain persistence foundation for the EPAMS Service Management module was implemented. This encompasses the domain model entities, enumeration contracts, relationship boundaries, and Spring Data JPA repositories for the Service Catalog, Service Tickets, and Contextual Ticket Comments.

### Milestones Delivered:
1. **Repository & Schema Reconnaissance:** Reconciled physical Flyway DDL (`V1__init_iam_schema.sql`, `V2__service_catalog_and_tickets.sql`, `V3__approvals_and_audit_chain.sql`) with architectural blueprints (`LLD.md`, `database-design.md`). Authored [`task-07c-schema-entity-mapping.md`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/implementation/task-07c-schema-entity-mapping.md).
2. **Service Catalog Domain Entity (`ServiceCatalogItem`):** Explicitly mapped to database table `service_catalog` with all 11 columns, unique item code constraint, and active status filtering.
3. **Service Ticket Domain Entity (`ServiceTicket`):** Implemented with 15 database columns, `@Version` optimistic concurrency token, finite-state machine enum `TicketStatus` (11 canonical states), priority enum `TicketPriority`, and domain helper methods for SLA clock pause/resume.
4. **Ticket Comment Domain Entity (`TicketComment`):** Implemented with internal/external visibility flagging, timestamping, and database-level foreign key cascade binding to tickets.
5. **Spring Data JPA Repositories:** Implemented `ServiceCatalogRepository`, `ServiceTicketRepository`, and `TicketCommentRepository` supporting pagination (`Pageable`), indexed status/department filtering, and audit counts.
6. **Persistence Test Suite:** Created `TicketPersistenceTests` (10 tests) verifying entity persistence, retrieval, referential integrity constraints, unique constraints, optimistic version increments, and cascade safety.
7. **Full Consolidated Regression Pass:** All **116 automated tests across 7 test classes** executed cleanly with **0 failures, 0 errors, and Maven Exit Code 0**.

---

## 2. Actual Database Tables Inspected

The physical Flyway migration scripts in `src/main/resources/db/migration/` serve as the schema source of truth:

| Database Table | Migration Source | Primary Key | Total Columns | Constraints & Indexes Inspected |
|---|---|---|:---:|---|
| `service_catalog` | `V2__service_catalog_and_tickets.sql` | `id` (BIGINT AUTO_INCREMENT) | 11 | `uk_service_catalog_item_code` (UNIQUE), `idx_catalog_department`, `idx_catalog_category` |
| `service_ticket` | `V2__service_catalog_and_tickets.sql` | `id` (BIGINT AUTO_INCREMENT) | 15 | `uk_ticket_number` (UNIQUE), `fk_ticket_catalog`, `fk_ticket_requester`, `fk_ticket_assignee`, `idx_ticket_dept_status`, `idx_ticket_requester_created`, `idx_ticket_status_sla`, `idx_ticket_resolved_status` |
| `ticket_comment` | `V2__service_catalog_and_tickets.sql` | `id` (BIGINT AUTO_INCREMENT) | 6 | `fk_comment_ticket` (ON DELETE CASCADE), `fk_comment_author`, `idx_comment_ticket_created` |
| `user_account` | `V1__init_iam_schema.sql` | `id` (BIGINT AUTO_INCREMENT) | 8 | `uk_user_account_username` (UNIQUE) |
| `profile` | `V1__init_iam_schema.sql` | `id` (BIGINT AUTO_INCREMENT) | 14 | `uk_profile_employee_id` (UNIQUE), `fk_profile_user`, `idx_profile_department`, `idx_profile_reporting_manager` |
| `audit_log` | `V1` + `V3` (Option A / ADR-016) | `id` (BIGINT AUTO_INCREMENT) | 9 | `uk_audit_log_sequence` (UNIQUE nullable), `idx_audit_log_curr_hash`, `idx_audit_target_user`, `idx_audit_action` |
| `audit_chain_head` | `V3__approvals_and_audit_chain.sql` | `id` (BIGINT) | 4 | Singleton row `id=1` with `last_sequence` and `current_hash` |

*Note on Ticket Assignment:* In Flyway V2, ticket assignment is modeled directly as `service_ticket.assignee_id` referencing `user_account(id)`. There is no separate assignment table.

---

## 3. Entity-to-Table Mapping & Implementation Details

### 3.1 Service Catalog Entity (`ServiceCatalogItem`)
* **Class:** `com.examly.springapp.model.ServiceCatalogItem`
* **Table:** Explicitly mapped with `@Table(name = "service_catalog")` to prevent Hibernate from defaulting to `service_catalog_item`.
* **Field Mapping:**
  * `id` &rarr; `id BIGINT AUTO_INCREMENT PRIMARY KEY`
  * `itemCode` &rarr; `item_code VARCHAR(50) NOT NULL UNIQUE`
  * `title` &rarr; `title VARCHAR(100) NOT NULL`
  * `category` &rarr; `category VARCHAR(50) NOT NULL`
  * `department` &rarr; `department VARCHAR(50) NOT NULL`
  * `description` &rarr; `description TEXT NOT NULL`
  * `estimatedCost` &rarr; `estimated_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00` (mapped to `java.math.BigDecimal`)
  * `slaTargetHours` &rarr; `sla_target_hours INT NOT NULL DEFAULT 24`
  * `requiresManagerApproval` &rarr; `requires_manager_approval BOOLEAN NOT NULL DEFAULT TRUE`
  * `requiresSecondaryApproval` &rarr; `requires_secondary_approval BOOLEAN NOT NULL DEFAULT FALSE`
  * `isActive` &rarr; `is_active BOOLEAN NOT NULL DEFAULT TRUE` (with `isActive()`, `getIsActive()`, and `setIsActive()` accessors)

### 3.2 Service Ticket Entity (`ServiceTicket`)
* **Class:** `com.examly.springapp.model.ServiceTicket`
* **Table:** `@Table(name = "service_ticket")`
* **Concurrency Control:** `@Version @Column(name = "version", nullable = false) private Long version = 0L;` mapped to Flyway V2's `version BIGINT NOT NULL DEFAULT 0`.
* **Enumerations:**
  * `priority` &rarr; `@Enumerated(EnumType.STRING)` mapping to `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL`).
  * `status` &rarr; `@Enumerated(EnumType.STRING)` mapping to `TicketStatus` (`DRAFT`, `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`).
* **Domain Methods:**
  * `pauseSlaClock(LocalDateTime now)`: Sets status to `ON_HOLD` and stamps `lastPausedAt`.
  * `resumeSlaClock(LocalDateTime now)`: Computes elapsed minutes, adds to `totalPausedMinutes`, and clears `lastPausedAt`.
  * `canBeCancelledByRequester()`: Enforces cancellation business rule (only cancellable prior to specialist assignment / progress).

### 3.3 Ticket Comment Entity (`TicketComment`)
* **Class:** `com.examly.springapp.model.TicketComment`
* **Table:** `@Table(name = "ticket_comment")`
* **Field Mapping:**
  * `ticket` &rarr; `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ticket_id", nullable = false)`
  * `author` &rarr; `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id", nullable = false)`
  * `commentText` &rarr; `@Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")`
  * `isInternal` &rarr; `@Column(name = "is_internal", nullable = false) private Boolean isInternal = false;`
  * `createdAt` &rarr; `@Column(name = "created_at", nullable = false, updatable = false)` populated automatically via `@PrePersist`.

---

## 4. Relationship Design & Cascade Safety Matrix

To prevent inadvertent data loss and accidental eager loading of entire graphs, all JPA associations follow strict containment and lazy-fetching patterns:

| Relationship | Owning Entity | Target Entity | Foreign Key Column | Fetch Type | Cascade Type | Delete Behavior |
|---|---|---|---|---|---|---|
| Ticket &rarr; Catalog Item | `ServiceTicket` | `ServiceCatalogItem` | `catalog_item_id` | `LAZY` | *None* | Restrict (DB prevents deleting catalog item with tickets) |
| Ticket &rarr; Requester | `ServiceTicket` | `UserAccount` | `requester_id` | `LAZY` | *None* | Restrict (DB prevents deleting employee account with tickets) |
| Ticket &rarr; Assignee | `ServiceTicket` | `UserAccount` | `assignee_id` | `LAZY` | *None* | Nullable; Restrict on user deletion |
| Comment &rarr; Ticket | `TicketComment` | `ServiceTicket` | `ticket_id` | `LAZY` | *None* | Cascaded by Database (`ON DELETE CASCADE`) |
| Comment &rarr; Author | `TicketComment` | `UserAccount` | `author_id` | `LAZY` | *None* | Restrict |

*Bidirectional Collection Omission:* `ServiceTicket` intentionally omits a bidirectional `@OneToMany List<TicketComment> comments` collection. This prevents circular JSON serialization vulnerabilities, eliminates N+1 accidental loading, and decouples comment lifecycle queries into `TicketCommentRepository`.

---

## 5. Repository Implementations

The Spring Data JPA repositories are located in package `com.examly.springapp.repository`:

### 5.1 `ServiceCatalogRepository`
* `Optional<ServiceCatalogItem> findByItemCode(String itemCode);`
* `boolean existsByItemCode(String itemCode);`
* `List<ServiceCatalogItem> findByIsActiveTrue();`
* `Page<ServiceCatalogItem> findByIsActiveTrue(Pageable pageable);`
* `List<ServiceCatalogItem> findByDepartmentAndIsActiveTrue(String department);`
* `Page<ServiceCatalogItem> findByDepartmentAndIsActiveTrue(String department, Pageable pageable);`
* `List<ServiceCatalogItem> findByCategoryAndIsActiveTrue(String category);`
* `Page<ServiceCatalogItem> findByCategoryAndIsActiveTrue(String category, Pageable pageable);`

### 5.2 `ServiceTicketRepository`
* `Optional<ServiceTicket> findByTicketNumber(String ticketNumber);`
* `boolean existsByTicketNumber(String ticketNumber);`
* `Page<ServiceTicket> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);`
* `Page<ServiceTicket> findByAssigneeIdOrderByCreatedAtDesc(Long assigneeId, Pageable pageable);`
* `Page<ServiceTicket> findByDepartmentOrderByCreatedAtDesc(String department, Pageable pageable);`
* `Page<ServiceTicket> findByDepartmentAndStatusOrderByCreatedAtDesc(String department, TicketStatus status, Pageable pageable);`
* `Page<ServiceTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);`
* `List<ServiceTicket> findByStatus(TicketStatus status);`
* `long countByStatus(TicketStatus status);`
* `long countByDepartmentAndStatus(String department, TicketStatus status);`
* `long countByRequesterIdAndStatus(Long requesterId, TicketStatus status);`

### 5.3 `TicketCommentRepository`
* `List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);`
* `List<TicketComment> findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(Long ticketId);`
* `long countByTicketId(Long ticketId);`

---

## 6. Schema Compatibility & Validation Results

* **Hibernate Validation Mode:** `spring.jpa.hibernate.ddl-auto=validate` was enforced during Spring context initialization.
* **Flyway Execution:** Flyway migrations `V1`, `V2`, and `V3` executed on a clean schema, followed immediately by Hibernate's strict metadata validation.
* **Result:** Zero schema mismatch errors. All tables, columns, data types, nullabilities, and unique constraints matched the Flyway DDL byte-for-byte.

---

## 7. Persistence Test Coverage (`TicketPersistenceTests`)

A dedicated integration test suite [`TicketPersistenceTests.java`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/test/java/com/examly/springapp/TicketPersistenceTests.java) was constructed and executed:

| Test Name | Focus Area | Assertions & Behaviors Verified | Status |
|---|---|---|:---:|
| `test1_ServiceCatalogItem_PersistAndRetrieve` | Service Catalog | Persists and retrieves item, verifies field mapping, active filters, and department queries. | **PASS** |
| `test2_ServiceCatalogItem_UniqueConstraintViolation` | Service Catalog | Confirms `DataIntegrityViolationException` is thrown when inserting duplicate `item_code`. | **PASS** |
| `test3_ServiceTicket_PersistAndRetrieveWithRelationships` | Service Ticket | Persists ticket with valid requester, assignee, and catalog item; verifies default version `0` and status `SUBMITTED`. | **PASS** |
| `test4_ServiceTicket_OptimisticLockingIncrementsVersion` | Optimistic Concurrency | Verifies `@Version` increments from `0` to `1` upon atomic entity mutation in a new transaction. | **PASS** |
| `test5_ServiceTicket_ForeignKeyConstraintViolation` | Referential Integrity | Asserts database rejection (`DataIntegrityViolationException`) when assigning a non-existent `requester_id`. | **PASS** |
| `test6_TicketComment_PersistAndRetrieveByTicket` | Ticket Comments | Tests public vs. internal comment visibility filtering; asserts chronological sorting. | **PASS** |
| `test7_DeleteTicket_CascadesComments_PreservesUserAndCatalog` | Cascade Safety | Proves deleting a ticket cascades to comments, while strictly preserving `UserAccount` and `ServiceCatalogItem`. | **PASS** |
| `test8_ServiceTicket_SlaPauseResumeBusinessMethods` | Domain Logic | Verifies `pauseSlaClock()` and `resumeSlaClock()` accumulate `totalPausedMinutes` accurately. | **PASS** |
| `test9_PaginationAndCustomQueries` | Repositories | Tests pagination (`PageRequest`), multi-page navigation, and count queries across departments. | **PASS** |
| `test10_SchemaValidation_ExplicitTableMappingProof` | Schema Metadata | Verifies runtime `@Table` annotations match `service_catalog`, `service_ticket`, and `ticket_comment`. | **PASS** |

---

## 8. Full Consolidated Regression Results

The entire EPAMS automated test suite was executed via Maven Surefire:

```bash
mvn test "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml
```

### Verified Execution Summary:
* **Maven Exit Code:** `0` (BUILD SUCCESS)
* **Total Elapsed Time:** `57.480 s`
* **Total Tests Executed:** `116`
* **Passed:** `116`
* **Failures:** `0`
* **Errors:** `0`
* **Skipped:** `0`

### Breakdown by Test Suite:
| Test Class | Test Focus | Tests Run | Failures | Errors | Status |
|---|---|:---:|:---:|:---:|:---:|
| `AccountLifecycleTests` | IAM Account Lifecycle, Lockouts & Expirations | 36 | 0 | 0 | **PASS** |
| `AuditLogTests` | Security Event Audit Logging & Filtering | 24 | 0 | 0 | **PASS** |
| `FlywayMigrationTests` | Flyway V1-V3 Migration Pipeline & Baseline Safety | 7 | 0 | 0 | **PASS** |
| `JwtAuthenticationTests` | Stateless JWT Tokens & Refresh Handshake | 11 | 0 | 0 | **PASS** |
| `RbacAuthenticationTests` | Role-Based Access Control & Authority Boundaries | 15 | 0 | 0 | **PASS** |
| `RiskEngineTests` | IP, Geographic & Velocity Risk Scoring | 13 | 0 | 0 | **PASS** |
| `TicketPersistenceTests` | **NEW: Catalog, Ticket & Comment JPA Persistence** | 10 | 0 | 0 | **PASS** |
| **Consolidated Total** | | **116** | **0** | **0** | **100% PASS** |

---

## 9. Known Limitations & Environment Classification

1. **H2 vs. MySQL Dialect Verification:**
   * Automated tests execute on in-memory H2 with `MODE=MySQL;DATABASE_TO_LOWER=TRUE`.
   * Live remote MySQL 8.0 server socket testing remains `NOT TESTED (BLOCKED)` due to environment authorization constraints (local port 3306 credentials unauthorized, Docker daemon inactive).
   * All DDL scripts are hardened with explicit `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` and clean standard MySQL primitives.
2. **Entity Isolation:**
   * No REST controllers or DTO mappers were exposed in this task; entities remain strictly scoped to the persistence layer.

---

## 10. Deferred Implementation Work

In accordance with strict out-of-scope boundaries, the following components remain deferred to subsequent tasks:
* REST API endpoints for Ticket creation, browsing, and comments (Task 07E).
* Service Ticket Finite State Machine (FSM) Transition Engine & Validation (Task 07D).
* Multi-level approval engine and anti-self-approval enforcement (`approval_task`).
* SLA business calendar calculation and breach warning cron jobs.
* Cryptographic audit hash-chain runtime computation engine.
* Outbound notification delivery and WebSocket alerts.

---

## 11. Files Created or Modified

### Files Created:
1. `springapp/src/main/java/com/examly/springapp/model/TicketPriority.java`: Priority enum (`LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL`).
2. `springapp/src/main/java/com/examly/springapp/model/TicketStatus.java`: 11-state ticket lifecycle enum.
3. `springapp/src/main/java/com/examly/springapp/model/ServiceCatalogItem.java`: Entity explicitly mapped to `service_catalog`.
4. `springapp/src/main/java/com/examly/springapp/model/ServiceTicket.java`: Ticket entity with `@Version` and SLA helper methods.
5. `springapp/src/main/java/com/examly/springapp/model/TicketComment.java`: Comment entity mapped to `ticket_comment`.
6. `springapp/src/main/java/com/examly/springapp/repository/ServiceCatalogRepository.java`: JPA repository for catalog items.
7. `springapp/src/main/java/com/examly/springapp/repository/ServiceTicketRepository.java`: JPA repository for tickets.
8. `springapp/src/main/java/com/examly/springapp/repository/TicketCommentRepository.java`: JPA repository for comments.
9. `springapp/src/test/java/com/examly/springapp/TicketPersistenceTests.java`: 10-method persistence test suite.
10. `docs/implementation/task-07c-schema-entity-mapping.md`: Schema-to-entity mapping blueprint.
11. `docs/implementation/task-07c-domain-persistence-report.md`: Comprehensive completion report.

### Files Restored / Verified:
* `springapp/src/main/resources/db/migration/V1__init_iam_schema.sql`: Restored full canonical V1 DDL content.

---

## 12. Recommended Next Task

**Task 07D: Service Ticket Finite State Machine (FSM) & Workflow Transition Engine**
* Implement `TicketStatusTransitionValidator` enforcing the approved 11-state transition matrix from `LLD.md`.
* Implement `TicketCommandService` and `TicketQueryService` interfaces.
* Implement atomic optimistic locking conflict handling (`OptimisticLockException` mapping to HTTP 409).
* Construct unit and service-level test suites validating legal vs. illegal state transitions.
