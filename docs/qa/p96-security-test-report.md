# EPAMS — P9.6 Application Security Verification Report (Stage J)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification (Stage J)  
**Baseline:** `a4857679c0bde310e4b705a46d7e7ef701bc3901` + P9.6 verified remediations (`QA-DEF-001`–`QA-DEF-005`)  
**Verification Date:** 2026-11-18  
**Verification Scope:** OWASP ASVS / OWASP Top 10 automated backend security tests, live HTTP header/cookie inspection, RBAC/IDOR negative testing, CSRF verification, and audit immutability controls.  

> **Honesty Boundary:** This report reflects **automated security test suites, configuration inspection, and targeted negative attack testing** executed inside the local repository environment. It is **not** a substitute for an independent external penetration test, DAST crawl, or formal SOC 2 / ISO 27001 security certification.

---

## 1. Executive Security Summary

| Security Domain | Automated Verification Status | Automated Evidence Source | Residual Risk / Gap |
| :--- | :---: | :--- | :--- |
| **Authentication & Session Security** | **PASS** | `RbacAuthenticationTests`, `P96RbacFieldSecurityIdorTests`, `P96ApiAndFieldValidationTests` | Local H2 dev fallback secret exists only in non-MySQL profile; production MySQL profile requires `JWT_SECRET` env var. |
| **Broken Access Control (IDOR / BOLA)** | **PASS** | `P96RbacFieldSecurityIdorTests` (11 IDOR/BOLA attack vectors) | Zero cross-user, cross-department, or privilege escalation leaks observed. |
| **Mass Assignment & Privilege Escalation** | **PASS** | `RbacAuthenticationTests.test14`, `P96RbacFieldSecurityIdorTests.test2` | `roles`, `salary`, `status` ignored/blocked on self-service endpoints. |
| **CSRF Protection (Double-Submit Cookie)** | **PASS** | `SecurityConfig`, `P96ApiAndFieldValidationTests`, `RbacAuthenticationTests` | Enforced on cookie-authenticated mutations; `GET /api/auth/csrf` issues `XSRF-TOKEN`. |
| **SQL Injection & Order-By Injection** | **PASS** | `P96ApiAndFieldValidationTests`, `ProfileController` `sortBy` allowlist (`QA-DEF-002`) | Parameterized JPA/JPQL everywhere; `sortBy=salary` oracle & invalid property HTTP 500 eliminated. |
| **XSS & Output Encoding** | **PASS (Automated)** | `P96ApiAndFieldValidationTests` + React JSX auto-escaping | Zero `dangerouslySetInnerHTML` usages in React frontend; CSP `script-src 'self'`. |
| **Error Handling & Information Disclosure** | **PASS** | `GlobalExceptionHandler` (`QA-DEF-004`) + `P96ApiAndFieldValidationTests` | RFC 7807 `ProblemDetail` across 400/401/403/404/409/415/500; raw `e.getMessage()` stripped from HTTP 500. |
| **HTTP Security Headers** | **PASS** | `P96RbacFieldSecurityIdorTests.test5` | `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Referrer-Policy` verified. |
| **Audit Chain Tamper-Evidence & Immutability** | **PASS** | `P96UnitBusinessLogicTests.test5`, `Stage2RealMysqlIntegrationTests.test3` | SHA-256 hash chain verified; `epams_app` denied `UPDATE`/`DELETE` on `audit_log` on MySQL 8. |
| **Static SAST / External DAST** | **NOT EXECUTED** | `semgrep` / `trivy` not installed locally | Recommended for CI/P10 security pipeline. |

---

## 2. Detailed OWASP Control Verification Matrix

### 2.1 Authentication, Session & Token Controls (OWASP A07)
| Test Case | Attack / Scenario Verified | Expected Behavior | Observed Result | Status |
| :--- | :--- | :--- | :--- | :---: |
| `SEC-AUTH-01` | Malformed / expired / tampered JWT bearer token | HTTP `401 Unauthorized` | Rejected before controller invocation | **PASS** |
| `SEC-AUTH-02` | `SUSPENDED` account reusing previously issued valid JWT (`P96RbacFieldSecurityIdorTests.test5`) | Immediate HTTP `401` on next API request | `JwtFilter` re-checks account active state on DB lookup → HTTP `401` | **PASS** |
| `SEC-AUTH-03` | Self-registration with forged `"roles": ["ROLE_ADMIN"]` payload | Ignored or rejected; never elevated | Account strictly assigned `ROLE_EMPLOYEE` (`RbacAuthenticationTests.test14`) | **PASS** |
| `SEC-AUTH-04` | Account lockout after 5 consecutive bad logins (`RbacAuthenticationTests`) | Account locked (`423`/`401`); brute force blocked | Lockout enforced and audited | **PASS** |
| `SEC-AUTH-05` | `HttpOnly` + `SameSite` auth cookie flags (`application-mysql.properties`) | `app.security.cookie.secure=true` default in MySQL profile | Verified in config; `COOKIE_SECURE=false` override only for local HTTP test harness | **PASS** |
| `SEC-AUTH-06` | Password hash exposure on `/api/users`, `/api/admin/**`, `/api/audit/**` | Zero `passwordHash` or `$2a$` strings serialized | Verified in `P96RbacFieldSecurityIdorTests.test3` (`assertFalse(body.contains("$2a$"))`) | **PASS** |

