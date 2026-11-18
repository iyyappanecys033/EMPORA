# Consolidated Architecture Decision Records (ADRs)
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-ADR-ALL-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved Architectural Baseline  
**Author:** Principal Software Architect & Enterprise Architecture Review Board  

---

## 1. Executive Summary & Index of Architecture Decisions

This document consolidates the foundational architectural decisions formulated and validated during Phase M1 and Phase 1 HLD/LLD design. Each record details the business context, alternatives considered, technical trade-offs, and downstream engineering consequences.

| ADR ID | Decision Topic | Status | Impact Area |
| :--- | :--- | :--- | :--- |
| **`ADR-001`** | Hybrid Approval Hierarchy (Single-Level Default, Conditional Two-Level) | **Approved** | Workflow Engine, Governance |
| **`ADR-002`** | Decoupled React Frontend Architecture During Development | **Approved** | Developer Experience, Build Pipeline |
| **`ADR-003`** | Multi-Stage Docker Production Build with Minimal JRE Image | **Approved** | DevOps, Security, Containerization |
| **`ADR-004`** | MySQL 8.0+ as Primary Production Persistence Tier | **Approved** | Database, Storage, Concurrency |
| **`ADR-005`** | Flyway Schema Migrations & Strict Hibernate DDL Validation | **Approved** | Schema Evolution, CI/CD |
| **`ADR-006`** | Modular Monolith Backend Architecture | **Approved** | Package Boundaries, Modularity |
| **`ADR-007`** | 11-State Ticket Lifecycle Finite State Machine | **Approved** | State Transitions, SLA Calculation |
| **`ADR-008`** | Optimistic Concurrency Control via JPA `@Version` & HTTP 409 | **Approved** | Data Integrity, Race Prevention |
| **`ADR-009`** | Mailpit Container Infrastructure for Email Testing | **Approved** | Testing, Notifications |
| **`ADR-010`** | Java 25 LTS Baseline & Spring Boot 4.1.x Target Evaluation | **Approved Baseline** | JVM Runtime, Framework Roadmap |
| **`ADR-011`** | Transactional SHA-256 Hash-Chained Audit Logging & Privilege Separation | **Approved** | Audit, Compliance, Non-Repudiation |
| **`ADR-012`** | Stateless JWT Authentication with Database-Backed Refresh Token Rotation | **Approved** | Security, Session Management |
| **`ADR-013`** | Department-Level Queue Access Isolation | **Approved** | Data Isolation, RBAC |
| **`ADR-014`** | Sensitive Field Masking at Service & Serializer Boundary | **Approved** | Data Privacy, DPDP Compliance |
| **`ADR-015`** | Synchronous Local Chain Head Row Locking for Audit Serialization | **Approved** | Audit Concurrency, Ledger Order |

---

## 2. Detailed Decision Records

### ADR-001: Hybrid Approval Hierarchy (Single-Level Default, Conditional Two-Level)
* **Context:** Routine requests (software licenses, stationery, employment letters) suffer from excessive bureaucracy when subjected to rigid multi-level management approvals. Conversely, expensive hardware capital expenditures ($> ₹10,000$) or privileged administrative permissions pose corporate liability if approved by a single first-line manager.
* **Decision:**
  1. Standard service requests default to **single-level approval** by the requester's direct Reporting Manager (`ROLE_MANAGER`). Once approved, the ticket routes immediately to the department resolver queue as `ASSIGNED`.
  2. Any request with an estimated cost exceeding the configurable threshold (`app.workflow.approval.financial-threshold`, default ₹10,000) or tagged as `PRIVILEGED_ACCESS` automatically escalates to a **two-level approval** (Reporting Manager $\rightarrow$ Department Head).
  3. Strict **Anti-Self-Approval** (`BR-005`, `BR-010`): A user cannot approve their own request under any circumstance. Manager requests automatically escalate to their department head.
* **Consequences:** Accelerates 85% of standard requests while maintaining strict fiscal and governance controls on high-risk actions.

---

### ADR-002: Decoupled React Frontend Architecture During Development
* **Context:** Running frontend assets strictly embedded inside the Spring Boot JAR during local feature development slows down frontend UI iterations due to continuous rebuild cycles.
* **Decision:** Decouple runtime environments during development. Developers run the React frontend independently via Webpack Dev Server on port 3000 with Hot Module Replacement (HMR) and an active proxy forwarding `/api` calls to Spring Boot on port 8080. For production, Docker multi-stage builds bundle optimized static assets into Spring Boot's `/static` classpath.
* **Consequences:** Near-instantaneous frontend feedback loop ($< 500\text{ ms}$); unified single-artifact container in production.

