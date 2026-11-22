# EPAMS 2.0 — Workstream J: Prioritised Product Roadmap

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream J)  
**Date:** 2026-11-18  
**Planning Discipline:** Phased strictly by dependency order and measurable exit criteria — **zero arbitrary calendar deadlines invented without engineering capacity estimates**.

---

## 1. Initiative Prioritisation Matrix (MoSCoW + Rationale)

| Priority Bucket | Initiative | Strategic Role | Dependency | Evidence / Risk |
| :--- | :--- | :--- | :--- | :--- |
| **MUST HAVE (Wave 0 — Pre-Build Gate)** | **Customer Validation Execution (Workstream D Interviews + Survey)** | Falsify/confirm `H1`–`H4` before writing R2/R3 code | R1 approval | Eliminates risk of building unvalidated features |
| **MUST HAVE (Wave 1 — Operational Completeness)** | **Admin Service Catalog CRUD + Vite/Vitest Frontend Migration + Legacy Cleanup** | Removes read-only catalog blocker & eliminates `100` CRA `react-scripts` dev advisories | R2 approval | Verified repo gaps (`Workstream A`) |
| **MUST HAVE (Wave 1 — Adoption Wedge)** | **Knowledge Management & Contextual Self-Service Deflection (R3)** | Day-1 ticket deflection + role-scoped KB using MySQL `FULLTEXT` | Wave 1 | Table-stakes adoption driver |
| **MUST HAVE (Wave 1 — Enterprise Gate)** | **OIDC SSO + Outbound Signed Webhooks + Hardened Docker Self-Host Packaging** | Unblocks 150–750 employee adoption & clean self-hosted pilots | Wave 1 | Mandatory enterprise deployment gate |
| **MUST HAVE (Wave 2 — Primary Differentiator)** | **Configurable Cross-Department Workflow Engine + Visual Designer + Dry-Run Simulator (R4)** | Core competitive differentiator replacing hardcoded 2-stage approvals | Wave 1 + R2 Workflow LLD | Highest complexity; requires strict security & durability tests |
| **MUST HAVE (Wave 2 — Flagship Hero Workflow)** | **Multi-Department Employee Lifecycle Orchestration: Onboarding / Transfer / Offboarding (R5)** | Highest-severity cross-department ROI proof built on R4 workflow engine | R4 Workflow Engine | Flagship demo & pilot value driver |
| **SHOULD HAVE (Wave 3 — Moat Amplification)** | **Trust, Privacy & Cryptographic Governance Console (R8)** | Surfaces existing SHA-256 audit chain + sensitive-data access history + exportable audit proof packs | Wave 1–2 | Amplifies EPAMS's strongest unique moat |
| **SHOULD HAVE (Wave 3 — Deterministic Value)** | **Deterministic Service Intelligence & Actionable Bottleneck Insights (R6 + R7)** | Skills/workload assignment recommendations, duplicate detection, SLA/approval bottleneck diagnosis (100% local, zero cloud LLM) | R3 KB + R4/R5 workflow telemetry | Delivers measurable efficiency without AI hype/privacy risk |
| **COULD HAVE (Wave 4 — Ecosystem)** | **Slack / Microsoft Teams Connectors, CSV Bulk Sync, SAML 2.0, Jira Data Center Importer** | Accelerates 2026–2029 Atlassian Data Center migrations & chat-native approvals | R9 Integration Framework | High upside if validated by Data Center migrant interviews |
| **DEFERRED** | **ML Predictive SLA Risk Models (R6 Level 7) & Multi-Tenancy** | Only justified after `>= 6–12` months of real multi-org dataset accumulation or validated SaaS demand | Real pilot datasets | Premature before customer validation |
| **REJECTED** | **Core HRIS/Payroll/Attendance/LMS, IT Asset Discovery/CMDB, Cloud-LLM Autonomous Chatbot, Microservices Split** | Saturated red oceans or architecture bloat conflicting with lightweight self-hosted positioning | N/A | Explicitly excluded to protect focus |

---

## 2. Phased Evolution Roadmap with Measurable Exit Criteria

| Phase | Scope | Prerequisites | Measurable Exit Criteria |
| :--- | :--- | :--- | :--- |
| **Phase 0: Customer Validation & R2 Design** | Execute `18`–`24` interviews + `50+` survey responses (`Workstream D`); freeze EPAMS 2.0 SRS, HLD, LLD, `V5+` migration strategy, and security design (`R2`) | User approval of R1 decisions | `H1`–`H4` formally scored with primary customer evidence; R2 SRS & architecture sign-off |
| **Phase 1: Self-Hosted Foundation & Self-Service (R3 + Core Gaps)** | Admin Catalog CRUD; Vite + Vitest toolchain migration (`0` CRA advisories) & React Router v7 upgrade; Knowledge Base (`FULLTEXT` search, versioning, visibility scoping, deflection before ticket creation); OIDC SSO + Production Docker Compose/TLS package | Phase 0 sign-off | Admin creates/edits catalog items via UI; KB deflection active with zero internal-article leaks; OIDC login verified; single-command Docker + MySQL 8 TLS deployment passes full regression |
| **Phase 2: Configurable Workflow Engine (R4)** | Purpose-built declarative DAG engine (`WorkflowDefinition/Version/Execution/Task`), visual designer, backend cycle/schema validator, dry-run simulator, version-pinned durable execution, allowlisted actions only | Phase 1 complete | Admin configures, simulates, and publishes a conditional multi-stage laptop approval workflow with `0` Java code changes; crash-restart & concurrency tests pass on MySQL 8 |
| **Phase 3: Employee Lifecycle Orchestration (R5)** | Onboarding, Transfer, and Offboarding multi-department journeys (IT, HR, Facilities, Finance) with task dependencies, blocker visibility, and mandatory approval gates before access revocation | Phase 2 (R4 Engine) | End-to-end onboarding & offboarding journeys verified across `4` departments with tamper-evident SHA-256 audit trail |
| **Phase 4: Governance Console & Deterministic Intelligence (R6, R7, R8)** | Sensitive-data access log, exportable cryptographic audit proof packs, retention policies; deterministic skills/workload assignment recommendations, duplicate detection, and actionable approval/SLA bottleneck diagnostics | Phase 3 complete | Every recommendation includes human-readable explanation & respects RBAC; auditor exports & verifies signed integrity pack |
| **Phase 5: Pilot Integrations, Enterprise Validation & Production Release (R9, R10, R11)** | Outbound signed webhooks, Slack/Teams alerts, CSV HRIS sync (and optional JSM Data Center importer if validated); `3`–`5` organization pilot measuring real baseline vs. post-EPAMS metrics; full security/SAST/DAST/manual a11y/soak certification | Phase 4 complete | Real pilot baseline metrics collected; external SAST/DAST & manual screen-reader checks complete; reproducible production release candidate signed off |
