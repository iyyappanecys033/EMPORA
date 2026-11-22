# EPAMS 2.0 — Workstream E: Problem Discovery & Prioritisation

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream E)  
**Date:** 2026-11-18  
**Evidence Transparency Note:** All scores below are grounded in **verified competitor/market research + actual EPAMS repository gap analysis (`RESEARCH FINDING` / `HYPOTHESIS`)**, NOT primary customer interview counts (validation pending). Scores are an explicit, inspectable decision tool — never disguised as validated demand.

---

## 1. Transparent Scoring Model (1–5 Scale per Factor, Max Weighted Score = 100)

| Factor | Weight | Scoring Anchor (1 = Low / Unfavorable → 5 = High / Favorable) |
| :--- | :---: | :--- |
| **Frequency** | `15%` | `1` = rare/annual → `5` = daily across org |
| **Severity / Impact** | `15%` | `1` = minor annoyance → `5` = security breach, Day-1 failure, or audit finding |
| **Economic Impact** | `15%` | `1` = negligible cost → `5` = major wasted staff hours or SaaS license penalty |
| **Affected User Breadth** | `10%` | `1` = admins only → `5` = entire employee population |
| **Existing Solution Dissatisfaction** | `15%` | `1` = cheap/easy alternatives exist → `5` = paywalled, sunsetting (JSM DC), or brittle DIY |
| **Willingness to Change (`HYPOTHESIS`)** | `10%` | `1` = inertial/low priority → `5` = active forcing function (audit, EOL, scaling) |
| **Technical Feasibility in EPAMS** | `10%` | `1` = multi-year rewrite → `5` = clean modular extension on Java 25/MySQL 8/React |
| **Competitive Headroom (Inverse Intensity)** | `10%` | `1` = saturated red ocean → `5` = underserved self-hosted/mid-market opening |

---

## 2. Candidate Problem Inventory & Transparent Prioritisation Matrix