---

### ADR-003: Multi-Stage Docker Production Build with Minimal JRE Image
* **Context:** Monolithic container images containing Node.js, npm, Maven build tools, and full JDKs expand image sizes to $> 1.2\text{ GB}$ and introduce significant CVE vulnerabilities.
* **Decision:** Implement a 3-stage `Dockerfile`:
  * Stage 1: `node:20-alpine` builds production React assets (`npm run build`).
  * Stage 2: `maven:3.9-eclipse-temurin-25` compiles backend Java classes and packages the JAR.
  * Stage 3: `eclipse-temurin:25-jre-alpine` copies *only* the compiled JAR. Execution runs under unprivileged service user `spring:spring` (UID 1001).
* **Consequences:** Production image size drops to $< 250\text{ MB}$; zero build tools or package managers exist in the production runtime attack surface.

---

### ADR-004: MySQL 8.0+ as Primary Production Persistence Tier
* **Context:** In-memory databases (H2) do not support production concurrency, durability, or relational indexing at enterprise scale.
* **Decision:** Standardize on **MySQL 8.0+ (InnoDB Storage Engine)** for all staging and production deployments. H2 is retained *strictly* for unit and fast automated slice tests. Strict `utf8mb4` character set and UTC timezones are enforced. HikariCP is configured with `maximum-pool-size: 10`.
* **Consequences:** Full ACID compliance, robust foreign key constraint enforcement, and support for row-level locking (`FOR UPDATE`).

---

### ADR-005: Flyway Schema Migrations & Strict Hibernate DDL Validation
* **Context:** Allowing Hibernate auto-DDL (`ddl-auto=update`) in production risks uncontrolled schema mutations, lost column constraints, or accidental data loss during application restarts.
* **Decision:** Adopt **Flyway** for deterministic, repeatable schema versioning.
  * Production configuration enforces `spring.jpa.hibernate.ddl-auto=validate`.
  * Versioned SQL scripts (`V1__init_schema.sql`, `V2__service_tickets.sql`, etc.) are maintained in source control and executed automatically on startup, recording checksums in `flyway_schema_history`.
* **Consequences:** Reproducible, auditable database evolution across environments; immediate startup failure if JPA entities diverge from database schema.

---

### ADR-006: Modular Monolith Backend Architecture
* **Context:** Adopting microservices for an organization of 100–1,000 employees introduces distributed transactions, network latency, RPC complexity, and operational overhead that far outweigh organizational need.
* **Decision:** Implement a **Modular Monolith** within a single Spring Boot deployable, partitioned into 6 strictly bounded modules: `core.iam`, `core.profile`, `module.ticket`, `module.workflow`, `module.audit`, and `core.notification`.
  * Inter-module communication is restricted to public Java service interfaces and Spring `ApplicationEventPublisher`. Direct cross-module database writes are strictly prohibited.
* **Consequences:** Preserves atomic in-memory transactions across tickets, workflows, and audit logs; zero network serialization overhead; clean evolutionary boundary for future microservice extraction if needed.

---

### ADR-007: 11-State Ticket Lifecycle Finite State Machine
* **Context:** Ambiguous ticket statuses in legacy ticketing systems result in unresolved tickets getting lost, resolvers prematurely closing tickets without verification, or employees cancelling tickets mid-fulfillment.
* **Decision:** Formally enforce an **11-State Finite State Machine (FSM)**:
  `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`, `REOPENED`.
  * Mandatory reasons ($\ge 15$ characters) for `ON_HOLD`, `REJECTED`, `CANCELLED`, and `REOPENED`.
  * SLA clock pauses in `ON_HOLD` and resumes with extended target in `IN_PROGRESS`.
  * Cancellation prohibited once in `IN_PROGRESS`.
  * Reopening permitted only within 48 business hours of `RESOLVED` and transitions directly to `IN_PROGRESS`. Auto-closure executed after 48 business hours.
* **Consequences:** Eliminates status ambiguity and enforces accountability across resolvers, managers, and employees.

---

### ADR-008: Optimistic Concurrency Control via JPA `@Version` & HTTP 409
* **Context:** In collaborative service management, two users (e.g. manager approving while employee updates description, or two resolvers claiming the same ticket) may submit conflicting updates simultaneously.
* **Decision:**
  1. Add integer column `version` mapped to JPA `@Version` on `ServiceTicket` and `ApprovalTask`.
  2. Spring Data JPA checks `version` on update; a collision throws `OptimisticLockException`.
  3. `@RestControllerAdvice` translates this exception to **HTTP 409 Conflict** with an informative message.
  4. **Strict Policy:** NO blind automated retries for human approval actions. The UI must prompt the user with a collision dialog to review updated state before re-submitting.
