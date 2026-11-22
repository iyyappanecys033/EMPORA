# EPAMS 2.0 — Workstream I: Technical & Product Feasibility Assessment

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream I)  
**Audited Baseline:** `6205bd5a9bc06a6dafbb259f73e8215325537f2e`  
**Boundary:** High-level feasibility & architectural impact assessment only (detailed HLD/LLD/schemas belong strictly to R2 after approval).

---

## 1. Existing EPAMS Architectural Foundation Assessment (`VERIFIED FACT`)

| Architectural Layer | Current State | Fit for EPAMS 2.0 (Direction C) | Known Constraints to Address |
| :--- | :--- | :---: | :--- |
| **Modular Monolith (Java `25` + Spring Boot `3.5.16`)** | Clean package separation (`controller`, `service`, `workflow`, `notification`, `security`, `repository`, `model`); `1` executable JAR | **Excellent** — directly enables the "single-binary self-hosted" positioning! Retain modular monolith; **zero microservices needed** | Enforce clear internal package boundaries as new modules (`catalog-admin`, `knowledge`, `workflow-engine`, `lifecycle`, `integrations`) are added |
| **Database (MySQL `8+` + Flyway `V1`–`V4` + Hibernate `6.6`)** | `13` tables + `flyway_schema_history`, `InnoDB`, `utf8mb4`, `ddl-auto=validate`, least-privilege `epams_migrator` / `epams_app` verified on MySQL `8.0.45` | **Excellent** — standard relational engine already understood by every SMB IT team | Keep `V1`–`V4` strictly immutable; all EPAMS 2.0 schemas must arrive as additive **`V5+` Flyway migrations** with documented rollback & brownfield safety |
| **Security (Spring Security `6.5`, JWT `HttpOnly`, CSRF, RBAC)** | `11` roles, department scoping, field-level PII redaction, `11` IDOR vectors verified | **Strong Base** | Must add `spring-boot-starter-oauth2-client` / OIDC (and optional SAML2) cleanly alongside local auth without breaking existing JWT/CSRF session mechanics |
| **Audit Chain (`AuditChainService`, `audit_chain_head`)** | SHA-256 chain + `FOR UPDATE` row lock + MySQL `epams_app` `SELECT, INSERT` only on `audit_log` | **Unique Moat** | Pessimistic lock serializes high-frequency writes — keep hash chain reserved for governance/state/workflow mutations (never spam with KB search impressions!) |
| **Notifications (`NotificationEventListener`, `SmtpEmailDeliveryAdapter`)** | `@TransactionalEventListener(AFTER_COMMIT)` + `uk_alert_recipient_dedup` | **Strong Base** | Generalize delivery adapter interface to support async outbound webhooks / Slack / Teams with retry & dead-letter tracking |
| **Scheduler (`SlaScheduledProcessor`)** | Single-JVM `AtomicBoolean` re-entrancy guard | **Good for 1-node** | Add DB-backed lock (e.g., ShedLock) when supporting `>= 2` replicas or durable workflow timer/escalation polling |
| **Frontend (React `18`, JS, Design System CSS)** | Modern accessible UI (`32/32` `axe-core` `0` violations, `30/30` 3-engine cross-browser) | **Good UX, Dated Build Tool** | Archived **`react-scripts@5.0.1`** (`100` dev-only `npm audit` findings) + `10` legacy CRUD components; migrate build toolchain to **Vite + Vitest** early in EPAMS 2.0 foundation |

---

## 2. Candidate Innovation Feasibility Breakdown

