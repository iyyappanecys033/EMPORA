# EPAMS 2.0 — R1 / R1.1 Product Strategy Decision Register

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1.1 — Customer Validation Execution (Updated from R1 Baseline)  
**Last Updated:** 2026-11-18  
**Git Baseline:** `6205bd5a9bc06a6dafbb259f73e8215325537f2e`  
**Governance Rule:** Only decisions explicitly authorized by the user may move to an `APPROVED*` status. Decisions not explicitly approved by the user remain preserved in their prior status (`PROPOSED`, `REQUIRES VALIDATION`, `DEFERRED`, or `REJECTED`).

**Allowed Statuses:** `APPROVED` | `APPROVED — PROVISIONAL` | `APPROVED — HYPOTHESIS TO VALIDATE` | `PROPOSED` | `REQUIRES VALIDATION` | `DEFERRED` | `REJECTED`

---

## 1. Master Decision Summary Table

| Decision ID | Decision Summary | Decision Date | Approval Status | Supporting Evidence | Remaining Assumptions / Validation Gate |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **`R1-DEC-001`** | Adopt **EPAMS — Lightweight, Self-Hosted, Governance-First Employee Service & Workflow Platform** as core positioning hypothesis | 2026-11-18 | **`APPROVED — PROVISIONAL`** | `r1-market-research.md` (Atlassian JSM DC sunset Mar 2026–Mar 2029; SaaS paywalls); `r1-existing-product-assessment.md` (single-JAR + SHA-256 audit moat) | Unvalidated customer preference for self-hosting vs. SaaS; must validate via R1.1 interviews (`H1`, `H3`) |
| **`R1-DEC-002`** | Target **`100`–`1,000`-employee** organisations (initial sweet spot **`150`–`750`**), privacy-sensitive focus; buyers: Head of IT, SysAdmin, VP Ops; co-sponsors: HR Ops & InfoSec | 2026-11-18 | **`APPROVED — HYPOTHESIS TO VALIDATE`** | `r1-market-research.md`, `r1-competitor-analysis.md` | Unvalidated demand volume, budget ownership, cross-dept buying authority, and self-hosted readiness (`H1`, `H4`) |
| **`R1-DEC-003`** | Adopt 3 provisional strategic initiatives: **P1** Self-Hosted Foundation & Knowledge-First Self-Service (Vite tracked as separate engineering maintenance); **P2** Configurable Cross-Dept Workflow Engine; **P3** Employee Lifecycle Orchestration | 2026-11-18 | **`APPROVED — PROVISIONAL`** | `r1-problem-discovery-and-prioritisation.md`, `r1-innovation-opportunity-analysis.md`, `r1-existing-product-assessment.md` | Strategic priorities only (**zero implementation authorized**); must validate problem urgency & switching interest per capability (`H2`, `H3`) |
| **`R1-DEC-004`** | Purpose-built declarative DAG Workflow Engine in Spring Boot + MySQL 8 with allowlisted actions (vs. external Camunda/Flowable BPMN engine) | 2026-11-18 *(Unchanged from R1)* | **`PROPOSED`** | `r1-technical-feasibility-assessment.md` | Technical architecture decision deferred to R2 after R1.1 validation confirms workflow demand |
| **`R1-DEC-005`** | Exclude Core HRIS/Payroll/Attendance, ITAM/CMDB network discovery, external B2C support desk, cloud-LLM chatbots, premature multi-tenancy, and microservices | 2026-11-18 *(Unchanged from R1)* | **`PROPOSED`** | `r1-competitor-analysis.md`, `r1-product-positioning.md` | User has not yet issued separate formal non-goal rejection sign-off; test whether ITAM/HRIS absence is a dealbreaker in R1.1 (`H3`) |
| **`R1-DEC-006`** | Retain Modular Monolith (Java 25 + Spring Boot 3.5 + MySQL 8) and execute Vite + Vitest frontend migration (plus React Router v7) as separate engineering maintenance | 2026-11-18 *(Unchanged from R1)* | **`PROPOSED`** | `r1-existing-product-assessment.md`, `stage1-git-release-audit.md` | Vite noted as separate maintenance activity under `R1-DEC-003`; formal engineering scheduling pending R2/maintenance approval |
| **`R1-DEC-007`** | **Enforce mandatory R1.1 Customer Validation Gate (`18`–`24` interviews; `50`–`100` secondary survey target) prior to full R2 SRS & Architecture work** | 2026-11-18 | **`APPROVED`** | `r1-customer-validation-plan.md`, `R1-USER-DECISION-RECORD.md` | **Active binding gate** — R2 cannot begin without documented customer evidence meeting `r1.1-r2-entry-criteria.md` and explicit user sign-off |
| **`R1-DEC-008`** | Evaluate Open-Core / Per-Agent / Per-Organisation commercial models during customer validation; defer ML Predictive SLA (Level 7) & multi-tenancy | 2026-11-18 *(Unchanged from R1)* | **`DEFERRED`** | `r1-business-value-and-metrics.md`, `r1-product-roadmap.md` | Commercial packaging requires buyer evidence (`r1.1-commercial-validation-framework.md`, `H4`); ML/multi-tenancy deferred |

---

## 2. Detailed Audit History of User-Updated Decisions (R1 → R1.1)

