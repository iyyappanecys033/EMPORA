# EPAMS — P9 Final Security Assessment Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Consolidated Security Assurance  
**Date:** 2026-11-18  

> **Scope Disclaimer:** Internal application security review, source-code audit, and automated regression verification. Not a third-party penetration test or compliance certification. Online CVE vulnerability database scanning (`npm audit` / OWASP Dependency-Check) is `BLOCKED` in this offline sandbox (`BLK-03`) and is a mandatory P10 CI gate.

---

## 1. Security Remediations Verified Across P9 & Final Consolidation

| ID | Severity | Area | Remediation & Evidence | Status |
| :---: | :---: | :--- | :--- | :---: |
| **SEC-P9-01** | **HIGH** | Audit Chain Reset Guard | `AuditChainService.resetChainToGenesis()` blocked unless `epams.audit.allow-chain-reset=true` (`false` in `application-mysql.properties`). Verified by `P9SecurityAndWorkflowQaTests.test1`. | `FIXED` |
| **SEC-P9-02** | **HIGH** | Internal Comment Spoofing | `TicketCommandService.addComment()` rejects `isInternal=true` for non-specialist/non-admin callers (`403`). Verified by `P9SecurityAndWorkflowQaTests.test2`. | `FIXED` |
| **SEC-P9-03** | **CRITICAL** | Legacy IAM PII / RBAC Exposure | Hardened `AccountStatusLogController`, `UserAccountController`, `UserRoleMappingController`, `RoleController`, `ProfileController` (`@PreAuthorize` + PII/salary redaction on `/api/profiles/directory`). Verified by `P9SecurityAndWorkflowQaTests.test3`. | `FIXED` |
| **SEC-P9-04** | **HIGH** | Approval History BOLA | `GET /api/v1/approvals/tickets/{ticketId}` enforces `canViewTicket \|\| isAssignedApprover` (`403`/`404`). Verified by `P9SecurityAndWorkflowQaTests.test4`. | `FIXED` |
| **SEC-P9-05** | **MEDIUM** | Unbounded Pagination | Clamped pagination `size` to `1..100` in `ProfileController` & `RoleController`. Verified by `P9SecurityAndWorkflowQaTests.test3`. | `FIXED` |
| **SEC-P9-06** | **CRITICAL** | Spring Security 6 Stateless SPA CSRF | Configured `SpaCsrfTokenRequestHandler`, `CsrfCookieFilter`, `NullAuthenticatedSessionStrategy`, materialized `/api/auth/csrf`, and RFC 7807 `403` `AccessDeniedHandler`. Verified over live Tomcat HTTP (`P9LiveServerE2EIntegrationTests`). | `FIXED` |
| **GAP-P8-03** | **MEDIUM** | Production Cookie Defaults | Set `app.security.cookie.secure=${COOKIE_SECURE:true}` and `same-site=${COOKIE_SAME_SITE:Lax}` explicitly in `application-mysql.properties`. | `FIXED` |
| **SMTP-HDR** | **MEDIUM** | SMTP Header Injection Guard | `SmtpEmailDeliveryAdapter.sanitizeHeader()` strips `\r`, `\n`, `\0` before writing SMTP headers; minimal templates never include internal notes or PII. | `FIXED` |

---

## 2. Offline Dependency & Static Security Review (`2026-11-18`)

- **Backend BOM (`spring-boot-starter-parent:3.4.12`):** Spring Security `6.4.13`, Hibernate `6.6.36.Final`, Flyway `10.20.1`, `jjwt 0.11.5`.
- **Frontend (`package.json`):** React `18.2.0`, React Router `6.11.1`, Axios `1.4.0`, `react-scripts 5.0.1` (CRA 5 dev dependency chain carries known transitive advisories on webpack-dev-server/nth-check in older builds; must be audited via `npm audit` in P10 online CI).
- **Online CVE Scan Status:** **`BLOCKED` (`BLK-03`)** — offline sandbox cannot reach NVD or npm registry advisory endpoints. Offline manifest review does **NOT** substitute for online CVE scanning.
