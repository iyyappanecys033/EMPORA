# Product & Architecture Decision Log (ADR)
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-ADR-001  
**Version:** 2.1.0  
**Date:** November 2026  
**Status:** Approved / Documented  
**Author:** Senior Enterprise Software Architect & Senior Product Manager  

---

## 1. Context and Objective

This document captures the key architectural, product, and technical decisions formulated during Phase M1 (Requirements Refinement). Each Architectural Decision Record (ADR) outlines the business context, alternatives considered, chosen direction, technical trade-offs, and consequences for the EPAMS engineering roadmap.

---

## 2. Decision Log Summary

| ADR ID | Topic | Decision | Status | Impact Area |
| :--- | :--- | :--- | :--- | :--- |
| **`ADR-001`** | Approval Hierarchy Depth | Single-level approval by default; conditional two-level approval for high-impact/privileged requests; configurable financial thresholds. | **Approved** | Workflow Engine, Governance |
| **`ADR-002`** | Frontend Build & Development Architecture | Decouple React dev server (`npm start`) during development; bundle via multi-stage Docker in production. | **Approved** | DX, Build Pipeline |
| **`ADR-003`** | Production Containerization | Multi-stage Docker build producing a minimal JRE runtime image. | **Approved** | DevOps, Deployment |
| **`ADR-004`** | Production Persistence Tier | MySQL 8.0+ as the primary transactional RDBMS; retain H2 for local testing. | **Approved** | Database, Storage |
| **`ADR-005`** | Database Schema Evolution | Adopt Flyway database migrations; disable `hibernate.ddl-auto=update` in production. | **Approved** | Data Integrity, CI/CD |
| **`ADR-006`** | Backend Architectural Style | Modular Monolith with bounded packages over Microservices. | **Approved** | Architecture, Testing |
| **`ADR-007`** | Ticket Lifecycle State Machine | 11-State Finite State Machine (FSM) with deterministic transitions, cancellation rules, and mandatory reasoning. | **Approved** | Ticketing Engine, SLAs |
| **`ADR-008`** | Concurrency Control on Ticket Mutations | JPA `@Version` Optimistic Locking with HTTP 409 Conflict handling; no blind automated retries for approvals. | **Approved** | Data Consistency, Security |
| **`ADR-009`** | Email Integration Testing Infrastructure | Mailpit container for local integration testing; `@MockBean JavaMailSender` for automated unit tests. | **Approved** | Testing, Notifications |
| **`ADR-010`** | Java & Spring Boot Technology Lifecycle Baseline | Maintain Spring Boot 3.4.12 + Java 25 LTS baseline in dev; evaluate Spring Boot 4.x / Java 21 enterprise baselines. | **PROPOSED (Pending Migration Testing)** | Runtime, JVM, Framework |
| **`ADR-011`** | Audit Logging Architecture & Integrity Classification | Transactional Local Outbox with SHA-256 Hash Chaining & Database-Level Privilege Restrictions; no unverified immutability claims. | **Approved** | Security, Compliance, Audit |

---

## 3. Detailed Decision Records

### ADR-001: Single-Level Approval Default with Configurable Two-Level Escalation & Privileged Access Policies

* **Context:**  
  In mid-sized organizations (100–1,000 employees), service requests vary widely in risk and financial impact. Routine requests (HR letters, software access) should not be choked by multi-level bureaucracy. However, capital hardware allocation (laptops) or elevated data permissions carry budget and security liabilities.
* **Decision:**  
  Adopt a **configurable hybrid approval model**:
  * **Standard Rule (Single-Level):** By default, requests requiring authorization route solely to the requester's direct Reporting Manager (`ROLE_MANAGER`). Once approved, the ticket transitions immediately to `ASSIGNED` in the respective department queue.
  * **Configurable Financial Threshold:** Any reimbursement or expense request exceeding an administrator-configurable threshold (`app.workflow.approval.financial-threshold`, default ₹10,000) automatically escalates to a two-level approval (Manager $\rightarrow$ Department Head).
  * **Stricter Privileged Access Policy:** Any request classified as `PRIVILEGED_ACCESS` (administrative role assignment, database credentials, production infrastructure keys) MUST mandate a secondary approval from the Department Head and validation by the Security Administrator (`ROLE_ADMIN`).
  * **Anti-Self-Approval Enforcement:** Users cannot approve their own requests under any circumstances (`BR-005`, `BR-010`). Requests raised by managers automatically route to their department lead or executive supervisor.
