# Real-World Problem Statement: Employee Lifecycle and Service Management

**Document ID:** EPAMS-PRB-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved  
**Author:** Senior Product Manager & Business Analyst  

---

## 1. Market Context & Target Organization Profile

### 1.1 Organizational Profile
* **Target Segment:** Indian Small and Medium-sized Enterprises (SMEs) and mid-market organizations.
* **Size Hypothesis:** 100 to 1,000 active employees.  
  *(Note: This size range is an initial working hypothesis based on typical organizational transitions from informal communication to formalized operational hierarchy; it is not yet a validated statistical market fact).*
* **Industry Profile:** Technology service firms, business process outsourcers (BPO/KPO), specialized manufacturing units, design agencies, and financial service consultancies.
* **Operating Model:** Hybrid and distributed work arrangements across tier-1, tier-2, and tier-3 Indian cities.

---

## 2. Stakeholder Ecosystem & User Personas

| Role / Persona | Core Responsibilities | Operational Reality |
| :--- | :--- | :--- |
| **Employee (Individual Contributor)** | Daily task delivery, keeping personal profile up to date, requesting workplace resources, requesting HR letters, submitting reimbursement queries. | Frustrated by fragmented communication channels; spends excessive time chasing approvals via email, chat, or in person. |
| **Reporting Manager / Team Lead** | Team output, task allocation, approving operational requests (hardware, software licenses, project reassignments, training). | Context switching between disparate tools; receives approval requests via informal chat threads with zero audit trail. |
| **People Operations / HR Executive** | Employee onboarding, document verification, policy dissemination, address/statutory updates, addressing employee grievances. | Swamped with repetitive, low-complexity requests (e.g., bonafide certificates, address updates, PF transfer queries) via shared inboxes. |
| **Department Head / Function Lead** | Operational budget oversight, resource allocation, team capacity planning, final approvals for high-value requests. | Lacks centralized visibility into department request volumes, bottlenecks, and average turnaround times. |
| **IT & Facilities Administrator** | Hardware provisioning, software credential issuance, workstation allocation, asset retrieval during offboarding. | Receives asset requests without formal approval verification; struggles to track asset return checklists upon employee departure. |
| **System & Compliance Administrator** | Identity and access governance, role definitions, audit trail integrity, regulatory adherence. | Anxious about data privacy (e.g., India's Digital Personal Data Protection Act) and unauthorized access to sensitive employee records. |

---

## 3. Workflow Inefficiencies & Root-Cause Analysis

### 3.1 Inefficiency 1: "The Shared Inbox and Chat Trap"
* **Observed Reality:** In organizations with 100–500 employees, service requests (IT assets, bonafide letters, salary certificate requests, payroll clarifications) are commonly handled via email distribution lists (`hr@company.com`, `it-support@company.com`) or messaging apps (Slack, Microsoft Teams, WhatsApp).
* **Consequences:**
  * Requests are lost, buried, or double-processed by different coordinators.
  * Zero visibility into Service Level Agreements (SLAs).
  * Employees have no way to check real-time status, prompting repetitive follow-up messages ("*Any update on my letter?*").
  * Lack of accountability regarding who approved or rejected an item.

### 3.2 Inefficiency 2: Fragmented Employee Lifecycle Handoffs
* **Observed Reality:** The Joiner-Mover-Leaver (JML) cycle involves cross-departmental coordination (HR, IT, Finance, Operations, Facilities).
* **Consequences:**
  * **Onboarding Friction:** New hires spend their first week without system credentials or laptops due to asynchronous email chains.
  * **Offboarding Security Risk:** Terminated employees retain access to internal accounts or confidential assets because the IT team was not formally or promptly notified by HR.

### 3.3 Inefficiency 3: Document Generation and Verification Overhead
* **Observed Reality:** Employees frequently require standard, stamped documentation (employment verification, bonafide letters for visas/loans, address proofs).
* **Consequences:**
  * HR executives manually draft, format, verify, and email documents individually.
  * Significant productivity drain on strategic HR initiatives.

### 3.4 Inefficiency 4: Compliance and Privacy Vulnerability
* **Observed Reality:** Employee PII (Personal Identifiable Information), salary slips, and medical records are transmitted over unencrypted email or stored on shared cloud drives without strict Role-Based Access Control (RBAC).
* **Consequences:**
  * Exposure to data leaks.
  * Inability to produce tamper-evident audit trails during statutory labor compliance or client security audits.

---

## 4. Why Existing Commercial Tools Fall Short for This Segment

| Category | Representative Examples | Limitations for Indian Mid-Market Organizations |
| :--- | :--- | :--- |
| **Enterprise Service Management Platforms** | ServiceNow HRSD | **Cost & Complexity Barrier:** Licensing models are priced in USD at enterprise tiers ($50–$100+ per user/month), with implementation cycles requiring specialized third-party system integrators taking 6–12 months. Over-engineered for a 250-person firm. |
| **Monolithic HRMS Suites** | Zoho People, Darwinbox, Keka | **Ticketing as an Afterthought:** These suites excel at attendance, leave, and payroll processing, but their internal service desk and customized multi-tier approval capabilities are often secondary, rigid, or difficult to configure for non-standard workflows. |
| **General-Purpose Helpdesks** | Jira Service Management, Zendesk | **Lack of HR Context:** Lacks native understanding of employee reporting hierarchies, department boundaries, compensation confidentiality, and employment lifecycle stages. |

---

## 5. Proposed MVP Problem Definition

### 5.1 Problem Statement
> **"Growing Indian organizations with 100–1,000 employees lack a unified, cost-effective, and secure internal service management platform. As a result, employee requests, approvals, and lifecycle transactions are dispersed across informal emails and chat threads, leading to lost requests, delayed onboarding/offboarding, zero SLA visibility, and severe compliance audit risks."**

### 5.2 The Specific Problem Our MVP Will Solve
The MVP will eliminate the operational overhead of informal request routing by providing:
1. **Centralized Service Catalog & Ticketing:** A dedicated self-service portal where employees can submit categorized requests (HR, IT, Facilities, Finance) with transparent status tracking.
2. **Context-Aware, Multi-Tier Approvals:** Deterministic routing of requests to reporting managers or department leads based on organizational hierarchy.
3. **Auditable Employee Lifecycle Governance:** Unified profile management paired with immutable audit logging for all account and service mutations.
4. **Actionable Operational SLA Metrics:** Real-time dashboards displaying request volumes, resolution times, and bottleneck points.

---

## 6. Categorization of Assertions

To maintain analytical rigor, all statements in this document are explicitly classified:

* **Evidence-Based (Directly Observed in Source Code):**
  * Existing EPAMS codebase supports user accounts, roles, basic profile data, and synchronous audit logging.
  * No service request, ticketing, or approval workflow constructs currently exist in the codebase.
* **Reasonable Working Assumptions (To Be Monitored):**
  * Mid-sized organizations prefer web-based portals over installing dedicated desktop clients.
  * Reporting managers require approval notifications with actionable context to prevent turnaround delays.
* **Product Hypotheses (To Be Validated Through User Testing):**
  * Consolidating internal requests into a structured catalog will reduce employee request resolution turnaround times by at least 40%.
  * Indian mid-market firms prioritize zero per-seat licensing over complex AI-driven automated chatbot resolutions during their initial digitisation phase.

