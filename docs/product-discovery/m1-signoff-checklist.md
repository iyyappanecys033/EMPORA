# Phase M1 Sign-Off Checklist & Governance Gate

**Document ID:** EPAMS-CHK-001  
**Version:** 2.0.0  
**Date:** November 2026  
**Status:** Conditionally Approved for Phase 1 (HLD & LLD)  
**Author:** Principal Software Architect & Senior Product Manager  

---

## 1. Governance Gate Overview

Phase M1 (Product Discovery and Requirements Analysis) establishes the analytical, functional, and architectural foundation for the **Employee Lifecycle and Service Management Platform (EPAMS)**. This checklist validates that all discovery deliverables, code verifications, and architectural decisions satisfy enterprise quality standards before Phase 1 (High-Level and Low-Level Design) commences.

---

## 2. Comprehensive Validation Checklist

### 2.1 Codebase Audit & Ground Truth Verification
- [x] **Repository Structure Inspected:** Complete audit of root, `.github/`, `.vscode/`, `springapp/`, and `springbackend/` directories documented in `project-audit.md`.
- [x] **Runtime & Language Verified:** OpenJDK 25.0.2 LTS runtime verified; Maven `pom.xml` configures `<java.version>25</java.version>`; compiles cleanly with `release 25`.
- [x] **Authentication Architecture Verified:** Stateless JWT signed via HMAC-SHA256 (`HS256`); dual-cookie issuance (`accessToken` 15 min, `refreshToken` 7 days).
- [x] **Dual Extraction Verified:** `JwtFilter` verified to extract from `accessToken` cookie first, with graceful fallback to `Authorization: Bearer <token>` header.
- [x] **CSRF Defense Verified:** `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header validated. Public auth endpoints explicitly exempted.
- [x] **Brute-Force Lockout Verified:** 5 consecutive failed attempts $\rightarrow$ 15-minute lock verified in `UserAccount` and `AuthService`.
- [x] **RBAC Enforcement Verified:** Dynamic mapping of `ROLE_` prefix in `CustomUserDetailsService`; `@PreAuthorize` method security validated.
- [x] **Audit Logging Documented:** Synchronous database persistence in request thread confirmed and logged as technical debt in `project-audit.md`.

### 2.2 Problem Definition & Market Benchmarking
- [x] **Target Segment Grounded:** Indian mid-market organizations (100–1,000 employees) defined as an explicit **working hypothesis** awaiting field evidence.
- [x] **SIH 2025 PS 25195 Integrated:** Scope of "Smart Helpdesk Ticketing Solution for IT Services" (AI classification, intelligent routing, self-service IT) documented and compared honestly.
- [x] **Competitor Capabilities Verified:** Official capabilities of Zoho People (HR Cases, attendance/payroll) and ServiceNow HRSD (Flow Designer, enterprise ESM) verified without false absence claims.
- [x] **Product Positioning Refined:** EPAMS positioned as a lightweight, security-focused Employee Operations Workflow Platform uniting IT, HR, Facilities, and Finance.

### 2.3 Business Rules & Approval Workflows
- [x] **Single-Level Default Approved:** Routine requests route to direct Reporting Manager (`BR-001`, `ADR-001`).
- [x] **Conditional Two-Level Escalation Approved:** High-impact requests (hardware, privileged access, expenses above threshold) escalate to Department Head.
- [x] **Configurable Financial Threshold:** Threshold stored in configuration (`app.workflow.approval.financial-threshold`, default ₹10,000) and modifiable by Admin (`BR-008`).
- [x] **Privileged Access Escalation Enforced:** Mandatory Department Head and Security Admin sign-off for elevated system access (`BR-009`).
- [x] **Strict Anti-Self-Approval Enforced:** Self-approval blocked at service and database levels (`BR-005`, `BR-010`).

### 2.4 Ticket Lifecycle & State Machine
- [x] **11-State FSM Defined:** `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`, `REOPENED`.
- [x] **Cancellation Guards:** Employees can cancel in `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED` (pre-work), and `ON_HOLD`. Prohibited once `IN_PROGRESS`.
- [x] **SLA Pause/Resume Defined:** SLA clock explicitly pauses in `ON_HOLD` and resumes with extended target date in `IN_PROGRESS`.
- [x] **Reopening & Auto-Closure:** Reopening restricted to 48 business hours with mandatory reason; auto-closure executes after 48 unconfirmed hours.

### 2.5 Architecture & Concurrency Decisions
- [x] **Optimistic Concurrency Control:** JPA `@Version` on `ServiceTicket` with HTTP 409 Conflict handling (`ADR-008`).
- [x] **Approval Integrity Policy:** Strict prohibition of blind automated retries for approval decisions (`ADR-008`).
- [x] **Email Testing Infrastructure:** Mailpit container (port 1025/8025) for local Docker integration; `@MockBean JavaMailSender` for unit testing (`ADR-009`).
- [x] **Runtime Lifecycle Baseline:** Java 25 LTS local compilation; recommended Java 21 LTS enterprise certified baseline for commercial production deployments of Spring Boot 3.4.x (`ADR-010`, Proposed).
- [x] **Audit Integrity Architecture:** Transactional local write with SHA-256 hash chaining and database privilege restrictions (`ADR-011`).
- [x] **Database & Migration Strategy:** MySQL 8.0+ production RDBMS (`ADR-004`) with Flyway versioned migrations (`ADR-005`).
- [x] **Frontend Architecture:** Decoupled React dev server on port 3000 during development (`ADR-002`); multi-stage Docker build in production (`ADR-003`).
- [x] **Backend Pattern:** Modular Monolith within single Spring Boot deployable (`ADR-006`).

### 2.6 Product Validation Fieldwork Foundation
- [x] **Validation Plan Established:** `docs/product-validation/product-validation-plan.md` created with multi-stage methodology.
- [x] **Stakeholder Interview Guide Established:** `docs/product-validation/stakeholder-interview-guide.md` created covering 4 personas (HR, IT, Managers, Employees).
- [x] **Validation Matrix Established:** `docs/product-validation/problem-validation-matrix.md` created with 10 explicit hypotheses, validation methods, and threshold criteria.

---

## 3. Governance Sign-Off & Phase Transition

| Role | Sign-Off Status | Date | Remarks |
| :--- | :--- | :--- | :--- |
| **Principal Software Architect** | **APPROVED** | November 13, 2026 | Technical baseline, concurrency model, and modular architecture validated. |
| **Senior Product Manager** | **CONDITIONALLY APPROVED** | November 13, 2026 | Approved to proceed to Phase 1 (HLD/LLD) in parallel with field stakeholder interviews. |
| **Lead Quality Assurance Engineer** | **APPROVED** | November 13, 2026 | Testability criteria and RTM coverage verified at 100%. |

**Formal Recommendation:** Proceed to **Phase 1: High-Level Design (HLD) and Low-Level Design (LLD)** to define database schema scripts, entity definitions, and REST contracts without modifying existing application source code.