### `R1-DEC-001` — Product Positioning
- **Original R1 Proposal (`PROPOSED`):** Adopt Direction C (Refined): Lightweight, Self-Hosted, Governance-First Employee Service & Workflow Platform.
- **User-Approved Decision (`2026-11-18`):** **`APPROVED — PROVISIONAL`** — Adopt *EPAMS — Lightweight, Self-Hosted, Governance-First Employee Service & Workflow Platform* as the core positioning hypothesis (self-hosted employee services + cross-department workflows + transparent governance + simpler deployment).
- **Evidence Supporting Decision:** Atlassian JSM Data Center sunset (new sales ended March 30, 2026; EOL March 28, 2029), cloud SaaS tier/transaction paywalls (Freshservice, Zoho People, Power Automate), and EPAMS's verified single-JAR + SHA-256 tamper-evident audit foundation.
- **Remaining Assumptions:** Target buyers value self-hosted simplicity and cryptographic governance enough to evaluate a newer platform over incumbent cloud SaaS or GLPI/Zammad/ManageEngine.
- **Validation Requirements:** Must validate via `H1` & `H3` across `18`–`24` interviews before locking R2 positioning.
- **Consequences for Roadmap:** Guides R1.1 concept testing and competitor switching questions; prohibits cloud-only multi-tenant or AI-chatbot scope creep.

### `R1-DEC-002` — Target Segment
- **Original R1 Proposal (`REQUIRES VALIDATION`):** Focus on privacy-sensitive / regulated `100`–`1,000`-employee organisations (`150`–`750` sweet spot).
- **User-Approved Decision (`2026-11-18`):** **`APPROVED — HYPOTHESIS TO VALIDATE`** — `100`–`1,000` employees (`150`–`750` initial sweet spot), potential privacy-sensitive focus; potential buyers: Head of IT, Systems Administrator, VP Operations; potential co-sponsors: HR Operations & InfoSec.
- **Evidence Supporting Decision:** Competitor gap analysis showing ServiceNow overkill below 1,000 employees and startup indifference to self-hosting below 100 employees.
- **Remaining Assumptions:** Buying authority alignment across IT/HR/Ops, available software budget, and operational appetite to self-host are all **`UNKNOWN` / UNVALIDATED**.
- **Validation Requirements:** Stratified recruitment across `100–149`, `150–300`, `301–750`, `751–1,000` employee bands and all 6 stakeholder roles (`r1.1-participant-recruitment-plan.md`).
- **Consequences for Roadmap:** Target segment cannot be locked into R2 SRS until `H1` and `H4` evidence confirms demand, authority, budget, and adoption readiness.

### `R1-DEC-003` — Strategic Initiatives
- **Original R1 Proposal (`PROPOSED`):** Top 3 capabilities (Foundation/KB/OIDC/Vite → Configurable Workflow Engine → Lifecycle Orchestration).
- **User-Approved Decision (`2026-11-18`):** **`APPROVED — PROVISIONAL`** — Priority 1: Self-Hosted Adoption Foundation & Knowledge-First Self-Service (Catalog Admin, KB, role-scoped KB access, search/discovery, OIDC SSO, secure self-hosted packaging; Vite migration separated as engineering maintenance); Priority 2: Configurable Cross-Department Workflow Engine (declarative definitions, conditions/controlled transitions, allowlisted actions, approval orchestration, dry-run simulation, versioning & version-pinned durable execution); Priority 3: Employee Lifecycle Orchestration (onboarding, transfers, offboarding, cross-dept dependencies, approval-gated access revocation, blocker/progress visibility).
- **Evidence Supporting Decision:** Verified repo gaps (read-only Catalog API, zero KB, hardcoded 2-stage approvals, local auth only) + competitor gap synthesis.
- **Remaining Assumptions:** Buyers consider these three capabilities high enough priority to justify switching or net-new adoption; absence of ITAM/CMDB or core payroll does not block adoption.
- **Validation Requirements:** Neutral concept testing across all three initiatives (`r1.1-solution-concept-testing.md`) measuring problem importance, switching willingness, and must-have scope.
- **Consequences for Roadmap:** Establishes provisional priority order for R1.1 validation only; **zero implementation or R2 design work authorized yet**.

### `R1-DEC-007` — Mandatory Validation Gate
- **Original R1 Proposal (`REQUIRES VALIDATION`):** Execute Workstream D customer validation before R3+ coding (with optional parallel R2 design).
- **User-Approved Decision (`2026-11-18`):** **`APPROVED`** — Actual customer validation **must occur before full R2 architecture and SRS work**. Gate must not be bypassed.
- **Evidence Supporting Decision:** Zero live customer interviews conducted in R1; high risk of building unvalidated workflow/orchestration architecture.
- **Remaining Assumptions:** Target sample (`18`–`24` interviews; `50`–`100` secondary survey responses) can be recruited across the 4–6 week R1.1 execution window.
- **Validation Requirements:** Satisfy all 8 criteria in `r1.1-r2-entry-criteria.md` with logged primary evidence in `r1.1-validation-evidence-register.md`.
- **Consequences for Roadmap:** **Hard stop before R2.** No `docs/epams-2.0/` SRS/HLD/LLD or feature implementation may begin until customer evidence is collected, reviewed, and explicitly approved by the user.
