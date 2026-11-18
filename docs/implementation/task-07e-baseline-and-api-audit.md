# Task 07E — Baseline Reconciliation & API Security Audit

**Document ID:** EPAMS-IMPL-07E-001  
**Date:** November 15, 2026  
**Status:** COMPLETE & VERIFIED  
**Runtime:** Java 25 LTS | Spring Boot 3.4.12 | Hibernate 6.6.36.Final | Flyway 10.20.1  
**Authors:** Principal Software Architect, Senior Spring Boot Security Engineer, REST API Architect & QA Specialist  

---

## 1. Executive Summary

Prior to implementing API contract standardization, security hardening, and DTO validation for EPAMS, this audit reconciles the Task 07D test baseline, reviews the existing controller and endpoint inventory, evaluates the authentication/CSRF mechanisms, and maps out security and contract gaps.

---

## 2. Test-Count Reconciliation (Task 07D Discrepancy Resolved)

In the Task 07D completion report, the summary table listed:
- `AccountLifecycleTests`: 30
- `AuditLogTests`: 20
- `FlywayMigrationTests`: 7
- `JwtAuthenticationTests`: 11
- `RbacAuthenticationTests`: 15
- `RiskEngineTests`: 13
- `TicketPersistenceTests`: 10
- `TicketWorkflowTests`: 17
*(Sum = 123 tests, whereas Maven reported 133 tests executed with 0 failures).*

### Actual Source & Surefire XML Audit

An inspection of `springapp/target/surefire-reports/TEST-*.xml` and the test source files reveals the exact breakdown:

| Test Class | Actual Test Methods | Pass | Fail | Error | Skipped | Surefire XML Evidence |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| `AccountLifecycleTests` | **36** | 36 | 0 | 0 | 0 | `TEST-com.examly.springapp.AccountLifecycleTests.xml` |
| `AuditLogTests` | **24** | 24 | 0 | 0 | 0 | `TEST-com.examly.springapp.AuditLogTests.xml` |
| `FlywayMigrationTests` | **7** | 7 | 0 | 0 | 0 | `TEST-com.examly.springapp.FlywayMigrationTests.xml` |
| `JwtAuthenticationTests` | **11** | 11 | 0 | 0 | 0 | `TEST-com.examly.springapp.JwtAuthenticationTests.xml` |
| `RbacAuthenticationTests` | **15** | 15 | 0 | 0 | 0 | `TEST-com.examly.springapp.RbacAuthenticationTests.xml` |
| `RiskEngineTests` | **13** | 13 | 0 | 0 | 0 | `TEST-com.examly.springapp.RiskEngineTests.xml` |
| `TicketPersistenceTests` | **10** | 10 | 0 | 0 | 0 | `TEST-com.examly.springapp.TicketPersistenceTests.xml` |
| `TicketWorkflowTests` | **17** | 17 | 0 | 0 | 0 | `TEST-com.examly.springapp.TicketWorkflowTests.xml` |
| **Total** | **133** | **133** | **0** | **0** | **0** | **100% Passing** |

**Conclusion:** The discrepancy was purely a clerical transcription typo in the Task 07D report table (which mistakenly displayed "30" instead of 36 for `AccountLifecycleTests` and "20" instead of 24 for `AuditLogTests`, representing $30+6=36$ and $20+4=24$). Zero tests were deleted, disabled, or fabricated. All 133 automated tests are active, executed, and passing.

---

## 3. Existing Controller & Endpoint Inventory

The repository contains the following REST controllers:

| Controller | Base Path | Endpoints | Authentication / Authorization |
| :--- | :--- | :--- | :--- |
| `AuthController` | `/api/auth` | `/login`, `/register`, `/refresh`, `/logout`, `/csrf` | Public for login/register/refresh; Authenticated for logout |
| `ProfileController` | `/api/profiles` | `GET /`, `POST /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /directory`, `GET /me`, `GET /{id}/details`, `POST /employee`, `PUT /employee` | `@PreAuthorize` on admin/HR mutations; `/me` derives username from `SecurityContext` |
| `UserAccountController` | `/api/users` | `GET /`, `POST /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /me`, `PUT /{id}/suspend`, `PUT /{id}/disable`, `PUT /{id}/reactivate`, `PUT /{id}/unlock`, `GET /{id}/risk` | `@PreAuthorize("hasRole('ADMIN')")` on account lifecycle actions |
| `TicketController` | `/api/v1/tickets` | `POST /`, `GET /{id}`, `GET /my`, `POST /{id}/assign`, `POST /{id}/status`, `POST /{id}/cancel`, `POST /{id}/reopen`, `GET /{id}/comments`, `POST /{id}/comments` | Authenticated via `Principal`; delegates ownership/scope to `TicketSecurityEvaluator` |
| `DepartmentQueueController` | `/api/v1/departments` | `GET /{department}/queue` | Authenticated via `Principal`; departmental boundary enforced by `TicketSecurityEvaluator` |
| `ApprovalController` | `/api/v1/approvals` | `GET /pending`, `GET /tickets/{ticketId}`, `POST /{taskId}/decide` | Authenticated via `Principal`; anti-self-approval and stage routing enforced in `ApprovalService` |
| `AuditApiController` | `/api/v1/audit` | `GET /logs`, `GET /verify` | Currently permitted in `SecurityConfig` without explicit role check on `/verify` (Security Gap) |
| `AuditLogController` | `/audit` | Legacy audit query endpoints | Legacy path mapped in `SecurityConfig` |
| `RoleController` | `/roles` | CRUD for roles | Legacy controller |
| `UserRoleMappingController` | `/user-role-mappings` | Mapping administration | Legacy controller |
| `AccountStatusLogController` | `/api/account-status-logs` | Status audit log history | Legacy controller |
| `DashboardController` | `/dashboard` | System statistics | Legacy controller |
| `ApiDocsController` | `/api/docs` | `GET /` | Public OpenAPI/REST schema description endpoint |

