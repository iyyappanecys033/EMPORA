# EPAMS P7 — Frontend Security Review & Threat Assessment

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P7 — Complete Frontend Integration & Security Verification  
**Date:** 2026-11-17  
**Author:** Application Security Engineer & DevSecOps Lead  
**Classification:** `[VERIFIED]`  

---

## 1. Executive Security Assessment

The P7 frontend integration was evaluated against the OWASP Top 10 (2021) and enterprise identity guidelines. The security posture is hardened and robust:
- **No Token Storage in LocalStorage:** Authentication utilizes HTTP-Only, SameSite=Lax cookies issued by Spring Security. Zero tokens are stored in `localStorage` or `sessionStorage`, mitigating Cross-Site Scripting (XSS) credential exfiltration.
- **CSRF Token Injection:** State-modifying requests (`POST`, `PUT`, `DELETE`, `PATCH`) automatically read the `XSRF-TOKEN` cookie and set the custom `X-XSRF-TOKEN` header via the centralized Axios request interceptor (`src/services/api.js`).
- **Defensive Silent 401 Refresh Queue:** Handles asynchronous session refreshes using a subscriber queue (`refreshSubscribers`) with `isRefreshing` latching, preventing concurrent refresh storms and infinite retry loops.
- **Backend-Authoritative Boundaries:** Frontend role predicates (`isResolver`, `isApprover`, `isAuditor`, `isAdmin`) drive UX navigation only. Backend `@PreAuthorize` rules and `TicketSecurityEvaluator` enforce authoritative security and reject any forged client-side state with HTTP 403.

---

## 2. Threat Vector Evaluation Matrix

| Threat Vector | OWASP Ref | Frontend Mitigation | Backend Authoritative Control | Status |
| :--- | :--- | :--- | :--- | :---: |
| **XSS Credential Theft** | A03:2021 | Tokens stored in HTTP-Only cookies; React auto-escapes all JSX bindings; zero `dangerouslySetInnerHTML`. | `accessToken` cookie flagged `httpOnly: true`. | **SECURE** |
| **Cross-Site Request Forgery** | A01:2021 | Request interceptor parses `XSRF-TOKEN` cookie and injects `X-XSRF-TOKEN` header on mutations. | `CookieCsrfTokenRepository` validates header match on all mutating requests. | **SECURE** |
| **Insecure Direct Object Reference (IDOR)** | A01:2021 | Client displays only user's tickets in `/my-tickets`; routes restricted by role. | `TicketSecurityEvaluator.canViewTicket()` validates requester or department assignment. | **SECURE** |
| **Cross-Department Ticket Tampering** | A01:2021 | Specialist workspace loads queue for active department. | `TicketSecurityEvaluator.canAccessDepartmentQueue()` rejects other departments with 403. | **SECURE** |
| **Anti-Self-Approval Bypass** | A04:2021 | `ApprovalQueue.js` compares `requesterUsername` with `currentUser.username` and disables approval. | `ApprovalService.java:128` throws `BusinessRuleViolationException` if requester == approver. | **SECURE** |
| **Confidential Note Leakage** | A01:2021 | Non-specialists cannot see internal note submission controls. | `TicketQueryService.java:98` strips `isInternal == true` comments for standard requesters. | **SECURE** |
| **Audit Ledger Tampering** | A08:2021 | UI provides no edit/delete controls; only queries logs and verifies ledger. | Cryptographic SHA-256 hash chains with monotonic sequence numbering; immutable table. | **SECURE** |
| **Replay & Concurrency Conflicts** | A04:2021 | Version token sent with status updates and approval decisions. | Hibernate `@Version` and optimistic locking throw 409 Conflict upon collision. | **SECURE** |

---

## 3. Residual Security Notes & Hardening Recommendations

1. **Cold-Start CSRF Cookie Synchronization:**
   - On a brand new browser session navigating directly to a form, the `XSRF-TOKEN` cookie might not yet be initialized until the first GET request completes.
   - *Current Mitigation:* `AuthContext.checkAuth()` executes immediately upon application mount, querying `GET /api/users/me` and triggering Spring Security's CSRF token cookie creation.
2. **Production Cookie Flags:**
   - In development/test environments, `app.security.cookie.secure` is `false` for localhost HTTP testing.
   - *Deployment Requirement:* In production (`application-mysql.properties` or staging), configure `app.security.cookie.secure=true` to enforce HTTPS-only cookie transmission.