| Candidate Capability | Existing Modules Reused | New Domain Concepts Needed | Likely DB (`V5+`) & API Impact | Security Implications | Operational Complexity Added | Feasibility Verdict |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| **A. Admin Service Catalog CRUD & Form Fields** | `ServiceCatalogItem`, `ServiceCatalogRepository`, `AuditChainService`, `ServiceCatalog.js` | Dynamic form schema JSON (optional Phase 1b) | Zero/minor additive columns (`V5`); `POST`/`PUT`/`PATCH` `/api/v1/admin/catalog` | Strict `ROLE_ADMIN` / dept-admin guard; sanitize custom form definitions | **Minimal** | **HIGH (Immediate Quick Win)** |
| **B. Knowledge Management & Self-Service Deflection** | `SecurityConfig`, `AuditChainService`, `GlobalSearchModal`, `CreateTicketModal`, MySQL `FULLTEXT` index | `KnowledgeArticle`, `KnowledgeArticleVersion`, `KnowledgeFeedback` | `V5` additive tables + MySQL `FULLTEXT` index (no Elasticsearch needed at `100`–`1,000` employee scale!); `/api/v1/kb/**` | Strict visibility filtering (`PUBLIC_EMPLOYEE` vs `INTERNAL_SPECIALIST` vs `DEPARTMENT_SCOPED`); strict Markdown/HTML XSS sanitization | **Low** (zero new infra services!) | **HIGH** |
| **C. Enterprise SSO (OIDC) & Outbound Webhooks** | `AuthService`, `UserAccount`, `NotificationEventListener` | `IdentityProviderConfig`, `WebhookEndpoint`, `WebhookDeliveryLog` | `V6` additive tables; `/api/v1/admin/integrations/**` | Encrypted client secrets at rest; HMAC-SHA256 webhook signing; **SSRF IP allowlist/blocklist** (block `169.254.169.254`, private CIDRs); JIT role-mapping guardrails | **Low–Medium** | **HIGH** |
| **D. Configurable Workflow Automation Engine** | `TicketCommandService`, `ApprovalService`, `SlaScheduledProcessor`, `AuditChainService`, `NotificationEventListener` | `WorkflowDefinition`, `WorkflowVersion`, `WorkflowNode`, `WorkflowTransition`, `WorkflowExecution`, `WorkflowTask`, `WorkflowExecutionLog` | `V7` additive tables; `/api/v1/workflows/**`; version-pinned execution state machine | **CRITICAL:** Zero arbitrary script/JS eval! Use strictly allowlisted condition operators (`==`, `>`, `IN`, `CONTAINS`) & allowlisted action types (`CREATE_APPROVAL`, `ASSIGN_DEPT`, `NOTIFY`, `CREATE_SUBTASK`, `ESCALATE`); backend DAG/cycle validator + dry-run simulator | **Medium–High** (requires durable state persistence & crash recovery tests) | **HIGH (Core R2 Architecture Focus)** |
| **E. Employee Lifecycle Orchestration (Onboarding / Offboarding)** | Built directly **on top of** Configurable Workflow Engine + `Profile` + `ServiceTicket` + `AuditChainService` | `LifecycleJourney`, `LifecycleTemplate`, `JourneyTaskDependency` | `V8` additive tables; `/api/v1/lifecycle/**` | Strict HR PII redaction; mandatory approval gate before any account suspension/deactivation | **Medium** (leverages Workflow Engine!) | **HIGH (Clean Layer on Engine)** |
| **F. Deterministic Operational Intelligence & Smart Assignment** | `AnalyticsService`, `ServiceTicketRepository`, `ApprovalTaskRepository` | `SpecialistSkillProfile`, deterministic rule scorers, bottleneck detector queries | `V9` minor tables/indexes; `/api/v1/intelligence/**` | Zero external LLM data egress; explainable reason list returned with every suggestion; RBAC & dept scope enforced server-side | **Low–Medium** | **HIGH** |
| **G. Governance & Privacy Console** | `AuditChainService`, `AuditLogRepo`, `ProfileController` | `SensitiveAccessLog`, `RetentionPolicy`, audit proof bundle export (signed JSON/CSV) | Additive read-audit table (separate from `audit_chain_head` lock to avoid hot-row contention) | Read-only auditor enforcement; retention anonymization must not invalidate historical SHA-256 chain hashes (use pseudonymized references or chain epoch anchoring) | **Low–Medium** | **HIGH** |

---

## 3. Architectural Option Comparison (Workflow Engine — Preview for R2)

| Option | Pros | Cons | R1 Feasibility Recommendation |
| :--- | :--- | :--- | :--- |
| **Option 1: Embed External BPMN Engine (Camunda 7 / Flowable)** | Rich BPMN 2.0 features out of the box | Heavy ~35–50 extra DB tables, Camunda 7 CE approaching EOL / Camunda 8 requires Zeebe cluster (destroys single-JAR lightweight positioning!), steep learning curve for SMB HR/IT admins | **NOT RECOMMENDED** (violates lightweight single-JAR principle) |
| **Option 2: Purpose-Built Lightweight Declarative DAG Workflow Engine in Spring Boot + MySQL 8** | Exact fit for EPAMS RBAC, SHA-256 audit chain, and 11-state ticket lifecycle; `~6–8` clean Flyway tables; zero extra infrastructure; deterministic dry-run simulator; strict allowlisted actions (zero RCE risk) | Requires careful R2 design for versioning, locking, idempotency, and crash recovery | **RECOMMENDED FOR R2 DESIGN** |

**Overall Feasibility Verdict (`VERIFIED FACT` / `RECOMMENDATION`):** Direction C (Lightweight Self-Hosted, Governance-First Employee Service & Workflow Platform) is **highly feasible** on EPAMS's existing Java 25 / Spring Boot 3.5 / MySQL 8 / React 18 modular monolith with **zero new infrastructure dependencies** (no Elasticsearch, no Redis, no Kafka, no microservices required for 100–1,000 employee deployments).