### 2.2 Authorization, IDOR/BOLA & Data Isolation (OWASP A01)
- **11 IDOR/BOLA Attack Vectors Verified** (full detail in [p96-idor-bola-test-report.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/qa/p96-idor-bola-test-report.md)):
  - Employee accessing another employee's ticket (`GET /api/v1/tickets/{id}`): HTTP `403`.
  - Employee commenting on / cancelling / transitioning another employee's ticket: HTTP `403`.
  - Non-approver or wrong approver calling `POST /api/v1/approvals/{id}/decision`: HTTP `403`.
  - IT resolver attempting to view or mutate HR/Finance department queues: HTTP `403`.
  - Non-HR/Admin calling `GET /api/employees/directory?sortBy=salary` (`QA-DEF-002` fix): HTTP `400` RFC 7807 `Invalid sort field` (eliminates salary ordering oracle).
  - Non-owner marking another user's notification read (`PUT /api/v1/notifications/{id}/read`): HTTP `403`/`404`, unread count unchanged.
  - `ROLE_AUDITOR` attempting write/mutation endpoints (catalog, ticket transition, admin update): HTTP `403` (strictly read-only).
  - Employee reading internal resolver notes (`isInternal=true`): Filtered out of `TicketCommentDTO` list before serialization.

### 2.3 Input Validation, Injection & Content-Type Hardening (OWASP A03 / A04)
- **Remediated during P9.6 (`QA-DEF-003`, `QA-DEF-004`):**
  - Added Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@PositiveOrZero`) to `RegisterRequest`, `LoginRequest`, and `EmployeeRequestDTO`, and `@Valid` to `AuthController` (`login`, `register`) and `ProfileController` (`saveEmployee`, `updateEmployee`).
  - Added explicit `GlobalExceptionHandler` RFC 7807 handlers for:
    - `HttpMediaTypeNotSupportedException` → HTTP `415 Unsupported Media Type`
    - `MissingServletRequestParameterException` → HTTP `400 Bad Request`
    - `PropertyReferenceException` → HTTP `400 Bad Request`
    - `NoResourceFoundException` → HTTP `404 Not Found`
  - Removed raw `e.getMessage()` exposure from generic `Exception` HTTP `500` handler (now returns safe constant `"An unexpected internal error occurred."`).
- **Injection Payloads Tested (`P96ApiAndFieldValidationTests`):**
  - SQL injection payload (`' OR '1'='1'; DROP TABLE service_ticket; --`) in search query & ticket description → treated strictly as literal parameter; zero SQL errors, zero table loss.
  - XSS payload (`<script>alert('xss')</script><img src=x onerror=alert(1)>`) in ticket description & comment → stored safely as text, serialized as JSON string, rendered via React JSX escaping (no raw HTML injection).
  - Unicode / multibyte payload (Tamil script `தமிழ் ஆதரவு கோரிக்கை` + emoji `🚀`) → preserved losslessly across UTF-8 / `utf8mb4`.
  - Oversized fields & malformed JSON (`{invalid`) → cleanly rejected with RFC 7807 HTTP `400`.

### 2.4 Security Headers & Browser Hardening (OWASP A05)
Verified live via `P96RbacFieldSecurityIdorTests.test5`:
- `X-Content-Type-Options: nosniff` — **PRESENT**
- `X-Frame-Options: DENY` — **PRESENT**
- `Content-Security-Policy` — **PRESENT** (restrictive policy configured in `SecurityConfig`)
- `Referrer-Policy` — **PRESENT**
- `Cache-Control: no-cache, no-store, max-age=0, must-revalidate` on authenticated API responses — **PRESENT**

### 2.5 Cryptographic Audit Integrity & Least-Privilege DB Controls (OWASP A09)
- `AuditChainService.resetChainForTesting()` is blocked in production (`epams.audit.allow-chain-reset=false` in `application-mysql.properties`) and throws `IllegalStateException` (`P96UnitBusinessLogicTests.test5`).
- Any tampering with `audit_log` content, hash, or sequence is detected deterministically by `GET /api/v1/audit/verify`.
- On real MySQL `8.0.45`, runtime account `epams_app` is restricted to `SELECT, INSERT` on `audit_log` and `SELECT, UPDATE` on `audit_chain_head` — `UPDATE`/`DELETE` attempts fail at the database engine level (`ERROR 1142`).

---

## 3. Security Gaps & Pre-P10 Recommendations

1. **MySQL TLS Transport (`application-mysql.properties`):** Local loopback test profile uses `useSSL=false&allowPublicKeyRetrieval=true`. Production P10 deployment **must** enforce `sslMode=VERIFY_IDENTITY` (or managed cloud TLS) and disable `createDatabaseIfNotExist=true`.
2. **External SAST/DAST & Penetration Testing:** Run Semgrep / CodeQL + Trivy container/filesystem scan in CI and schedule an independent OWASP DAST scan against a staging deployment before general availability.
3. **Frontend Direct Dependency Patching:** Plan controlled patch of `axios` and `react-router-dom` (detailed in [p96-vulnerability-assessment.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/qa/p96-vulnerability-assessment.md)).
4. **HSTS Header at Edge / Reverse Proxy:** Ensure `Strict-Transport-Security: max-age=31536000; includeSubDomains` is enabled when TLS terminates in P10 production ingress/load balancer.
