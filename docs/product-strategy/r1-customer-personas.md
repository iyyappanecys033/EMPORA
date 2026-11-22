# EPAMS 2.0 — Workstream D: Customer & User Research Personas

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** R1 — Product Discovery & Market Validation (Workstream D)  
**Evidence Status:** **`HYPOTHESIS / RESEARCH PERSONAS — PRIMARY CUSTOMER INTERVIEWS NOT YET CONDUCTED`**  
*(Zero fabricated quotes or invented interview transcripts. Personas are synthesized from market/competitor research and domain analysis to guide real discovery interviews.)*

---

## Target Organization Profile (`HYPOTHESIS`)
- **Size:** `100`–`1,000` employees (primary focus: `150`–`750` employees)
- **Verticals:** Fintech/BFSI, healthcare/healthtech, legal/accounting firms, engineering/software outsourcers, defense/manufacturing suppliers, EU GDPR / India DPDP-sensitive organizations
- **Current Stack Pattern:** Core HRIS/Payroll already exists (e.g., Zoho/Keka/BambooHR/legacy payroll) + Microsoft 365 or Google Workspace, **but internal cross-department employee requests (IT + HR + Facilities + Finance) still run on fragmented email, Slack/Teams DMs, spreadsheets, DIY SharePoint lists, or an aging/sunsetting Jira Data Center instance.**

---

## Persona 1 — Employee (Primary End User)
- **Role:** Software Engineer, Analyst, Sales Rep, or Operations Staff
- **Goals:** Get hardware, software access, HR letters, payroll clarifications, and workplace support resolved quickly without guessing which email alias or Slack channel to message.
- **Hypothesized Pain Points:**
  - Cannot tell whether a request belongs to IT, HR, Finance, or Facilities.
  - Files tickets for simple policy questions because answers are buried in outdated PDFs/wikis.
  - Zero visibility after sending an email ("black hole" approvals — has to ping manager and IT repeatedly).
- **Current Workarounds:** Direct-messaging IT/HR staff on Slack/Teams, emailing shared inboxes, asking peers.
- **Success Signal:** Finds answer via self-service in `< 60 seconds`, or submits a guided request with transparent status and expected SLA.

## Persona 2 — HR Operations Specialist / HR Manager (Primary Cross-Dept Champion)
- **Role:** HR Ops Lead / People Operations Manager (team of `2`–`8` HR staff)
- **Goals:** Deliver smooth onboarding/offboarding, respond to confidential employee inquiries safely, and eliminate repetitive HR FAQ volume without buying an enterprise ServiceNow license.
- **Hypothesized Pain Points:**
  - **Onboarding & offboarding coordination chaos:** Creating manual checklists across IT (laptop, email, VPN), Facilities (access badge, desk), Finance (payroll/bank setup), and Manager — one missed IT handoff leaves a new hire idle on Day 1 or an ex-employee with active VPN access after exit!
  - Answering the same policy/leave/reimbursement questions 20×/week.
  - Strict confidentiality required for salary, medical, and employee-relations tickets (cannot share raw HR ticket data with IT admins).
- **Current Workarounds:** Excel/Google Sheets onboarding trackers, email chains, manual calendar reminders.
- **Success Signal:** One-click onboarding/offboarding orchestration with automated cross-department task dependencies and strict HR data privacy.

## Persona 3 — IT Support Lead / Service Desk Specialist (Primary Fulfiller)
- **Role:** IT Support Engineer / Helpdesk Lead (team of `2`–`10` IT staff)
- **Goals:** Meet SLAs, avoid burnout from repetitive/unstructured tickets, prevent duplicate requests, and balance ticket load fairly across specialists.
- **Hypothesized Pain Points:**
  - Receiving incomplete requests missing manager approval or cost-center info.
  - Duplicate tickets filed by frustrated employees during outages or slow approvals.
  - Unbalanced queues (one senior specialist buried with 25 tickets while others have 4) and manual triage overhead.
- **Current Workarounds:** Manual Slack triage, ad-hoc spreadsheet queues, Jira/GLPI workarounds.
- **Success Signal:** Complete, pre-approved tickets routed automatically to the right specialist with visible workload balance and knowledge deflection cutting repetitive password/VPN tickets.

## Persona 4 — Department Manager (Primary Approver)
- **Role:** Engineering Manager, Finance Lead, Operations Director (`8`–`30` direct reports)
- **Goals:** Approve valid team requests quickly without becoming an operational bottleneck, while having visibility into team delays and approvals.
- **Hypothesized Pain Points:**
  - Approval requests buried in 100+ daily emails; forgets pending items until employees escalate.
  - Insufficient context in approval emails (missing estimated cost, SLA impact, or policy threshold).
  - Zero visibility into which department is holding up a new hire's setup.
- **Current Workarounds:** Replying "Approved" in email threads (zero structured audit trail).
- **Success Signal:** Single consolidated approval inbox with clear context, automated reminders/escalations, and auditable approve/reject decisions.

## Persona 5 — System / IT Administrator (Technical Buyer & Operator)
- **Role:** Head of IT / Systems Administrator / InfoSec Lead
- **Goals:** Run a secure, auditable, low-maintenance internal platform on company-controlled infrastructure without surprise SaaS renewals or complex multi-node ops.
- **Hypothesized Pain Points:**
  - Forced cloud migration from Jira Data Center EOL (March 2029) conflicts with internal data-sovereignty policies.
  - SaaS per-seat / per-ESM-instance / per-workflow-transaction price spikes.
  - Maintaining complex self-hosted stacks (Elasticsearch + Redis + Ruby + plugins) with a lean 3-person IT team.
  - Passing internal/external security & compliance audits (proving who approved access, who viewed salary PII, and proving audit logs were never altered).
- **Current Workarounds:** Patching legacy Jira Data Center/ManageEngine, running manual DB audit scripts, restricting SaaS adoption.
- **Success Signal:** Single-container/JAR deployment backed by standard MySQL 8, OIDC/SAML SSO, no-code workflow updates without code deployments, and one-click cryptographic audit verification.

## Persona 6 — Operations Leader / COO / VP Operations (Economic Buyer)
- **Role:** COO, VP of Operations, or CFO (`150`–`750` employee firm)
- **Goals:** Reduce invisible productivity loss from slow internal operations, keep per-employee software TCO predictable, and maintain audit-ready operational governance.
- **Hypothesized Pain Points:**
  - No single view of where internal processes stall (approval delays vs. IT understaffing vs. missing documentation).
  - Paying for 3–4 overlapping tools (HRIS + IT helpdesk + wiki + Power Automate add-ons) while employees still complain about slow onboarding.
- **Current Workarounds:** Quarterly manual Excel rollups from HR and IT leads.
- **Success Signal:** Actionable operational visibility (bottlenecks, SLA compliance, onboarding cycle time) at a fraction of ServiceNow/Freshservice Enterprise TCO.
