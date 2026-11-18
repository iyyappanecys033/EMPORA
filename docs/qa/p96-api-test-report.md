# EPAMS — P9.6 API Test Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage E: Complete API Verification
**Scope:** All `18` Spring controllers (`86` REST API endpoints + `9` SPA routes in `SpaController`)

---

## 1. Executive Summary

Every Spring controller in EPAMS was audited from source and verified across positive flows, authentication boundaries (unauthenticated `401`, cookie+CSRF, Bearer JWT), role/ownership/department authorization (`403`), invalid/missing identifiers (`404`), Bean Validation (`400`), malformed JSON (`400`), unsupported media type (`415`), optimistic locking conflicts (`409`), and RFC 7807 Problem Details compliance.

- **API Regression Suites Executed:** `ApiSecurityTests` (`19`), `JwtAuthenticationTests` (`14`), `RbacAuthenticationTests` (`15`), `AccountLifecycleTests` (`37`), `AnalyticsTests` (`8`), `NotificationTests` (`12`), `P9SecurityAndWorkflowQaTests` (`10`), `P9LiveServerE2EIntegrationTests` (`2`), `P96ApiAndFieldValidationTests` (`5`), `P96RbacFieldSecurityIdorTests` (`5`).
- **Result:** **100% passing (`0` failures, `0` errors)** across all API contract tests.

---

## 2. RFC 7807 Problem Details & Error Hygiene Verification

Verified live against `GlobalExceptionHandler.java` (after `QA-DEF-004` remediation):

| HTTP Status | Triggering Scenario Tested | Verified Response Contract | Stack Trace / SQL Leakage |
| :---: | :--- | :--- | :---: |
| `200` / `201` / `204` | Valid reads, creations, deletes across all domains | Valid JSON DTOs; `passwordHash` omitted | None |
| `400` | Missing/invalid DTO fields (`RegisterRequest`, `LoginRequest`, `CreateTicketRequestDTO`, `EmployeeRequestDTO`, `ApprovalDecisionRequestDTO`), malformed JSON, invalid `sortBy` (`salary` or unknown), invalid path/query types | RFC 7807 JSON (`type`, `title`, `status=400`, `detail`, `instance`, `timestamp`, `correlationId`, `errors[]`) | None |
| `401` | Unauthenticated protected API calls, invalid credentials, expired/tampered JWT, suspended account JWT replay | HTTP `401` (`HttpStatusEntryPoint` / `AuthController`) | None |
| `403` | Missing CSRF on cookie mutation, unauthorized role, cross-user IDOR, cross-department queue | RFC 7807 `application/problem+json` (`status=403`) | None |
| `404` | Unknown catalog ID/code, unknown ticket ID, unknown resource | RFC 7807 (`status=404`, `NoResourceFoundException` / `ResourceNotFoundException`) | None |
| `405` | Unsupported HTTP verb (e.g., `PATCH` on `PUT`-only notification endpoint) | RFC 7807 (`status=405`) | None |
| `409` | Stale `@Version` concurrent ticket modification | RFC 7807 (`status=409`, `https://epams.internal/errors/concurrency-conflict`) | None |
| `415` | `text/plain` sent to JSON endpoint (`POST /api/auth/login`) | RFC 7807 (`status=415`, fixed via `QA-DEF-004`) | None |
| `500` | Unhandled server exception path (`GlobalExceptionHandler.handleException`) | Static safe message `"An unexpected internal error occurred."` (raw `e.getMessage()` removed via `QA-DEF-004`; full trace logged server-side only) | **Eliminated** |

---

## 3. Defects Found & Remediated in Stage E
- **`QA-DEF-001`:** Expanded `SpaController` & `SecurityConfig` so all modern React SPA routes forward cleanly to `/index.html` (`200`) on the standalone JAR.
- **`QA-DEF-002`:** Enforced strict `sortBy` allowlist on `GET /api/profiles/directory` (blocking `sortBy=salary` for non-HR/Admin and preventing `PropertyReferenceException` HTTP 500).
- **`QA-DEF-003`:** Added Jakarta Bean Validation (`@Valid`) to `RegisterRequest`, `LoginRequest`, and `EmployeeRequestDTO`.
- **`QA-DEF-004`:** Added `415`/`404`/`400` exception handlers and eliminated raw `e.getMessage()` leakage from HTTP `500` responses in `GlobalExceptionHandler`.
