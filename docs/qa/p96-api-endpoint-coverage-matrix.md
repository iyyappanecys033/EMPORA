# EPAMS — P9.6 API Endpoint Coverage Matrix (`86` REST Endpoints + `1` SPA Forward Controller)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage E: Controller-by-Controller Endpoint Coverage Matrix

| Controller | Endpoint Count | Key Paths & Methods | Auth / Scope Policy | Automated Verification Suites | Status |
| :--- | :---: | :--- | :--- | :--- | :---: |
| `AuthController` | 5 | `POST /api/auth/{login,register,refresh,logout}`, `GET /api/auth/csrf` | Public login/register/refresh/csrf; cookie+Bearer | `JwtAuthenticationTests`, `RbacAuthenticationTests`, `P96ApiAndFieldValidationTests` | **Verified** |
| `TicketController` | 9 | `POST /api/v1/tickets`, `GET /my`, `GET /{id}`, `POST /{id}/{assign,status,cancel,reopen,comments}`, `GET /{id}/comments` | Authenticated + `TicketSecurityEvaluator` (owner/dept/admin) | `TicketWorkflowTests`, `ApiSecurityTests`, `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `DepartmentQueueController` | 1 | `GET /api/v1/departments/{department}/queue` | Resolver (own dept) / `ADMIN` | `ApiSecurityTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `ApprovalController` | 3 | `GET /api/v1/approvals/pending`, `GET /tickets/{ticketId}`, `POST /{taskId}/decide` | Assigned Approver / Anti-self-approval | `TicketWorkflowTests`, `ApiSecurityTests`, `P9SecurityAndWorkflowQaTests` | **Verified** |
| `ServiceCatalogController` | 3 | `GET /api/v1/catalog`, `GET /{id}`, `GET /code/{itemCode}` | Authenticated (bounded pagination) | `ApiSecurityTests`, `P96ApiAndFieldValidationTests` | **Verified** |
| `NotificationController` | 4 | `GET /api/v1/notifications/my`, `GET /my/unread-count`, `PUT /{id}/read`, `PUT /my/read-all` | Owner-scoped (`Principal`) | `NotificationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `AnalyticsController` | 9 | `GET /api/v1/analytics/{dashboard,overview,tickets/*,departments/performance,services/popularity,sla/compliance,approvals/overview}` | `ADMIN`, `AUDITOR`, `MANAGER`, `HR_MANAGER`, `DEPT_HEAD`, Resolvers | `AnalyticsTests`, `P96RbacFieldSecurityIdorTests`, Live Perf Suite | **Verified** |
| `AuditApiController` | 2 | `GET /api/v1/audit/logs`, `GET /api/v1/audit/verify` | `ADMIN`, `AUDITOR` | `ApiSecurityTests`, `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `AuditLogController` | 5 | `GET /api/audit-logs`, `/{id}`, `/action/{action}`, `/actor/{actor}`, `/target/{target}` | `ADMIN` | `AuditLogTests`, `P96ApiAndFieldValidationTests` | **Verified** |
| `ProfileController` | 12 | `GET /api/profiles/{me,directory,/{id}/details,/,/{id},/name/*,/search/*}`, `POST/PUT /employee`, `POST/PUT/DELETE /` | `/me` & `/directory` auth (PII redacted); mutations `ADMIN`/`HR_MANAGER`; delete `ADMIN` | `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `UserAccountController` | 11 | `GET /api/users/me`, CRUD `/{id}`, `PUT /{id}/{suspend,disable,reactivate,unlock}`, `GET /{id}/risk` | `/me` auth; reads `ADMIN`/`HR_MANAGER`/`AUDITOR`; lifecycle/risk `ADMIN` | `AccountLifecycleTests`, `RbacAuthenticationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `RoleController` | 6 | `POST/GET/PUT/DELETE /api/roles`, `GET /page/{page}/{size}` | Reads `ADMIN`/`AUDITOR`; writes `ADMIN` | `RbacAuthenticationTests`, `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `UserRoleMappingController` | 8 | CRUD + `/expanded`, `/user/{userId}`, `/user/{userId}/role/{roleId}` | Reads `ADMIN`/`AUDITOR`; writes `ADMIN` | `RbacAuthenticationTests`, `P96ApiAndFieldValidationTests` | **Verified** |
| `AccountStatusLogController` | 5 | `POST/GET/PUT/DELETE /api/accountStatusLogs` | `ADMIN` | `AccountLifecycleTests`, `P96ApiAndFieldValidationTests` | **Verified** |
| `DashboardController` | 1 | `GET /api/dashboard/metrics` | `ADMIN` | `Stage1RemediationVerificationTests`, `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` | **Verified** |
| `HealthController` | 1 | `GET /api/v1/health` | Public (`permitAll`) | `P96ApiAndFieldValidationTests`, Live Perf & Restart Suite | **Verified** |
| `ApiDocsController` | 1 | `GET /api/docs` | Public (`permitAll`) | `P96ApiAndFieldValidationTests` | **Verified** |
| `SpaController` | 18 SPA mappings | `/`, `/catalog`, `/tickets/**`, `/approvals`, `/queue/**`, `/audit-chain`, `/notifications/**`, `/analytics/**`, `/profile`, `/preferences`, `/admin/metrics`, `/users/**`, `/roles/**`, `/profiles/**`, `/user-role-mappings/**`, `/audit/**` | Public forward to `/index.html` (data remains API-authenticated) | `P96ApiAndFieldValidationTests` + Standalone JAR live verification | **Verified** |
| **Total** | **86 REST APIs + SPA** | **100% of discovered controllers mapped** | **All boundaries tested** | **229 backend + 49 frontend + live Chrome** | **100% Covered** |
