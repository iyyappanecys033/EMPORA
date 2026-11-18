# EPAMS P9 — Comprehensive Security Assessment & Remediation Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  
**Assessment Scope:** Full backend source inspection (16 controllers, 8 security classes, 14 services, Flyway V1–V4), frontend auth/CSRF/route guard inspection, and automated security regression & live-server HTTP verification.  

> **Scope & Honesty Disclaimer:** This assessment represents rigorous internal application security engineering review and automated verification. It is **not** a formal third-party penetration test or compliance certification. External CVE database scanning (`OWASP Dependency-Check` / `npm audit`) is classified as `BLOCKED` in this offline sandbox and is a mandatory P10 CI gate.

---

## 1. Confirmed Security Defects Discovered & Remediated in P9

| Finding ID | Severity | Discovery Phase | Affected Files | Root Cause | Remediation Applied | Verification Status |
| :---: | :---: | :--- | :--- | :--- | :--- | :---: |
| **SEC-P9-01** | **HIGH** | Phase A Source Audit | `AuditChainService.java`, `application-mysql.properties`, `src/test/resources/application.properties` | `resetChainToGenesis()` was an unguarded public method on a production `@Service` bean capable of deleting all chained audit records. | Guarded with `@Value("${epams.audit.allow-chain-reset:false}")`; throws `UnsupportedOperationException` unless explicitly enabled. Forced `false` in `application-mysql.properties` and enabled `true` solely in test properties. | `VERIFIED` (`P9SecurityAndWorkflowQaTests.test1`) |
| **SEC-P9-02** | **HIGH** | Phase A Source Audit | `TicketCommandService.java` | `addComment()` checked `canViewTicket()` but allowed non-specialist requesters to submit `isInternal = true`, spoofing internal resolver notes. | Enforced `if (isInternal && !(isAdmin || isDepartmentResolver))` throwing `UnauthorizedScopeException` (HTTP `403`). | `VERIFIED` (`P9SecurityAndWorkflowQaTests.test2`) |
| **SEC-P9-03** | **CRITICAL** | Phase A Source Audit | `ProfileController.java`, `UserAccountController.java`, `UserRoleMappingController.java`, `AccountStatusLogController.java`, `RoleController.java` | Legacy Phase 1 IAM endpoints lacked `@PreAuthorize`: any authenticated employee could enumerate all users/profiles (leaking `salary`, `dob`, `address`, `emergencyContact`), read role mappings, and perform full CRUD on `/api/accountStatusLogs`. | Locked down `/api/accountStatusLogs` to `ADMIN`; restricted `GET /api/users` & `GET /api/profiles` to `ADMIN`, `HR_MANAGER`, `AUDITOR`; restricted `GET /api/roles` & `GET /api/userRoleMappings` to `ADMIN`, `AUDITOR`; redacted PII/salary fields on `GET /api/profiles/directory` for non-ADMIN/HR_MANAGER callers; preserved `/api/users/me` for all authenticated users. | `VERIFIED` (`P9SecurityAndWorkflowQaTests.test3`) |
| **SEC-P9-04** | **HIGH** | Phase A Source Audit | `ApprovalController.java`, `ApprovalService.java` | `GET /api/v1/approvals/tickets/{ticketId}` had zero ownership/scope enforcement (BOLA/IDOR), allowing any employee to read approval records and decision rationales for other users' tickets. | Added `actorUsername` propagation and enforced `ticketSecurity.canViewTicket(ticket, actorUsername) || isAssignedApprover` (404 for unknown ticket, 403 for unauthorized caller). | `VERIFIED` (`P9SecurityAndWorkflowQaTests.test4`) |
| **SEC-P9-05** | **MEDIUM** | Phase A Source Audit | `ProfileController.java`, `RoleController.java` | Unbounded `size` pagination parameters allowed arbitrary large-page extraction (DoS / memory exhaustion vector). | Clamped `page >= 0` and `size` to `[1, 100]` across both controllers. | `VERIFIED` (`P9SecurityAndWorkflowQaTests.test3`) |
| **SEC-P9-06** | **CRITICAL** | Phase C Live-Server HTTP E2E | `SecurityConfig.java`, `AuthController.java` | Four interacting Spring Security 6 stateless SPA CSRF defects hidden by MockMvc `.with(csrf())`: (1) `AuthController.getCsrfToken()` did not materialize deferred `CsrfToken`; (2) default `XorCsrfTokenRequestAttributeHandler` rejected raw `X-XSRF-TOKEN` header echoed from `CookieCsrfTokenRepository` by React/Axios SPA; (3) default `CsrfAuthenticationStrategy` inside `SessionManagementFilter` deleted the `XSRF-TOKEN` cookie (`Max-Age=0`) on every authenticated stateless JWT request; (4) missing `AccessDeniedHandler` forwarded CSRF rejections to `/error`, returning misleading `401 Unauthorized` instead of `403 Forbidden`. | Implemented Spring Security 6 official `SpaCsrfTokenRequestHandler` + `CsrfCookieFilter` + `NullAuthenticatedSessionStrategy` for stateless JWT CSRF preservation, materialized `CsrfToken` in `GET /api/auth/csrf`, and added RFC 7807 `403 Forbidden` `AccessDeniedHandler` in `SecurityConfig.java`. | `VERIFIED` (`P9LiveServerE2EIntegrationTests.test1`, `test2` & all 177 baseline tests) |