* **Consequences:**  
  * *Positive:* Balances speed for 85% of routine requests with strict fiscal and security controls for high-risk actions.
  * *Negative:* Workflow engine must handle conditional branch evaluations based on runtime ticket attributes.

---

### ADR-002: Decoupled React Frontend Architecture During Development

* **Decision:**  
  Maintain a **Monorepo with Decoupled Runtime Development**:
  * In local development, the React frontend runs independently on Node.js / Webpack Dev Server (`http://localhost:3000`) with Hot Module Replacement (HMR).
  * The frontend `package.json` configures `"proxy": "http://localhost:8080"` to transparently forward `/api` calls to the local Spring Boot backend.
  * For production builds, the unified Maven build or Dockerfile bundles the optimized static assets into the JAR.
* **Consequences:**  
  * *Positive:* Instant feedback loop (HMR $< 500\text{ ms}$), independent frontend dependency management, cleaner linting.
  * *Negative:* Developers must run two terminal processes (`mvn spring-boot:run` and `npm start`) during local development.

---

### ADR-003: Multi-Stage Docker Production Build

* **Decision:**  
  Implement a **two-stage container build** via `Dockerfile`:
  * **Build Stage (Builder):** Uses `maven:3.9-eclipse-temurin-21` (or 25) with Node.js to compile Java sources and build production React assets (`npm run build`).
  * **Runtime Stage (Runner):** Copies *only* the single executable `springapp-0.0.1-SNAPSHOT.jar` into a minimal `eclipse-temurin:21-jre-alpine` container.
  * Non-root container user (`spring:spring`) executes the process.
* **Consequences:**  
  * *Positive:* Image size drops to $< 250\text{ MB}$, zero build compilers in production, immutable artifact, reproducible CI/CD.

---

### ADR-004: MySQL 8.0+ as the Primary Production Transactional RDBMS

* **Decision:**  
  Standardize on **MySQL 8.0+** with InnoDB storage engine for all staging and production deployments.
  * Retain H2 *strictly* for unit and automated integration test suites (`@SpringBootTest`).
  * Enforce strict UTF8MB4 character encoding and UTC server timezone.
  * Configure HikariCP connection pooling (`maximum-pool-size=10`, `connection-timeout=20000ms`).

---

### ADR-005: Flyway Database Schema Migrations

* **Decision:**  
  Integrate **Flyway** for deterministic, repeatable schema versioning:
  * Disable Hibernate auto-DDL in production (`spring.jpa.hibernate.ddl-auto=validate`).
  * Store versioned migration scripts in `src/main/resources/db/migration/` (`V1__init_schema.sql`, `V2__service_tickets.sql`, etc.).
  * Migrations execute automatically on application startup, recording checksums in `flyway_schema_history`.

---

### ADR-006: Modular Monolith Backend Architecture

* **Decision:**  
  Adopt a **Modular Monolith** pattern within a single Spring Boot deployable:
  * Enforce clean package boundaries:
    * `com.examly.springapp.core.iam`: Identity, authentication, token lifecycle, security filter.
    * `com.examly.springapp.core.profile`: Employee master records, organization directory, department mapping.
    * `com.examly.springapp.module.ticket`: Service catalog, tickets, comments, SLA tracking.
    * `com.examly.springapp.module.workflow`: Approval tasks, hierarchy resolution, decision state transitions.
    * `com.examly.springapp.module.audit`: Tamper-evident audit logging.
    * `com.examly.springapp.module.notification`: In-app alerts, transactional email dispatch.
  * Modules communicate via strongly typed Java service interfaces and event listeners (`ApplicationEventPublisher`), not direct cross-module database writes.

---

### ADR-007: Comprehensive Ticket Lifecycle Finite State Machine (11 States)

* **Context:**  
  Service requests transition through multiple human and administrative stages. Ambiguous or missing statuses lead to tickets becoming "lost in limbo" or resolvers closing tickets prematurely.
