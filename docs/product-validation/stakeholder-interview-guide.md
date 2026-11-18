# Stakeholder Interview Guide: Employee Lifecycle & Service Operations

**Document ID:** EPAMS-INT-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved for Field Execution  
**Author:** Senior Product Manager & Lead Business Analyst  

---

## 1. Interview Protocol & Guidelines for Interviewers

### 1.1 Objective
To extract unvarnished, empirical evidence regarding current employee request workflows, inter-departmental handoffs, approval bottlenecks, software tooling costs, and statutory audit pressures in organizations with approximately 100 to 1,000 employees.

### 1.2 Interviewer Rules
1. **Never pitch or defend EPAMS:** Do not say "*Our platform will solve this by...*". Ask open-ended questions: "*How do you handle this today?*"
2. **Dig into the 'Last Time':** Avoid hypothetical answers. Prompt for specifics: "*Tell me about the last time an employee requested a new laptop. Walk me through every step from request to receipt.*"
3. **Capture Workarounds:** Pay extreme attention to informal tools (WhatsApp groups, spreadsheets, desk visits, private Slack messages). Workarounds reveal unmet needs.
4. **Quantify Time and Cost:** Whenever an interviewee mentions a delay, quantify it: "*How many hours did that take? How many days elapsed?*"

---

## 2. Persona-Specific Interview Modules

### Module A: HR & People Operations Leaders

* **Current Process & Volume:**
  1. How many routine requests (bonafide letters, address changes, PF transfer queries, employment verification) does your team receive weekly?
  2. What is the step-by-step workflow when an employee requests a bonafide certificate for a passport or bank loan?
* **Tools & Costs:**
  3. What software currently handles your HR requests? (e.g., Zoho People, Darwinbox, Jira, or shared inboxes)?
  4. What are the key limitations or licensing pain points with your current tools?
* **Lifecycle & Handoffs (JML):**
  5. When a new hire joins, how do you coordinate laptop provisioning with IT and desk/badge allocation with Facilities?
  6. During employee offboarding, how do you ensure that all physical assets are returned and digital access is revoked across all departments? Have you ever experienced an ex-employee retaining access after departure?
* **Compliance & Audits:**
  7. How do you maintain an audit trail of employee profile and compensation changes for statutory labor inspections or external client audits?
  8. How do you protect sensitive employee compensation details from accidental exposure to colleagues or unauthorized managers?

---

### Module B: IT Support & System Administrators

* **Ticketing & Queue Management:**
  1. How do employees currently log technical issues (hardware breakdowns, software licenses, VPN access)?
  2. How do you prioritize and assign incoming requests among your IT support staff?
  3. Do you currently track Service Level Agreements (SLAs) or resolution turnaround times? If yes, how?
* **Approval Bottlenecks:**
  4. Before fulfilling an expensive software license (e.g., JetBrains, Figma) or high-spec laptop, how do you verify that the employee's manager approved the expense?
  5. What happens when a manager delays or ignores an approval request? How many days does a ticket sit idle waiting for manager sign-off?
* **Access Governance & Security:**
  6. How are privileged access requests (database credentials, production server access) approved and audited?
  7. What is your biggest daily operational headache when dealing with employee hardware/software requests?

---

### Module C: Reporting Managers & Team Leads

* **Approval Workload & Friction:**
  1. On average, how many operational requests (hardware, training, leave, software access) do you approve in a typical week?
  2. Through what channels do you receive these requests today (Email, Slack, WhatsApp, in-person)?
  3. How quickly do you typically review and act on an approval request? What causes delays on your end?
* **Context & Decision Clarity:**
  4. When asked to approve a resource request, do you have enough context (e.g., current equipment, project budget, urgency) to make an informed decision without messaging the employee for clarification?
  5. How do you feel about logging into a dedicated web portal to approve requests vs. receiving interactive approval buttons inside Slack/Teams or email?
* **Governance & Escalation:**
  6. What is your organization's policy for high-value requests? At what financial amount or asset level does your Department Head need to co-sign?
  7. What happens if you are out of office or unavailable for approvals? Is there a formal delegation mechanism?

---

### Module D: General Employees (Individual Contributors)

* **Experience & Transparency:**
  1. Think about the last time you requested something from IT, HR, or Facilities. What did you request, and how did you submit it?
  2. Did you know who was working on your request and when it would be completed? How did you check the status?
  3. Did you have to follow up or send reminder messages? How many times did you follow up before resolution?
* **Portal Usability:**
  4. If a single web portal allowed you to request IT hardware, HR letters, and Facilities services in 3 clicks with a live status timeline, would you use it instead of emailing coordinators directly?
  5. What is the most frustrating part of dealing with internal company support today?

---

## 3. Post-Interview Debrief & Synthesis Template

Immediately following each interview, the interviewer must complete the following structured scorecard:

```markdown
### Interview Debrief
- **Organization Code:** [ORG-xxx] (e.g. ORG-001)
- **Headcount:** [e.g. 240 employees]
- **Industry:** [e.g. B2B SaaS]
- **Stakeholder Role:** [e.g. HR Manager]
- **Date & Duration:** [e.g. Nov 14, 2026 - 45 min]

#### Key Findings Summary
1. Primary Request Mechanism: [e.g. Shared Google Form + Slack channel]
2. Observed Average MTTR: [e.g. 4.5 business days for bonafide letter]
3. Core Frustration Quote: "[Insert exact quote from interviewee]"
4. Tooling Disconnect: [e.g. Uses Zoho People for attendance, but IT helpdesk is completely unmanaged]
5. Hypothesis Validation Impact:
   - HYP-001 (Request Loss / Delays): [Confirmed / Rejected / Partially Confirmed]
   - HYP-002 (Manager Portal Adoption): [Confirmed / Rejected / Partially Confirmed]
   - HYP-003 (Unified Service Catalog): [Confirmed / Rejected / Partially Confirmed]
```