---

## 2. Role × Resource Authorization Matrix (Verified Post-Remediation)

Legend: **ALLOW** = Permitted by RBAC & scope; **SCOPED** = Permitted only for own/department/assigned records; **DENY (403)** = Blocked by `@PreAuthorize` or service evaluator; **401** = Unauthenticated.

| Resource / Endpoint | Anonymous | `EMPLOYEE` | `MANAGER` | `HR_MANAGER` | Specialist (`IT_SUPPORT` / `FACILITIES_CREW` / `HR_SPECIALIST`) | `AUDITOR` | `ADMIN` |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `POST /api/auth/login`, `/register`, `/refresh` | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| `GET /api/v1/health` | ALLOW (minimal) | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| `GET /api/users/me` | 401 | ALLOW (self) | ALLOW (self) | ALLOW (self) | ALLOW (self) | ALLOW (self) | ALLOW (self) |
| `GET /api/users`, `/api/users/{id}` | 401 | DENY (403) | DENY (403) | ALLOW | DENY (403) | ALLOW | ALLOW |
| `GET /api/profiles`, `/api/profiles/{id}` | 401 | DENY (403) | DENY (403) | ALLOW | DENY (403) | ALLOW | ALLOW |
| `GET /api/profiles/directory` | 401 | SCOPED (PII redacted) | SCOPED (PII redacted) | ALLOW (full) | SCOPED (PII redacted) | SCOPED (PII redacted) | ALLOW (full) |
| `GET/POST/PUT/DELETE /api/accountStatusLogs/**` | 401 | DENY (403) | DENY (403) | DENY (403) | DENY (403) | DENY (403) | ALLOW |
| `GET /api/v1/catalog` | 401 | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| `POST /api/v1/tickets` & `GET /api/v1/tickets/{id}` | 401 | SCOPED (own) | SCOPED (own/approver) | SCOPED (own/approver) | SCOPED (own/dept) | ALLOW (read) | ALLOW |
| `POST /api/v1/tickets/{id}/comments` (`isInternal=true`) | 401 | DENY (403) | DENY (403) | DENY (403) | SCOPED (dept) | DENY (403) | ALLOW |
| `GET /api/v1/approvals/tickets/{ticketId}` | 401 | SCOPED (own) | SCOPED (assigned/view) | SCOPED (assigned/view) | SCOPED (dept) | ALLOW (read) | ALLOW |
| `POST /api/v1/approvals/{id}/approve\|reject` | 401 | DENY (403) | SCOPED (no self-approve) | SCOPED (no self-approve) | DENY (403) | DENY (403) | SCOPED (no self-approve) |
| `GET /api/v1/departments/{dept}/queue` | 401 | DENY (403) | DENY (403) | DENY (403) | SCOPED (own dept) | DENY (403) | ALLOW (all depts) |
| `GET/PUT /api/v1/notifications/**` | 401 | SCOPED (owner only) | SCOPED (owner only) | SCOPED (owner only) | SCOPED (owner only) | SCOPED (owner only) | SCOPED (owner only) |
| `GET /api/v1/analytics/**` | 401 | DENY (403) | SCOPED (own dept) | SCOPED (own dept) | SCOPED (own dept) | ALLOW (org-wide) | ALLOW (org-wide) |
| `GET /api/v1/audit/**` & `/verify` | 401 | DENY (403) | DENY (403) | DENY (403) | DENY (403) | ALLOW | ALLOW |

