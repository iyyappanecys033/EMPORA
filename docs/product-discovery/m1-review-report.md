# Phase M1 Review & Requirements Refinement Report
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-REV-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Ready for Formal Stakeholder Sign-Off  
**Author:** Senior Product Manager, Lead Business Analyst & Enterprise Software Architect  

---

## 1. Executive Summary

This report concludes **Phase M1: Product Discovery and Requirements Analysis** for the **Employee Lifecycle and Service Management Platform (EPAMS)**. 

Through source-level code audits, competitive benchmarking (evaluating Zoho People, ServiceNow HRSD, and **SIH 2025 Problem Statement 25195 – Smart Helpdesk Ticketing Solution for IT Services**), and stakeholder workflow analysis, this report reconciles all initial assumptions into an evidence-based product specification and architectural baseline.

---

## 2. Technical Audit Corrections & Ground Truth

The initial audit claims in `project-audit.md` were rigorously cross-referenced against the actual Java source code and Maven build manifests. The following corrections and verified findings are now established:

| Technology / Component | Initial Claim | Source Code Ground Truth | Verified Status |
| :--- | :--- | :--- | :--- |
| **Java Version** | Java 21 / 25 general | OpenJDK 25.0.2 LTS runtime; Maven configured with `<java.version>25</java.version>` compiling 57 source files with `javac [debug parameters release 25]`. | **Confirmed & Verified** |
| **JWT Implementation** | Generic JWT | `io.jsonwebtoken:jjwt-api:0.11.5` signing with HMAC-SHA256 (`HS256`) using a 256-bit Base64-decoded secret. Access token defaults to 15 min, refresh token to 7 days. | **Confirmed & Verified** |
| **Token Delivery & Fallback** | Cookie-only | `JwtFilter.java` inspects the `accessToken` HTTP-only cookie first; if absent, it gracefully falls back to the `Authorization: Bearer <token>` header, supporting both browser and programmatic API clients. | **Confirmed & Verified** |
| **CSRF Defense** | Standard CSRF | `CookieCsrfTokenRepository.withHttpOnlyFalse()` sets `XSRF-TOKEN` cookie; client passes `X-XSRF-TOKEN` header on mutating requests. Explicitly ignores `/api/auth/login`, `/register`, and `/refresh`. | **Confirmed & Verified** |
| **Password Hashing** | BCrypt mentioned | `BCryptPasswordEncoder` bean instantiated in `SecurityConfig.java` and utilized across `AuthService` and `DataInitializer`. | **Confirmed & Verified** |
| **Brute-Force Lockout** | Unclear mechanism | Implemented in `UserAccount` (`failedLoginAttempts`, `lockedUntil`). Threshold: 5 failed attempts $\rightarrow$ 15-minute lock. Admin can unlock via `PUT /api/users/{id}/unlock`. | **Confirmed & Verified** |
| **RBAC Mapping** | Custom authorities | `CustomUserDetailsService` dynamically prepends `ROLE_` to database role names (`ADMIN` $\rightarrow$ `ROLE_ADMIN`). Enforced via `@EnableMethodSecurity` and `@PreAuthorize`. | **Confirmed & Verified** |
| **Audit Logging** | Async assumed | **Synchronous write** in request thread via `AuditLogServiceImpl.java` using `SecurityContextHolder` caller. Flagged as technical debt for Phase M2 optimization. | **Confirmed & Verified** |
| **Database Tier** | Hybrid H2/MySQL | H2 in-memory is the active development runtime. MySQL connector is present in `.m2`, and dedicated profile `application-mysql.properties` is prepared. | **Confirmed & Verified** |

---

## 3. SIH 2025 PS 25195 & Strategic Positioning