| Rank | Problem ID & Statement | Primary Persona(s) | Today's Workaround & Failure Mode | Freq (15%) | Sev (15%) | Econ (15%) | Breadth (10%) | Dissat (15%) | Change (10%) | Feas (10%) | Headroom (10%) | Weighted Score (/100) | Evidence Basis |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **1** | **`PROB-01`: Rigid, Unconfigurable Cross-Department Approval & Routing Workflows** *(Every policy/threshold change requires custom code, consultants, or metered SaaS upgrades)* | Admins, HR, IT, Managers | Email chains, brittle Power Automate flows (6k/day cap, owner breaks on exit), or Freshservice/Zoho Enterprise paywalls | `4` | `4` | `5` | `4` | `5` | `4` | `4` | `4` | **`86.0`** | `RESEARCH FINDING` + `VERIFIED FACT` (EPAMS hardcoded 2-stage) |
| **2** | **`PROB-02`: Fragmented Multi-Department Employee Onboarding & Offboarding Coordination** *(Missed laptops, late payroll, unrevoked ex-employee VPN/app access)* | HR Ops, IT, Facilities, Managers | Manual Excel checklists & email pings; high security & Day-1 productivity impact; gated in ServiceNow EJM / expensive ESM instances | `3` | `5` | `5` | `4` | `4` | `4` | `4` | `4` | **`83.0`** | `RESEARCH FINDING` + `HYPOTHESIS` |
| **3** | **`PROB-03`: Repetitive Employee Requests & Knowledge Fragmentation** *(Zero contextual deflection before ticket filing; answers buried in PDFs/wikis)* | Employees, HR, IT | Asking peers on Slack, filing duplicate tickets; requires separate paid Confluence or Enterprise portal | `5` | `3` | `4` | `5` | `4` | `4` | `5` | `3` | **`82.0`** | `RESEARCH FINDING` + `VERIFIED FACT` (EPAMS has 0 KB) |
| **4** | **`PROB-04`: Self-Hosted Data Sovereignty & Auditability Squeeze** *(JSM Data Center EOL Mar 2029; SaaS privacy friction; manual audit reconstruction)* | IT Admin, InfoSec, Auditors, COO | Sticking on sunsetting JSM DC, heavy Zammad 4-service stack, or dated GLPI/ManageEngine | `3` | `5` | `4` | `3` | `5` | `5` | `5` | `5` | **`82.0`** | `VERIFIED FACT` (JSM DC EOL) + EPAMS SHA-256 chain strength |
| **5** | **`PROB-05`: Read-Only / Rigid Internal Service Discovery & Catalog Ops** *(Non-technical admins cannot update service offerings, forms, or SLAs cleanly)* | Admins, Employees | SQL/ticket workarounds; EPAMS currently lacks Admin Catalog CRUD UI/API | `4` | `4` | `3` | `5` | `3` | `4` | `5` | `3` | **`76.0`** | `VERIFIED FACT` (EPAMS source audit) |
| **6** | **`PROB-06`: Manual Ticket Triage, Duplicate Filings & Unbalanced Resolver Workload** | IT Specialists, Dept Managers | Manual queue cherry-picking; senior staff overloaded while peers are idle | `4` | `3` | `4` | `3` | `3` | `3` | `4` | `3` | **`68.0`** | `RESEARCH FINDING` / `HYPOTHESIS` |
| **7** | **`PROB-07`: Descriptive-Only Reporting Without Actionable Bottleneck Diagnosis** | Ops Leaders, Managers | Exporting CSVs to Excel to figure out *why* SLAs or approvals stalled | `3` | `3` | `3` | `2` | `3` | `3` | `4` | `4` | **`62.0`** | `VERIFIED FACT` (EPAMS descriptive analytics) |
| **8** | **`PROB-08`: Lack of Enterprise Identity (SSO) & Tool Integration** *(Local passwords create onboarding friction & security risk)* | IT Admins, Employees | Manual account creation & separate passwords | `5` | `4` | `3` | `5` | `4` | `4` | `4` | `2` | **`78.0`** *(Table-Stakes Blocker)* | `VERIFIED FACT` (EPAMS local auth only) |
| **9** | **`PROB-09`: Full HRIS / Payroll / Attendance Consolidation** | HR Teams | Zoho People, BambooHR, Keka, Frappe HR already solve this cheaply | `5` | `4` | `3` | `5` | `2` | `2` | `1` | `1` | **`56.0`** *(REJECTED)* | `RESEARCH FINDING` (Red ocean) |
| **10** | **`PROB-10`: IT Asset Discovery / CMDB / Network Inventory** | IT Ops | GLPI (free OSS) & ManageEngine already dominate | `4` | `3` | `3` | `2` | `2` | `2` | `1` | `1` | **`46.0`** *(REJECTED)* | `RESEARCH FINDING` (Red ocean) |

---

## 3. Selected Core Problem Focus (`RECOMMENDATION` — Subject to Workstream D Validation)

- **Primary Problem to Solve:** **`PROB-01` + `PROB-04` Combined** — *Privacy-sensitive `100`–`1,000`-employee organizations lack a lightweight, self-hosted, cryptographically auditable platform where HR/IT admins can configure cross-department service approvals and routing without code, cloud lock-in, or enterprise SaaS paywalls.*
- **Two Supporting Problems:**
  1. **`PROB-02` (Employee Onboarding & Offboarding Orchestration):** Highest-severity cross-department workflow where a missed handoff causes Day-1 downtime or post-exit security exposure.
  2. **`PROB-03` (Knowledge-First Employee Self-Service & Deflection):** Immediate Day-1 adoption wedge that cuts repetitive HR/IT tickets before they are created (paired with fixing `PROB-05` Admin Catalog CRUD and `PROB-08` OIDC/SAML SSO as mandatory table-stakes blockers).
