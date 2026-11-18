# EPAMS — P9.6 Role-Based Access Control (RBAC) Matrix

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage G: Complete 11-Role Authorization Matrix

## 1. Actual Roles Discovered in Source (`11` Roles)
`ADMIN`, `AUDITOR`, `DEPT_HEAD`, `EMPLOYEE`, `FACILITIES_CREW`, `HR_MANAGER`, `HR_SPECIALIST`, `IT_SUPPORT`, `MANAGER`, `RESOLVER`, `USER`

## 2. Role × Operation × Scope Matrix (Verified via Automated & Live Browser Tests)

| Capability / API | `EMPLOYEE` / `USER` | `MANAGER` / `DEPT_HEAD` | `IT_SUPPORT` / `HR_SPEC` / `FAC_CREW` / `RESOLVER` | `HR_MANAGER` | `AUDITOR` | `ADMIN` | Verified Evidence |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| Public Auth / Health / Docs | Allow | Allow | Allow | Allow | Allow | Allow | `P96ApiAndFieldValidationTests` |
| Browse Service Catalog (`/api/v1/catalog`) | Allow | Allow | Allow | Allow | Allow | Allow | `ApiSecurityTests`, `P96ApiAndFieldValidationTests` |
| Create Ticket (`POST /api/v1/tickets`) | Allow (self) | Allow (self) | Allow (self) | Allow (self) | Allow (self) | Allow | `ApiSecurityTests`, `P96RbacFieldSecurityIdorTests` |
| Read Own Ticket (`GET /api/v1/tickets/{id}`) | Allow (owner) | Allow (owner/approvee) | Allow (own dept) | Allow (owner/HR) | Allow (read) | Allow | `TicketWorkflowTests`, `P96RbacFieldSecurityIdorTests` |
| Read Other User's Ticket (IDOR) | **DENY (`403`)** | **DENY (`403`)** | **DENY (other dept `403`)** | **DENY (`403`)** | Read-only | Allow | `P96RbacFieldSecurityIdorTests` (`P96-SEC-01`) |
| Department Queue (`GET /api/v1/departments/{d}/queue`) | **DENY (`403`)** | **DENY (`403`)** | **Own Dept Only (`200` / `403`)** | HR only | **DENY (`403`)** | Allow (all) | `ApiSecurityTests`, `P96RbacFieldSecurityIdorTests` (`P96-SEC-04`) |
| Approval Decision (`POST /api/v1/approvals/{id}/decide`) | **DENY (`403`)** | Allow (assigned; anti-self `400`) | **DENY (`403`)** | Allow (assigned) | **DENY (`403`)** | Allow (anti-self `400`) | `TicketWorkflowTests`, `ApiSecurityTests` |
| Internal Ticket Comments (`isInternal=true`) | **Hidden / Forced `false`** | Hidden if requester | Allow (own dept) | Allow (HR) | Read | Allow | `TicketWorkflowTests`, `P96RbacFieldSecurityIdorTests` (`P96-SEC-02`) |
| Notifications (`/api/v1/notifications/**`) | Own only (`403/404` other) | Own only | Own only | Own only | Own only | Own only | `NotificationTests`, `P96RbacFieldSecurityIdorTests` |
| Analytics (`/api/v1/analytics/**`) | **DENY (`403`)** | Allow | Allow (dept-scoped) | Allow | Allow (read) | Allow (all) | `AnalyticsTests`, `P96RbacFieldSecurityIdorTests` |
| Audit Chain (`/api/v1/audit/{logs,verify}`) | **DENY (`403`)** | **DENY (`403`)** | **DENY (`403`)** | **DENY (`403`)** | **Allow (read)** | **Allow (read)** | `ApiSecurityTests`, `P96RbacFieldSecurityIdorTests` (`P96-SEC-03`) |
| Self Profile (`GET /api/profiles/me`) | Allow (self) | Allow (self) | Allow (self) | Allow (self) | Allow (self) | Allow (self) | `P95UiUxTransformation`, Chrome E2E |
| Directory (`GET /api/profiles/directory`) | Redacted PII; no `sortBy=salary` | Redacted PII | Redacted PII | Full HR fields + salary sort | Redacted PII | Full HR fields + salary sort | `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` |
| HR Employee Upsert (`POST/PUT /api/profiles/employee`) | **DENY (`403`)** | **DENY (`403`)** | **DENY (`403`)** | **Allow** | **DENY (`403`)** | **Allow** | `P96ApiAndFieldValidationTests`, `P96RbacFieldSecurityIdorTests` |
| IAM User/Role/Risk/Dashboard Admin (`/api/users/**`, `/api/roles/**`, `/api/dashboard/metrics`) | **DENY (`403`)** | **DENY (`403`)** | **DENY (`403`)** | Users read only | Users/Roles read only (**mutations `403`**) | **Allow (all)** | `RbacAuthenticationTests`, `P96RbacFieldSecurityIdorTests` (`P96-SEC-03`) |