* **Decision:**  
  Formally define an **11-State Finite State Machine (FSM)**:
  1. `SUBMITTED`: Created by employee; awaiting workflow evaluation.
  2. `PENDING_APPROVAL`: Enqueued for direct Reporting Manager review.
  3. `PENDING_SECONDARY_APPROVAL`: Enqueued for Department Head review (for `HIGH_IMPACT` / privileged requests per `ADR-001`).
  4. `ASSIGNED`: Approvals complete; routed to department queue and claimed by a resolver.
  5. `IN_PROGRESS`: Actively being worked on by assigned specialist.
  6. `ON_HOLD`: Work paused awaiting requester input or third-party dependency; **mandates hold reason**. **SLA clock PAUSES**.
  7. `RESOLVED`: Resolver marks work completed; triggers employee verification window. **SLA clock STOPS**.
  8. `CLOSED`: Terminal state; employee confirms resolution or auto-closed after 48 business hours.
  9. `REJECTED`: Terminal state; denied by manager, dept head, or resolver; **mandates rejection reason ($\ge 15$ chars)**.
  10. `CANCELLED`: Terminal state; employee withdraws request prior to active work (`SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ON_HOLD`).
  11. `REOPENED`: Employee disputes resolution within 48 hours; **mandates reopen reason ($\ge 15$ chars)**; transitions directly to `IN_PROGRESS` (never straight to `RESOLVED`). **SLA clock RESUMES with extended target**.
* **Transition Guard Rules:**
  * Once a ticket enters `IN_PROGRESS`, an employee cannot unilaterally cancel; they must request cancellation via comments, and resolver acts.
  * `REJECTED`, `CLOSED`, and `CANCELLED` are immutable terminal states; no transitions out of terminal states are permitted.

---

### ADR-008: Optimistic Concurrency Control via JPA `@Version` & No Blind Retries for Approvals

* **Decision:**  
  1. Add `@Version private Long version;` to `ServiceTicket` entity.
  2. Spring Data JPA automatically checks the version integer on update. If a concurrent modification occurred, it throws `OptimisticLockException`.
  3. `@RestControllerAdvice` catches `OptimisticLockException` and returns **HTTP 409 Conflict** with a structured payload:
     ```json
     {
       "error": "CONFLICT",
       "message": "This ticket was modified by another user. Please refresh and review the updated details before taking action."
     }
     ```
  4. **Strict Policy:** NO blind automatic retries for approval or rejection operations. An approval is a human governance action based on specific ticket data. The UI must prompt the user with a collision dialog and reload the latest state.

---

### ADR-009: Email Testing Architecture (Mailpit Container & MockMailSender)

* **Decision:**  
  * **Local Integration Testing:** Include **Mailpit** (SMTP port 1025, Web UI port 8025) in `docker-compose.yml` to capture all outbound transactional emails without real SMTP credentials.
  * **Unit & Slice Testing:** Configure `@MockBean private JavaMailSender mailSender;` in Spring Boot test suites to verify that mail triggers fire with correct recipient addresses and subject lines without opening socket connections.

---

### ADR-010: Java & Spring Boot Technology Lifecycle Baseline

* **Status:** **PROPOSED (Subject to Migration Testing)**
* **Context & Mandatory Lifecycle Policy:**
  Long-Term Support (LTS) is a mandatory selection criterion for this project.
  1. Java 25 LTS is the preferred long-term Java baseline.
  2. The project must evaluate Spring Boot 4.1.x as the target framework generation while maintaining production stability.
  3. We must not select an unsupported Spring Boot release merely because it exists in the repository.
  4. Note that Spring Boot does NOT share the LTS naming or lifecycle conventions of Java (Spring Boot minor versions receive ~12 months OSS support and ~15–24 months commercial support).

* **Detailed Evaluation of Candidate Combinations:**

