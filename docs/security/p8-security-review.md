# EPAMS P8 — Security Review (Notifications, Analytics & Observability)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Notifications, Performance & Analytics  
**Date:** 2026-11-17  
**Classification:** `[VERIFIED]`  

---

## 1. Notification Security Verification Matrix

| Security Control | Implementation Mechanism | Automated Verification Test | Status |
| :--- | :--- | :--- | :---: |
| **Authentication Mandatory** | `SecurityConfig.java`: `.anyRequest().authenticated()` | `NotificationTests.test1_UnauthenticatedNotificationRequestsReturn401` | `VERIFIED` |
| **Strict Ownership Isolation** | `NotificationService.java`: resolves `userAccountRepo.findByUsername(principal.getName())` and checks `alert.getRecipient().getId().equals(user.getId())` | `NotificationTests.test6_OwnershipIsolationAndIdorPreventionEvenForAdmin` | `VERIFIED` |
| **Admin Private Notification Boundary** | Even `ROLE_ADMIN` receives `403 Forbidden` if attempting to mark another user's private notification as read | `NotificationTests.test6_OwnershipIsolationAndIdorPreventionEvenForAdmin` | `VERIFIED` |
| **CSRF Mutation Protection** | Spring `CookieCsrfTokenRepository` enforced on `PUT /api/v1/notifications/{id}/read` and `PUT /api/v1/notifications/my/read-all` | `NotificationTests.test7_CsrfProtectionRequiredOnNotificationMutations` | `VERIFIED` |
| **Pagination Abuse Prevention** | `size` clamped to `1..50` and `page >= 0` in `NotificationService` | `NotificationTests.test8_PaginationBoundsPreventUnboundedQueries` | `VERIFIED` |
| **Internal Comment Confidentiality** | Internal specialist notes are never emitted into `TicketNotificationEvent` messages | `NotificationTests.test12_InternalCommentsNeverLeakIntoNotifications` | `VERIFIED` |
| **Rollback Safety** | `@TransactionalEventListener(phase = AFTER_COMMIT)` guarantees rolled-back transactions generate zero alerts | `NotificationTests.test11_RolledBackBusinessTransactionDoesNotPersistNotifications` | `VERIFIED` |

---

## 2. Analytics Security & Privacy Verification Matrix

| Security Control | Implementation Mechanism | Automated Verification Test | Status |
| :--- | :--- | :--- | :---: |
| **Unauthenticated Blocking** | Spring Security filter rejects anonymous callers with `401 Unauthorized` | `AnalyticsTests.test1_AuthorizationEnforcedForEmployeeAndCrossDepartmentScope` | `VERIFIED` |
| **Employee Role Denial** | Standard `ROLE_EMPLOYEE` rejected with `UnauthorizedScopeException` (`403 Forbidden`) | `AnalyticsTests.test1_AuthorizationEnforcedForEmployeeAndCrossDepartmentScope` | `VERIFIED` |
| **Cross-Department Parameter Tampering** | `AnalyticsService.resolveAuthorizedDepartmentScope()` validates `department` query param against `Profile.department`; spoofing another department throws `403 Forbidden` | `AnalyticsTests.test1_AuthorizationEnforcedForEmployeeAndCrossDepartmentScope` | `VERIFIED` |
| **Auditor Read-Only Scope** | `ROLE_AUDITOR` receives org-wide read-only analytics (`GET` only; zero mutation endpoints exist) | `AnalyticsTests.test1_AuthorizationEnforcedForEmployeeAndCrossDepartmentScope` | `VERIFIED` |
| **PII & Employee Ranking Privacy** | Analytics return strictly aggregated counts/rates; zero salary, personal contact info, or individual employee rankings exposed | Code inspection (`AnalyticsService.java`, `dto/analytics/*`) | `VERIFIED` |
| **Query DoS Prevention** | Date range validated (`startDate <= endDate`, max span `366` days); popularity `limit` clamped `1..25` | `AnalyticsTests.test3_InvalidDateRangesRejectedWith400BadRequest` | `VERIFIED` |

---

## 3. Observability & Logging Hygiene

- **Correlation ID Sanitization (`CorrelationIdFilter.java`):** Incoming `X-Correlation-Id` headers are stripped to `[a-zA-Z0-9\-_.]` and truncated to 64 characters to prevent CRLF log injection and XSS reflection (verified in `PerformanceBenchmarkTests.test2`).
- **Zero Secret Logging:** `CorrelationIdFilter` logs only `correlationId`, `method`, `path`, `status`, and `durationMs`—never logging `Authorization` headers, `Cookie` headers, JWTs, refresh tokens, passwords, or request payloads.
- **Health Endpoint Minimality (`HealthController.java`):** `GET /api/v1/health` exposes only `status`, `database`, `timezone`, and `timestamp`—never exposing JDBC URLs, database credentials, hostnames, or exception stack traces.