### 3.1 Analysis of SIH 2025 Problem Statement 25195
* **Problem Scope:** "Smart Helpdesk Ticketing Solution for IT Services" focuses on automating IT ticket classification, priority handling, and SLA escalation.
* **Why IT-Only Helpdesks Fail Mid-Market Enterprises:**
  * In Indian organizations with 100–1,000 employees, IT requests account for only a portion of daily operational friction. Employees face equal or greater delays in **HR requests** (bonafide letters, statutory updates), **Facilities issues** (desk allocations, access cards), and **Finance queries** (reimbursements).
  * Standalone IT helpdesks operate in a silo without access to the employee's reporting manager or department head, making multi-tier approvals impossible.
  * Standalone helpdesks cannot coordinate **Joiner-Mover-Leaver (JML)** transitions that simultaneously require IT hardware provisioning, Facilities badges, and HR document verification.

### 3.2 EPAMS Strategic Differentiation
EPAMS adopts the core ticketing, priority, and SLA capabilities of SIH 25195 and elevates them into a **Cross-Department Employee Lifecycle & Service Management Hub**:
* **Single Digital Front Door:** One unified catalog covering IT, HR, Facilities, and Finance.
* **Hierarchy-Aware Approvals:** Leverages natural employee-manager reporting relationships to execute approvals without administrative friction.
* **Lifecycle Continuity:** Coordinates cross-departmental onboarding and exit checklists.

---

## 4. Summary of Architectural Decisions (ADR-001 to ADR-010)

* **`ADR-001` (Approval Hierarchy):** Standard requests require **Single-Level Approval** (Reporting Manager only). High-impact requests (hardware, configurable financial threshold [default ₹10,000], privileged access per `BR-008`, `BR-009`) require **Two-Level Approval** (Manager $\rightarrow$ Department Head). Enforces strict anti-self-approval (`BR-010`).
* **`ADR-002` (Frontend Development):** Decoupled React dev server on `http://localhost:3000` with API proxying to port 8080 for sub-second hot reload during development.
* **`ADR-003` (Containerization):** Multi-stage `Dockerfile` with a minimal Alpine JRE runner image ($< 250\text{ MB}$), running as a non-root user.
* **`ADR-004` (Production RDBMS):** Standardize on **MySQL 8.0+** with HikariCP connection pooling for production; retain H2 for automated integration tests.
* **`ADR-005` (Schema Migrations):** Integrate **Flyway** for versioned SQL migrations (`V1__init.sql`), deprecating Hibernate auto-DDL in production.
* **`ADR-006` (Architectural Pattern):** **Modular Monolith** within a single deployable Spring Boot artifact, enforcing clean package boundaries between IAM, Profile, Ticket, Workflow, and Audit.
* **`ADR-007` (Ticket State Machine):** 11-State Finite State Machine (`SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`, `REOPENED`) with strict validation guards and mandatory rejection reasoning.
* **`ADR-008` (Concurrency Control):** JPA `@Version` optimistic locking on `ServiceTicket` returning HTTP 409 Conflict upon simultaneous collision. **Strict policy: No blind automatic retries for approval decisions**.
* **`ADR-009` (Email Testing):** Embedded **Mailpit** container (SMTP port 1025, Web UI port 8025) in `docker-compose.yml` for offline local verification; `@MockBean JavaMailSender` for automated test suites.
* **`ADR-010` (Runtime Baseline):** Java 25 LTS local compilation; recommended Java 21 LTS enterprise certified baseline for commercial production deployments of Spring Boot 3.4.x.

---

## 5. Final Refined Scope & Requirements Baseline

### 5.1 Final P0 (MVP Core Scope)
1. **Module 1: IAM & RBAC Governance:** Stateless JWT (cookie + Bearer fallback), refresh tokens, lockout protection (5 failed attempts $\rightarrow$ 15 min lock), manual unlock, CSRF defense, CSP headers.
2. **Module 2: Employee Profile & Organization Directory:** Master records (`Profile`), sensitive salary masking, and paginated full-text directory search.
3. **Module 3: Unified Service Request System:** Standardized multi-department catalog (IT, HR, Facilities, Finance), 11-state FSM, threaded activity comments, and SLA timers.
4. **Module 4: Deterministic Approval Workflows:** Single-level default manager routing; conditional two-level department head escalation for high-impact tickets (`BR-001`, `BR-008`, `BR-009`, `BR-010`, `FR-016A`, `FR-016B`).
5. **Module 5: Event Notifications & Audit Ledger:** Real-time in-app notification ledger and immutable, tamper-evident audit logging (`AuditLog`).