* **Consequences:** Eliminates silent lost updates and guarantees transactional consistency.

---

### ADR-009: Mailpit Infrastructure for Email Testing
* **Context:** Testing transactional emails against real SMTP relays risks spamming real employee inboxes or failing builds when external network access is unavailable.
* **Decision:** Integrate **Mailpit** (SMTP port 1025, Web UI port 8025) as a standard service in `docker-compose.yml` for local and staging environments. Automated Spring Boot unit tests use `@MockBean JavaMailSender` to verify email generation without opening network sockets.
* **Consequences:** Safe, isolated, zero-cost email verification with zero risk of external email leakage.

---

### ADR-010: Java 25 LTS Baseline & Spring Boot 4.1.x Target Evaluation
* **Context:** Long-Term Support (LTS) is mandatory for enterprise runtime longevity. The project evaluates Java 25 LTS and Spring Boot 4.1.x.
* **Mandatory Policy Analysis:**
  * User instruction: *"Do not silently retain Java 25 compilation with Java 21 production runtime."*
  * **Candidate A (Spring Boot 4.1 + Java 21 LTS):** Solid LTS, but requires major dependency upgrades across Hibernate 7, Spring Security 7, and Jakarta EE 11.
  * **Candidate B (Spring Boot 4.1 + Java 25 LTS):** Preferred long-term target architecture. Provides maximum support runway (supported through 2030+). Target design aligns all package architectures, security APIs, and container images with this combination upon GA.
  * **Candidate C (Current Baseline: Spring Boot 3.4.12 + Java 25 LTS runtime):** Verified in local environment where all 57 classes compile cleanly with OpenJDK 25.0.2 LTS. **Important Notice:** Spring Boot 3.4.x was officially validated by VMware/Broadcom up to Java 23; running with Java 25 is an experimental development baseline. We do not claim official vendor certification for Spring Boot 3.4 on Java 25.
* **Decision:**
  1. Standardize the runtime strictly on **Java 25 LTS** across both development compilation and production container stages (`eclipse-temurin:25-jre-alpine`), eliminating runtime version drift.
  2. Retain Spring Boot 3.4.12 as the verified implementation baseline for initial development, transitioning to Spring Boot 4.1.x when GA releases of Spring Framework 7, Spring Boot 4, and JJWT 0.12+ are certified.
  3. Design all HLD/LLD interfaces, security filter chains, and Jakarta EE namespaces to be forward-compatible with Spring Boot 4.1.x and Spring Security 7.x.

---

### ADR-011: Transactional SHA-256 Hash-Chained Audit Logging & Privilege Separation
* **Context:** Service records and approval authorizations require verifiable non-repudiation. Misrepresenting simple database logs as "immutable" or using asynchronous listeners that break atomicity is a critical compliance hazard.
* **Decision:**
  1. Implement **Transactional Local Cryptographic Hash Chaining**: Every `AuditLog` entry stores `previousHash`, `currentHash`, `payloadHash`, and `sequenceNumber`.
     $$H_n = \text{SHA256}(H_{n-1} \parallel \text{Seq}_n \parallel \text{PayloadHash}_n)$$
  2. **Direct Synchronous Transactional Persistence:** Audit logging executes synchronously via direct method invocation inside the **same ACID transaction** as domain mutations (`@Transactional(isolation = Isolation.READ_COMMITTED)`). If an audit write or hash calculation fails, the entire transaction rolls back. Fire-and-forget `@Async` execution or `AFTER_COMMIT` listeners for core audit events are strictly prohibited.
  3. **Decoupled Asynchronous Notifications:** Notifications and email dispatches are subscribed via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with `@Async`, ensuring emails are only dispatched after successful DB commit and SMTP failures never impact the database transaction.
  4. **Database Privilege Separation:** In production MySQL, `epams_app` is granted `SELECT, INSERT` on `audit_log`, with `UPDATE` and `DELETE` explicitly revoked.
  5. **Honest Documentation Policy:** Formally classified as an **"Append-Only, Cryptographically Hash-Chained Audit Trail"**. No claims of hardware immutability are made prior to Phase 2 WORM storage integration.
* **Consequences:** Tamper-evident ledger detecting unauthorized data alterations; robust defense against application-level SQL injection or malicious insiders.

---

