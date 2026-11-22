# EPAMS 2.0 — Workstream H: Business Value, Metrics & Commercial Models

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream H)  
**Date:** 2026-11-18  
**Honesty Rule:** Zero fabricated revenue forecasts, ARR projections, or customer ROI claims. All target improvements are explicitly labeled **`HYPOTHETICAL TARGET`** requiring real pilot baseline measurement.

---

## 1. Product & Operational Value Metrics Framework

| Metric | Definition & Formula | Primary Data Source | Frequency | Baseline Requirement | Limitations & Guardrails |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Average Request Handling Time (MTTR)** | `SUM(business_minutes(created_at, resolved_at) - total_paused_minutes) / COUNT(resolved)` | `service_ticket` + `BusinessCalendar` | Weekly / Monthly | Minimum 30 days pre-change baseline | Sensitive to ticket complexity mix; segment by department & priority |
| **Approval Turnaround Time** | `AVG(hours(created_at, decided_at))` across decided `approval_task` rows | `approval_task` | Weekly | 30-day baseline per approval stage | Outliers (vacationing managers) skew mean; track **p50 and p90** alongside mean |
| **SLA Compliance Rate (%)** | `(Resolved within SLA / Total Resolved) * 100` | `service_ticket` (`AnalyticsService`) | Weekly / Monthly | Current period baseline | Can be gamed by abusing `ON_HOLD` — monitor `total_paused_minutes` ratio alongside SLA |
| **Self-Service Resolution / Deflection Rate (%)** | `KB sessions with "Helpful" & 0 ticket created within 24h / (KB sessions + Tickets created) * 100` | New `kb_feedback` & `kb_search_log` + `service_ticket` | Monthly | Track from Day 1 of KB launch | Deflection is an inferred proxy; verify employee didn't switch to Slack/email |
| **Duplicate Request Rate (%)** | `Tickets closed as DUPLICATE / Total Tickets Submitted * 100` | `service_ticket` (duplicate link/reason) | Monthly | Requires explicit duplicate-resolution tagging | Under-reported unless specialists tag duplicates consistently |
| **Open Backlog & Aged Backlog (> 7 days)** | `COUNT(open tickets)` and `COUNT(open tickets WHERE age > 7 business days)` | `service_ticket` | Daily / Weekly | Current queue snapshot | Must separate `ON_HOLD` vs. actively stalled tickets |
| **Reopen Rate (%)** | `Tickets transitioned RESOLVED -> IN_PROGRESS / Total RESOLVED * 100` | `audit_log` / `service_ticket` | Monthly | 30-day baseline | Measures true resolution quality (prevents premature closing to game MTTR) |
| **Manual Assignment / Reassignment Effort** | `AVG(reassignment_count per ticket)` & `time(submitted -> first_assigned)` | `audit_log` (`TICKET_ASSIGNED`) | Weekly | 30-day baseline | Proxy for triage friction; verify specialist satisfaction qualitatively |
| **Onboarding Completion Time & Day-1 Readiness (%)** | `% of onboarding journeys with 100% pre-Day-1 tasks completed before start_date` | New `lifecycle_journey` + `workflow_task` | Per cohort / Monthly | Manual pre-EPAMS HR checklist baseline | High executive visibility metric; directly ties to Day-1 productivity |
| **Offboarding Revocation SLA (%)** | `% of offboarding access-revocation tasks completed within 4h of exit timestamp` | New `lifecycle_journey` + `audit_log` | Per exit / Monthly | Audit log / IT ticket baseline | Direct security & compliance proof metric |
| **Workflow Execution Reliability (%)** | `Completed workflow_executions / (Completed + Failed) * 100` | New `workflow_execution` | Daily / Weekly | `>= 99.5%` technical SLO | Distinguishes config errors from engine/DB failures |
| **Employee Satisfaction (CSAT)** | Optional 1–5 rating post-ticket resolution (`AVG` & response rate) | Optional post-resolution feedback | Monthly | Requires `>= 20%` response rate | Survey fatigue risk — keep to 1-click optional prompt |

### Explicit Classification of Proposed Outcome Targets (`HYPOTHETICAL TARGETS` — NOT ACHIEVED)
- *25% reduction in average request handling time* — **`HYPOTHETICAL TARGET`**
- *40% reduction in manual assignment effort* — **`HYPOTHETICAL TARGET`**
- *30% reduction in duplicate requests* — **`HYPOTHETICAL TARGET`**
- *30% reduction in approval turnaround* — **`HYPOTHETICAL TARGET`**
- *15 percentage-point improvement in SLA compliance* — **`HYPOTHETICAL TARGET`**
- *20% employee self-service resolution rate* — **`HYPOTHETICAL TARGET`**

*None of these targets may be claimed as product outcomes until measured before-and-after in a real pilot (`Stage 11`).*

---

## 2. Commercial & Packaging Model Analysis (`RESEARCH FINDING` / `HYPOTHESIS`)

*(Zero fabricated revenue forecasts or unvalidated price points; objective tradeoff comparison only.)*

| Commercial Model | How It Works | Strengths for Target Segment (`100`–`1,000` Employees) | Risks & Tradeoffs |
| :--- | :--- | :--- | :--- |
| **Open-Core (Community Self-Hosted + Paid Enterprise Edition)** *(Recommended Candidate)* | Core portal, Catalog CRUD, 11-state ticketing, basic 2-stage approvals, KB, and basic audit free/source-available; Paid Enterprise unlocks **OIDC/SAML SSO, Visual Workflow Designer + Simulation, Multi-Dept Lifecycle Orchestration, Cryptographic Audit Export/Governance Console, and Support SLA** | Maximizes developer/SysAdmin grassroots adoption (especially JSM Data Center & `r/selfhosted` buyers); zero sales friction for initial PoC; aligns monetization strictly with genuine enterprise governance value | Requires careful open-vs-paid boundary so free edition stays genuinely useful without cannibalizing paid governance tiers; avoid "SSO tax" backlash if basic OIDC is expected |
| **Per-Agent / Per-Fulfiller Self-Hosted Subscription** *(Unlimited Free Employees)* | Annual license based on resolver/agent seats (`10`–`50` agents in a `300`-person org); all employees submit requests free (mirrors Freshservice/JSM agent model, minus SaaS tier/transaction caps) | Predictable CFO budgeting; encourages 100% employee portal adoption (no penalty for adding casual employees) | Must avoid ManageEngine's hated "per-ESM-department multiplier" — include HR + IT + Facilities + Finance workspaces in one license |
| **Flat Tiered Per-Organization Licensing** *(e.g., Up to 250 / 500 / 1,000 Employees)* | Simple annual tier by headcount band, unlimited agents, unlimited workflows, zero transaction metering | Ultra-simple procurement; directly counter-positions against Freshservice transaction caps & ServiceNow complexity | Leaves value on table in high-agent IT service orgs if bands are too wide |
| **Implementation & Support Services (Add-On)** | Optional paid Jira Data Center migration tooling/assistance, custom HRIS webhook integration, and priority SLA support | Directly monetizes the 2026–2029 JSM Data Center migration wave | Services do not scale linearly like software licenses; keep optional |

**Commercial Validation Prerequisite (`REQUIRES VALIDATION`):** Test Open-Core + Predictable Per-Agent vs. Flat Per-Org tiers during Workstream D interviews with IT Directors and COOs before finalizing commercial packaging.