### 5.2 Explicit Exclusions from MVP
* Biometric hardware integration.
* Full payroll calculation and disbursement math.
* Applicant tracking / recruitment scheduling.
* Multi-tenant billing / credit card processing.

---

## 6. Complete Documentation Deliverables Created

All discovery and requirements artifacts reside within [docs/product-discovery/](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery):

1. **[project-audit.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/project-audit.md):** Corrected codebase audit, dependency verification, and security observations.
2. **[problem-statement.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/problem-statement.md):** Indian SME segment context, pain point root causes, and persona definitions.
3. **[competitor-analysis.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/competitor-analysis.md):** Commercial evaluation of Zoho People, ServiceNow HRSD, and Freshservice.
4. **[competitor-gap-analysis-v2.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/competitor-gap-analysis-v2.md):** Deep comparison incorporating SIH 2025 PS 25195 and cross-department lifecycle workflows.
5. **[product-vision.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/product-vision.md):** Strategic pillars, stakeholder value propositions, and multi-horizon roadmap.
6. **[product-scope.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/product-scope.md):** Refined MoSCoW priorities, MVP boundaries, exclusions, and operational KPIs.
7. **[SRS-v1.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/SRS-v1.md):** IEEE 830-compliant specification with 11-state FSM, updated approval rules, and concurrency NFRs.
8. **[assumptions-and-risks.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/assumptions-and-risks.md):** Technical and behavioral assumptions with a comprehensive risk severity matrix.
9. **[requirements-traceability-matrix.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/requirements-traceability-matrix.md):** Bidirectional RTM mapping objectives to FRs, NFRs, use cases, and tests.
10. **[product-decision-log.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/product-decision-log.md):** Formal ADRs (ADR-001 through ADR-010).
11. **[m1-signoff-checklist.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/m1-signoff-checklist.md):** Formal sign-off checklist and Phase M2 readiness criteria.
12. **[m1-review-report.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/product-discovery/m1-review-report.md):** (This Document) Synthesis and sign-off report.

---

## 7. Remaining Unresolved Decisions

1. **Async Audit Queue Mechanism:** Should asynchronous audit persistence in Phase M2 be backed by Spring `@Async` with `ThreadPoolTaskExecutor` or an embedded/external message queue (e.g., RabbitMQ)? *(Recommendation for M2: Spring `@Async` ThreadPool to maintain zero-infrastructure simplicity).*
2. **Document Upload Storage Tier for Phase P1:** When file attachments are introduced in Phase P1, should initial storage use local encrypted volume mounts or S3-compatible object storage (MinIO)? *(Recommendation: S3-compatible abstraction layer with MinIO container for local dev).*

---

## 8. M1 Approval Recommendation & Governance Status

**Phase M1 Status: CONDITIONALLY APPROVED (Ready for Phase 1 HLD & LLD)**

1. **Technical & Architectural Baseline:** **APPROVED.** All codebase audits, language/runtime verifications (Java 25 LTS, Spring Boot 3.4.12, Spring Security 6), and architectural decisions (ADR-001 through ADR-011) are complete, verified, and testable.
2. **Product Problem Validation:** **CONDITIONALLY APPROVED.** The target market segment (100–1,000 employees), manager portal responsiveness, and cross-department consolidation are formally classified as **working hypotheses**. A complete, rigorous Product Validation Plan and field interview protocol have been established under `docs/product-validation/` to gather recorded stakeholder evidence in parallel with Phase 1 design.
3. **Exact Recommended Next Task:** Proceed to **Phase 1: High-Level Design (HLD) and Low-Level Design (LLD)** to define database schema scripts (Flyway), entity models, API contracts (OpenAPI), and the state machine transition engine without modifying existing application code.