### ADR-012: Stateless JWT Authentication with Database-Backed Refresh Token Rotation
* **Context:** Stateful HTTP sessions require server-side session clustering and complicate horizontal container scaling. Purely stateless JWTs cannot be revoked if a user is terminated or compromised.
* **Decision:** Adopt a hybrid approach:
  * Short-lived stateless JWT access tokens (15-minute expiration) carried in the `Authorization: Bearer` header.
  * Long-lived refresh tokens (7-day expiration) stored in the database and transmitted in an `HttpOnly`, `Secure`, `SameSite=Lax` cookie.
  * Refresh token rotation: Every refresh operation revokes the old token and issues a new one. Logout immediately revokes the active token in the database.
* **Consequences:** Maximizes API performance while preserving immediate session revocation capabilities.

---

### ADR-013: Department-Level Queue Access Isolation
* **Context:** Service specialists in one department (e.g. IT) should not have access to sensitive personal requests in another department (e.g. HR grievances or disciplinary requests).
* **Decision:** Enforce department-level isolation in `DepartmentQueueService` and `TicketController` via Spring Security method expressions (`@PreAuthorize`). A resolver can only query and claim tickets where `ticket.department == user.profile.department`, unless granted `ROLE_ADMIN`.
* **Consequences:** Restricts lateral data access and enforces organizational confidentiality.

---

### ADR-014: Sensitive Field Masking at Service & Serializer Boundary
* **Context:** Under data privacy regulations (such as India's DPDP Act 2023), employee compensation data (`salary`) must not be visible to general colleagues browsing the company directory.
* **Decision:** Implement compensation masking at the Service Layer during DTO mapping. When `/api/v1/profiles` or `/api/v1/profiles/{id}` is queried, `salary` is populated only if the caller holds `ROLE_HR` or is retrieving their own profile (`callerUsername == profile.username`). For all other callers, `salary` is set to `null`.
* **Consequences:** Guarantees regulatory data minimization without requiring complex database-level column masking.

---

### ADR-015: Synchronous Local Chain Head Row Locking for Audit Serialization
* **Context:** In multi-threaded environments, concurrent transactions computing SHA-256 hashes would experience race conditions and chain forking if reading the latest record concurrently.
* **Decision:** Introduce a single-row state anchor table `audit_chain_head` (`id = 1`). Transactions acquiring the next sequence execute:
  `SELECT current_hash, last_sequence FROM audit_chain_head WHERE id = 1 FOR UPDATE;`
  This row-level lock serializes chain updates for $< 1\text{ ms}$ per transaction, guaranteeing gapless sequence numbers and deterministic hash chaining without distributed locks.
* **Consequences:** Prevents hash chain forks and sequence collisions under high concurrency.

---

### ADR-016: Separation and Boundary Alignment of the Three Ticket Lifecycle Clocks
* **Context:** EPAMS operates three distinct lifecycle timers over the canonical `Asia/Kolkata` business calendar (Monday–Friday, 09:00–18:00 IST, excluding weekends and configured holidays): (1) Resolution SLA, (2) 48-business-hour Dispute/Reopen Window (`BR-003`), and (3) Scheduled Auto-Closure (`HLD.md` §5.3). Early implementation accidentally evaluated Clocks 2 and 3 using wall-clock `plusHours(48)` and left their boundary relationship implicit.
* **Decision:**
  1. **Clock 1 (Resolution SLA):** Runs across active fulfillment states (`SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`), pauses during `ON_HOLD` (accumulating `total_paused_minutes`), and stops permanently upon `RESOLVED`. Scanned periodically by `SlaScheduledProcessor.processSlaAlerts()` with `ON_HOLD` explicitly excluded.
  2. **Clock 2 (Dispute / Reopen Window):** Starts at `resolvedAt` (normalized to `09:00` next business day if resolved outside business hours) and runs for **48 accumulated business hours** via `BusinessCalendar.calculateReopenDeadline(resolvedAt)`. The boundary is **INCLUSIVE** of the exact deadline instant (`evaluationTime <= deadline`).
  3. **Clock 3 (Automatic Closure Eligibility):** Intentionally linked directly to the expiration of Clock 2 (`HLD.md` §5.3) with a **STRICTLY EXCLUSIVE** lower bound (`evaluationTime > calculateReopenDeadline(resolvedAt)`). `SlaScheduledProcessor.processAutoClosures()` transitions eligible `RESOLVED` tickets to `CLOSED` through `TicketStateMachine` under `SYSTEM_AUTO_CLOSURE`, recording a chained audit event and deduplicated `AFTER_COMMIT` notification.
* **Consequences:** Guarantees zero gap and zero overlap between employee reopen eligibility and automated system closure at every instant while preserving independent SLA pause semantics.