---

## 3. OWASP Top 10 & API Security Verification Checklist

| Control Domain | Implementation Evidence | Verification Result |
| :--- | :--- | :---: |
| **API1:2023 Broken Object Level Authorization (BOLA)** | Tickets (`TicketSecurityEvaluator`), Approvals (`SEC-P9-04` fix), Notifications (`recipient.id == currentUser.id`), Legacy Profiles/Users (`SEC-P9-03` fix). | `VERIFIED` |
| **API2:2023 Broken Authentication** | `HttpOnly` + `SameSite=Lax` JWT cookies (`accessToken`, `refreshToken` scoped to `/api/auth`), BCrypt password hashing, 5-attempt lockout, immediate suspended-user JWT rejection in `JwtFilter`, refresh token DB revocation on logout. | `VERIFIED` |
| **API3:2023 Broken Object Property Level Authorization** | Sensitive PII (`salary`, `dob`, `address`, `emergencyContact`, `failedLoginAttempts`, `lockedUntil`) redacted on directory endpoint; internal specialist comments stripped for requesters and blocked on write (`SEC-P9-02`). | `VERIFIED` |
| **API4:2023 Unrestricted Resource Consumption** | Bounded pagination (`size <= 100`) on catalog, tickets, notifications, profiles, roles (`SEC-P9-05`); bounded async executor (`core=2, max=4, queue=100, CallerRunsPolicy`); analytics `maxRangeDays` clamping. | `VERIFIED` |
| **API5:2023 Broken Function Level Authorization** | `@EnableMethodSecurity` + explicit `@PreAuthorize` across all administrative, operational, approval, analytics, and audit controllers. | `VERIFIED` |
| **CSRF Protection (Double-Submit Cookie)** | `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `SpaCsrfTokenRequestHandler` + `CsrfCookieFilter` + `403` `AccessDeniedHandler` (`SEC-P9-06`). | `VERIFIED` |
| **Security Headers & XSS Mitigations** | `Content-Security-Policy` (`frame-ancestors 'none'; object-src 'none'`), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, React JSX auto-escaping, zero tokens stored in `localStorage`/`sessionStorage`. | `VERIFIED` |
| **SQL Injection Prevention** | 100% Spring Data JPA / JPQL parameterized queries; zero string-concatenated SQL. | `VERIFIED` |
| **Audit Integrity & Non-Repudiation** | Synchronous SHA-256 hash chain persisted within the mutating transaction; production reset blocked (`SEC-P9-01`). | `VERIFIED` |
| **Dependency Vulnerability Scanning (CVE)** | Offline sandbox lacks internet access for OWASP Dependency-Check / `npm audit`. | `BLOCKED` (Mandatory P10 CI Gate) |
| **TLS / `Secure` Cookie Flag in Production** | `app.security.cookie.secure` defaults to `false` for local HTTP development; must be set `true` behind HTTPS in P10 production config. | `DEFERRED` to P10 |

