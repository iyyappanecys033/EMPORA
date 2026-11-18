# EPAMS REST API Security Architecture & Hardening Review

**Document Version:** 1.0.0  
**Phase:** Phase 2 — Testing Foundation & Workflow Orchestration  
**Task:** Task 07E — REST API Contracts, Security Hardening & DTO Validation  
**Date:** November 2026  
**Status:** Complete & Verified  

---

## 1. Executive Summary

This security review details the defensive posture, authentication mechanisms, authorization boundaries, input sanitization, and cryptographic audit controls implemented across the EPAMS REST API boundary. 

All endpoints have been hardened against common API security risks, including:
- Insecure Direct Object References (IDOR / BOLA)
- Broken Object Property Level Authorization (BOPLA / Mass Assignment)
- Cross-Site Request Forgery (CSRF)
- Role Authority Pollution & Privilege Escalation
- Uncontrolled Concurrency & Race Conditions (TOCTOU)
- Audit Trail Tampering & Repudiation

---

## 2. Authentication Architecture

The EPAMS platform supports a dual-credential authentication scheme tailored for both single-page web applications (SPA) and backend service integrations:

### 2.1 HttpOnly Session Cookies (Browser Clients)
- **Token Delivery:** JWT Access Token stored in `Set-Cookie: accessToken=<JWT>; Path=/; HttpOnly; SameSite=Strict; Secure`.
- **XSS Mitigation:** `HttpOnly` flag prevents client-side JavaScript execution contexts from accessing or exfiltrating the token.
- **CSRF Defense:** Combined with Spring Security's `CookieCsrfTokenRepository` (`SameSite=Strict` + `X-XSRF-TOKEN` header validation on non-safe HTTP methods).

### 2.2 Bearer Authorization Headers (Service / CLI Clients)
- **Token Delivery:** `Authorization: Bearer <JWT>`.
- **Processing:** Extracted by `JwtAuthenticationFilter` and validated via HMAC-SHA256 signature against the platform secret key.
- **CSRF Exemption:** Bearer token headers cannot be ambiently attached by web browsers in cross-origin contexts; hence, requests bearing a valid `Authorization: Bearer` header are exempt from standard cookie-based CSRF checks.

### 2.3 Role Authority Normalization
To prevent Spring Security authority mismatches and `"ROLE_ROLE_..."` double-prefixing:
```java
// CustomUserDetailsService.java
String roleName = mapping.getRole().getRoleName();
String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
authorities.add(new SimpleGrantedAuthority(authority));
```
This guarantees uniform evaluation across `@PreAuthorize("hasRole('ADMIN')")` and `hasAuthority("ROLE_ADMIN")`.

---

## 3. Authorization Boundaries & Access Control

EPAMS enforces fine-grained Role-Based Access Control (RBAC) augmented with Attribute-Based Access Control (ABAC) through `TicketSecurityEvaluator`:

### 3.1 Role Hierarchy & Persona Capabilities
| Persona | Standard Role | Permitted Actions |
|:---|:---|:---|
| **Employee** | `ROLE_EMPLOYEE` | Browse Catalog, Submit Tickets, View Own Tickets, Add Public Comments, Cancel Own Tickets. |
| **Manager** | `ROLE_MANAGER` | Employee capabilities + Approve/Reject direct report tickets in Approval Queue. |
| **Dept Head** | `ROLE_DEPT_HEAD` | Manager capabilities + Stage 2 Financial/Resource Approvals + View Department Queue. |
| **Resolver** | `ROLE_RESOLVER`, `ROLE_IT_SUPPORT`, `ROLE_FACILITIES_CREW`, `ROLE_HR_SPECIALIST` | View Assigned Department Queue, Self-Assign Tickets, Update Ticket Status, Add Internal Notes. |
| **Auditor** | `ROLE_AUDITOR` | Read-only inspection of Audit Logs & Execution of Cryptographic Audit Chain Verification. |
| **Admin** | `ROLE_ADMIN` | Full global access across all departments, ticket overrides, and user management. |

### 3.2 Departmental Isolation (Cross-Queue Containment)
Resolvers are strictly confined to tickets and queues matching their assigned department profile:
```java
// TicketSecurityEvaluator.java
public boolean canAccessDepartmentQueue(String department, Authentication auth) {
    if (isAdmin(auth)) return true;
    UserAccount user = getUserAccount(auth);
    Profile profile = profileRepo.findByUserId(user.getId()).orElse(null);
    if (profile == null || profile.getDepartment() == null) return false;
    return profile.getDepartment().equalsIgnoreCase(department) && isResolver(auth);
}
```
A resolver belonging to `Facilities` cannot access the `IT` queue or mutate `IT` tickets (`403 Forbidden`).

---

## 4. IDOR (Insecure Direct Object Reference) Protection

To prevent IDOR vulnerabilities (OWASP API3:2023 Broken Object Property Level Authorization & Broken Object Level Authorization):

1. **Ticket Access (`/api/v1/tickets/{id}`):**
   - Evaluated before controller invocation via `@PreAuthorize("@ticketSecurityEvaluator.canAccessTicket(#id, authentication)")`.
   - Access is granted **only** if the caller is the ticket requester, the assigned resolver, a resolver in the ticket's department, a manager with a pending approval task for that ticket, an auditor, or an administrator.
   - Any other employee attempting to view or guess ticket IDs is rejected with `403 Forbidden`.

2. **Approval Task Access (`/api/v1/approvals/{taskId}/decision`):**
   - Validated against `task.getApprover().getId() == caller.getId()`.
   - Anti-Self-Approval guard: Even if a manager is somehow assigned an approval task for their own ticket, the system rejects approval attempts with `403 Forbidden`.

3. **Comment Visibility Filtering:**
   - Internal resolver notes (`isInternal = true`) are stripped from responses returned to ticket requesters. Requesters only receive public customer-facing comments.

---

## 5. Input Validation & Mass Assignment Protection

1. **Dedicated Request DTOs:**
   Domain entities (`ServiceTicket`, `ApprovalTask`, `UserAccount`) are NEVER directly bound as `@RequestBody` controller parameters.
2. **Jakarta Bean Validation:**
   All incoming request payloads are strictly validated using `@Valid`:
   - `@NotNull`, `@NotBlank` on mandatory identifiers and descriptions.
   - `@Size(max = ...)` bounds to prevent database column overflow and denial-of-service payloads.
   - Explicit enumerations (`TicketPriority`, `TicketStatus`, `ApprovalDecision`) prevent illegal state injection.
3. **Optimistic Locking (`@Version`):**
   State mutation DTOs require explicit `version` fields. Stale writes trigger `ObjectOptimisticLockingFailureException` and map to `409 Conflict`.

---

## 6. Cryptographic Audit Chain Boundaries

1. **Tamper-Evident Ledger:**
   Every ticket creation, status change, assignment, cancellation, reopening, and comment is synchronously chained using SHA-256 hash pointers (`previous_hash`, `current_hash`, `sequence_number`).
2. **Dedicated Verification Boundary:**
   The verification endpoint `/api/v1/audit/verify` is restricted exclusively to `ROLE_ADMIN` and `ROLE_AUDITOR`. Regular employees, resolvers, and managers cannot invoke chain verification (`403 Forbidden`).
3. **Read-Only Auditor Persona:**
   Auditors are granted read-only access and cannot alter tickets or execute operational workflows.

