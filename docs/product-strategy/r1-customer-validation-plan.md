# EPAMS 2.0 — Workstream D: Customer Validation Plan

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream D)  
**Current Validation Status:** **`CUSTOMER VALIDATION: NOT YET CONDUCTED`**  
**Goal:** Falsify or confirm R1 hypotheses before committing engineering months to R2/R3–R9 implementation.

---

## 1. Hypotheses to Validate or Falsify (`HYPOTHESIS`)

| Hypothesis ID | Hypothesis Statement | Falsification Criterion (Pivot / Reject Trigger) | Validation Criterion (Proceed Trigger) |
| :--- | :--- | :--- | :--- |
| **`H1` (Segment & Self-Hosting)** | Privacy-sensitive `100`–`1,000`-employee organizations (incl. JSM Data Center migrants & regulated SMBs) actively seek a lightweight, single-binary self-hosted ESM platform | `< 20%` of target respondents care about self-hosting or data residency; majority strongly prefer cloud SaaS | `>= 40%` of target regulated/Data-Center-affected respondents list self-hosting or data sovereignty as a top-3 decision criterion |
| **`H2` (Primary Pain)** | Cross-department onboarding/offboarding handoffs + unconfigurable approval bottlenecks + repetitive employee FAQs represent the top 3 recurring operational pains | Respondents report current tools already solve onboarding & approvals satisfactorily with minimal friction | `>= 60%` of HR/IT leads cite concrete incidents within the last 30 days involving onboarding misses, stalled approvals, or repetitive FAQs |
| **`H3` (Primary Differentiator)** | No-code configurable cross-department workflows + tamper-evident governance inside a single self-hosted JAR/MySQL stack beats bloated multi-service stacks (Zammad/ServiceNow/ManageEngine) | Technical buyers insist ITAM/CMDB (GLPI) or full Payroll/HRIS (Zoho People) is mandatory before adopting an ESM portal | `>= 50%` of technical buyers confirm they already own HRIS/Payroll and prefer a clean, decoupled self-hosted service & workflow layer with OIDC/SAML SSO |
| **`H4` (Commercial Viability)** | Organizations with `150`–`750` employees will pay predictable per-organization or per-agent self-hosted licensing rather than high SaaS Enterprise tier markups | Zero willingness to pay (`\$0` expectation only) or mandatory SOC 2 Type II certification required even for self-hosted open-core pilots | `>= 5` qualified organizations commit to an unpaid or paid design-partner pilot once SSO + Catalog CRUD + Knowledge Base / Configurable Workflows exist |

---

## 2. Recruitment Channels & Sample-Size Design

| Target Group | Target Sample Size | Recruitment Channels |
| :--- | :---: | :--- |
| **Qualitative Discovery Interviews** (`45 min` each) | **`18`–`24` total** (`4` HR Ops, `5` IT Support/SysAdmin, `4` Dept Managers, `3` InfoSec/Auditors, `3` Ops Leaders/COOs, `3` Employees) across `>= 10` distinct organizations (`100`–`1,000` employees) | LinkedIn outreach to Heads of IT / HR Ops in regulated SMBs; Atlassian Data Center migration communities (`r/jira`, `r/sysadmin`, `r/selfhosted`, Spiceworks); local India/EU SaaS & HR Ops peer networks; warm founder/alumni introductions |
| **Quantitative Validation Survey** (`r1-customer-survey.md`) | **`50`–`100` completed responses** (minimum `35` in `100`–`1,000` headcount band) | IT/SysAdmin forums, HR Ops Slack/Discord communities, self-hosted software communities, LinkedIn polls |
| **Design Partner Discovery Calls** | **`3`–`5` prospective pilot organizations** | Recruited directly from high-pain interview participants who express unprompted interest in testing a live self-hosted prototype |

---

## 3. Execution Timeline & Evidence Collection Protocol (3–4 Weeks Pre-R2)
- **Week 1:** Finalize survey form, recruit initial `10` interview participants, launch quantitative survey.
- **Week 2–3:** Conduct `18`–`24` structured behavioral interviews (`r1-customer-interview-guide.md`); log anonymized notes, concrete past-failure counts, tool spend, and verbatim pain quotes in a structured evidence repository.
- **Week 4:** Synthesize evidence against `H1`–`H4`; update `R1-DECISION-REGISTER.md` statuses from `REQUIRES VALIDATION` → `APPROVED` or `REJECTED` before freezing R2 SRS & architecture.
- **Data Ethics:** Zero PII stored in public Git repositories; anonymize participant/company identifiers (`ORG-01`, `INT-01`).