| Dimension | Candidate A: Spring Boot 4.1 + Java 21 LTS | Candidate B: Spring Boot 4.1 + Java 25 LTS | Candidate C: Spring Boot 3.4.12 (Current) + Java 25 LTS (Dev) / Java 21 LTS (Prod) |
| :--- | :--- | :--- | :--- |
| **Official Compatibility** | Full official alignment (Spring Boot 4 baselines on Java 21 minimum). | Full official alignment (Spring Framework 7 / Boot 4 designed to support Java 25 LTS). | Verified runtime compatibility: Spring Boot 3.4.12 compiles and executes with Java 25 LTS in our test environment; Java 21 is the certified enterprise baseline. |
| **Dependency Compatibility** | Requires major dependency upgrades: Hibernate 7.x, Spring Security 7.x, Jakarta EE 11 namespaces, JJWT 0.12.x+. | Requires major dependency upgrades across all libraries; ecosystem maturity still evolving for bleeding-edge releases. | 100% verified with current dependencies: Hibernate 6.6.36, JJWT 0.11.5, Spring Security 6.4, MySQL Connector 9.x. Zero migration breakages today. |
| **Breaking Changes & Migration Risks** | High: Deprecated API removals in Spring Security 7 (filter chain refactoring, authorizeHttpRequests alterations), Jakarta EE 11 package shifts. | High: High risk of third-party annotation processor or bytecode manipulation incompatibilities with early Java 25 tooling. | Very Low in present state: All 57 classes compile cleanly with javac release 25; test suite passes without deprecation warnings. |
| **LTS & Support Considerations** | Java 21 LTS supported through at least September 2028 (commercial through 2031). Spring Boot 4.x will have standard 12-month OSS support per minor release. | Java 25 LTS supported through September 2030 (commercial through 2033). | Java 25 LTS provides maximum 5-year runway. Spring Boot 3.4.x has commercial support into 2026/2027. |
| **Testing Required** | Comprehensive regression testing of all security filters, JWT parsing, and JPA entity listeners under Spring Boot 4. | Extensive bytecode validation, surefire testing, and container verification on bleeding-edge JDK 25 images. | Smoke tests and integration tests already executed and passing in current workspace. |

* **Exact Java Distribution & Licensing:**
  * **Distribution:** `Eclipse Adoptium Temurin 25.0.2+10-LTS` (OpenJDK 64-Bit Server VM Temurin-25.0.2+10).
  * **License:** GNU General Public License v2 with Classpath Exception (GPLv2+CE), permitting free, unrestricted commercial production use with zero vendor lock-in.

* **Architectural Recommendation:**
  * **Phase M1 / Initial Baseline:** Maintain the current proven baseline (**Spring Boot 3.4.12 + Java 25 LTS runtime in development / Java 21 LTS in production containers**).
  * **Upgrade Roadmap Policy:** When Spring Boot 4.1 reaches General Availability (GA) and third-party libraries (specifically JJWT and Hibernate) achieve stable GA validation, execute a formal migration spike to Spring Boot 4.1 + Java 25 LTS with full regression testing.

---

### ADR-011: Audit Logging Architecture & Integrity Classification

* **Context:**  
  Enterprise audit logging is frequently misrepresented as "immutable" or "tamper-evident" without architectural mechanisms to support such claims.
* **Precise Technical Classification:**
  1. **Audit Trail:** A chronological log of system, security, and domain events. *(EPAMS currently implements this).*
  2. **Append-Only Storage:** Storage where records can only be inserted, with update and delete permissions revoked at the database user privilege level. *(Recommended for Phase M2).*
  3. **Tamper-Evident Logging:** Log entries linked cryptographically (e.g., cryptographic hash chaining, where record $N$ includes a SHA-256 hash of record $N-1$ and its own payload). Any retroactive alteration to historical logs breaks the chain and is mathematically detectable. *(Recommended for Phase M2).*
  4. **Immutable Storage (WORM):** Hardware or cloud-enforced Write Once Read Many storage (e.g., AWS S3 Object Lock in Compliance Mode) where no identity—including database administrators or cloud root accounts—can alter or delete records before a designated retention expiration. *(Out of scope for initial on-premises / single-container deployment).*

* **Evaluation of Architecture Options for ~1,000 Employees (~200–500 tickets/day):**
  * *Option 1 (Current Synchronous Write):* Simple, but ties API latency directly to audit database I/O.
  * *Option 2 (Simple Asynchronous `@Async`):* Fast, but unpersisted audit events in thread memory will be permanently lost if the JVM crashes before writing to the database.
  * *Option 3 (Transactional Outbox with Cryptographic Hash Chaining):* Audit events are written to the database within the *same ACID transaction* as the domain mutation. Each record computes `hash = SHA256(previousHash + timestamp + actor + action + target + details)`.

* **Decision for EPAMS:**
  1. Adopt **Transactional Local Persistence with SHA-256 Hash Chaining**: Every `AuditLog` entry stores `previousHash` and `currentHash`.
  2. **Database Privilege Separation:** In production MySQL, the application runtime user is granted `INSERT` and `SELECT` on `audit_log`, but strictly denied `UPDATE` and `DELETE` privileges.
  3. **Honest Documentation Policy:** EPAMS will formally describe its audit subsystem as an **"Append-Only, Cryptographically Hash-Chained Audit Trail"**. We will **NOT** claim "hardware immutability" or "unconditional tamper-proofing" against hypervisor-level or cloud-root attacks.