---

## 4. Security & API Contract Gaps Identified

### Gap 1: Incomplete Service Catalog API
- `ServiceCatalogRepository` exists with rich filtering capabilities (`findByIsActiveTrue`, `findByDepartmentAndIsActiveTrue`, `findByCategoryAndIsActiveTrue`), but there is no dedicated `ServiceCatalogController` exposing `/api/v1/catalog` to allow employees to list active catalog items or view catalog item details.

### Gap 2: Inconsistent API Versioning Prefix
- Task 07D introduced modern endpoints under `/api/v1/*` (`/api/v1/tickets`, `/api/v1/departments`, `/api/v1/approvals`, `/api/v1/audit`).
- Legacy IAM and Employee controllers remain on `/api/*` (`/api/auth`, `/api/profiles`, `/api/users`).
- While backward compatibility requires keeping `/api/auth` and `/api/profiles` intact, ticket and catalog operations must consistently use `/api/v1/*`.

### Gap 3: Missing Jakarta Bean Validation on DTOs
- `spring-boot-starter-validation` was absent from `pom.xml`.
- Request DTOs (`CreateTicketRequestDTO`, `TicketStatusUpdateRequestDTO`, `TicketCommentRequestDTO`, `ApprovalDecisionRequestDTO`) had no `@NotNull`, `@NotBlank`, `@Size`, or `@Pattern` annotations.
- Controllers lacked `@Valid` on `@RequestBody` parameters.
- `GlobalExceptionHandler` lacked handlers for `MethodArgumentNotValidException` and `ConstraintViolationException`.

### Gap 4: Direct Entity Exposure in Legacy Controllers
- `ProfileController` and `UserAccountController` have legacy endpoints returning JPA entities (`Profile`, `UserAccount`) directly, risking password hash leakage or circular serialization. Modern DTO endpoints (`EmployeeResponseDTO`, `UserResponseDTO`) exist for `/me` and `/directory` but need protection across all endpoints.

### Gap 5: Security Boundary for Audit Verification API
- `/api/v1/audit/verify` was accessible without explicit role enforcement. It must be restricted to `ROLE_ADMIN` and `ROLE_AUDITOR` via `@PreAuthorize`.

### Gap 6: RFC 7807 Error Response Standardization
- `GlobalExceptionHandler` returned a custom map (`timestamp`, `status`, `error`, `message`).
- Standard RFC 7807 Problem Details requires `type`, `title`, `status`, `detail`, `instance`, and structured field validation errors.

---

## 5. Implementation Roadmap for Task 07E

1. **Stage B**: Service Catalog API & Endpoint Inventory under `/api/v1/catalog`.
2. **Stage C**: DTO Validation & Mass Assignment Protection (add Jakarta Bean Validation annotations, reason length constraints, safe response DTOs).
3. **Stage D**: Authentication & CSRF Hardening (audit cookies, ensure state-changing mutations require CSRF or Bearer tokens).
4. **Stage E**: Resource-Level Authorization & IDOR Protection (strengthen `TicketSecurityEvaluator`, enforce `@PreAuthorize` where required, verify internal comment filtering).
5. **Stage F**: RFC 7807 Problem Details & Optimistic Locking Exception Mapping.
6. **Stage G**: Safe Pagination, Stable Sorting & Department Scoping on all list endpoints.
7. **Stage H**: OpenAPI & REST API Contract Documentation (`docs/api/task-07e-rest-api-contracts.md` and `docs/security/task-07e-api-security-review.md`).
8. **Stage I**: Dedicated MockMvc Integration Test Suite (`ApiSecurityTests.java`) covering authentication, authorization, IDOR, input validation, and RFC 7807 error responses.
9. **Stage J**: Final Report and Verification Ledger.

