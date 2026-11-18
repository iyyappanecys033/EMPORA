# EPAMS — P9.6 Application Feature Inventory

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage A: Complete Application Feature Inventory
**Baseline Commit:** `a4857679c0bde310e4b705a46d7e7ef701bc3901`

---

## 1. Backend Modules & Functional Capabilities (`126` Java Classes)

| Module | Source Classes | Entities / Enums | Primary Capabilities |
| :--- | :---: | :--- | :--- |
| **Authentication & Session IAM** | `AuthController`, `AuthService`, `JwtFilter`, `JwtService`, `RefreshTokenService`, `CustomUserDetailsService`, `SecurityConfig` | `UserAccount`, `RefreshToken`, `AccountStatus` | Stateless JWT (`HttpOnly` cookie + Bearer fallback), SHA-256 hashed rotating refresh tokens with reuse detection, 5-attempt / 15-min lockout, logout revocation, SPA CSRF double-submit (`XSRF-TOKEN` / `X-XSRF-TOKEN`) |
| **User Account Lifecycle & Risk** | `UserAccountController`, `UserAccountServiceImpl`, `AccountStatusLogController`, `AccountStatusLogServiceImpl`, `RiskAssessmentService`, `DashboardController` | `UserAccount`, `AccountStatusLog` | CRUD, suspend/disable/reactivate/unlock, status audit trail, heuristic account risk scoring, O(1)-query admin IAM dashboard |
| **Roles & RBAC Mappings** | `RoleController`, `RoleServiceImpl`, `UserRoleMappingController`, `UserRoleMappingServiceImpl`, `TicketSecurityEvaluator` | `Role`, `UserRoleMapping` | Role catalog CRUD & pagination, user-role assignments, 11-role RBAC enforcement, department & ownership evaluators |
| **Employee Profiles & Directory** | `ProfileController`, `ProfileServiceImpl` | `Profile` | Self-service `GET /api/profiles/me`, paginated searchable directory with non-HR/Admin PII redaction (`salary`, `dob`, `address`, `emergencyContact`), HR/Admin employee upsert |
| **Service Catalog** | `ServiceCatalogController`, `ServiceCatalogRepository` | `ServiceCatalogItem` | Active service listing, department/category filtering, lookup by ID or item code, SLA target hours, 1-level & 2-level approval policy flags |
| **11-State Service Ticketing** | `TicketController`, `TicketCommandService`, `TicketQueryService`, `DepartmentQueueController`, `TicketStateMachine` | `ServiceTicket`, `TicketComment`, `TicketStatus` (11 states), `TicketPriority` (5 tiers) | Ticket creation, requester-scoped listing, department-scoped resolver queue, assignment, deterministic 11-state transitions, mandatory ≥15-char reasons, WIP cancel restriction, 48-business-hour dispute reopen, public vs. internal comments, `@Version` optimistic locking |
| **Multi-Stage Approval Engine** | `ApprovalController`, `ApprovalService` | `ApprovalTask`, `ApprovalStatus` | Single-level Manager & two-level Department Head escalation, strict anti-self-approval, mandatory ≥15-char rejection/approval rationale, pending approver queue |
| **SLA & Business Calendar** | `BusinessCalendar`, `SlaScheduledProcessor`, `SchedulingConfig` | — | `Asia/Kolkata` timezone Mon–Fri `09:00–18:00`, externalized multi-year holidays, ON_HOLD SLA pause/resume tracking, 48-business-hour reopen window, idempotent hourly auto-closure & SLA breach scanner |
| **Cryptographic Audit Chain** | `AuditApiController`, `AuditLogController`, `AuditChainService`, `AuditLogServiceImpl` | `AuditLog`, `AuditChainHead` | Monotonic sequence numbering with pessimistic row lock (`AuditChainHead`), canonical SHA-256 parent-hash chaining (`REQUIRES_NEW`), full tamper-detection verification API |
| **Notifications & Email** | `NotificationController`, `NotificationService`, `NotificationEventListener`, `EmailDeliveryAdapter`, `SmtpEmailDeliveryAdapter`, `TicketNotificationEvent` | `InAppAlert` | Post-commit (`AFTER_COMMIT`) persistence, unique deduplication keys, unread counter, mark read / mark all read, ownership checks, SMTP header-sanitized async delivery with failure isolation |
| **Operational Analytics** | `AnalyticsController`, `AnalyticsService` | 9 Analytics DTOs | Dashboard bundle, overview KPIs, status & 5-tier priority distributions, daily trends, department workload, service popularity, business-calendar SLA compliance, approval pipeline |
| **Platform Health & Docs** | `HealthController`, `ApiDocsController`, `SpaController`, `CorrelationIdFilter`, `CorsConfig`, `GlobalExceptionHandler` | — | `/api/v1/health`, `/api/docs`, request correlation ID (`X-Correlation-ID`), CORS, RFC 7807 Problem Details |

---

## 2. Frontend Routes & Workspaces (`26` Declared Routes in `App.js`)

| Route | Component | Access Guard | Interactive Capabilities |
| :--- | :--- | :--- | :--- |
| `/` | `EmployeeDashboard` | Public landing / Auth workspace | Guest intro + Login trigger; authenticated identity hero, KPIs, Recent Tickets, Featured Services, Notifications Preview |
| `/catalog` | `ServiceCatalog` | Public / Auth | Search, department dropdown, derived category pills, pagination, `CreateTicketModal` launch |
| `/tickets` | `MyTickets` | Authenticated | Status/priority/sort filters, Table ↔ Card toggle, pagination, new request modal |
| `/tickets/:id` | `TicketDetail` + `TicketComments` | Authenticated (owner/dept/admin) | 11-state `StatusTimeline`, SLA overdue badge, state transition/cancel/reopen actions, public & internal comments |
| `/notifications` | `NotificationsPage` | Authenticated | Unread filter, event-type filter, mark read, mark all read, ticket/approval deep links |
| `/profile` | `EmployeeProfile` | Authenticated | `GET /api/profiles/me`, monogram avatar, read-only RBAC notice (HR/Admin edit link) |
| `/preferences` | `PreferencesPage` | Authenticated | Theme (`Light`/`Dark`/`System`), Density (`Comfortable`/`Compact`), Font Scale (`Default`/`Large`), Reduced Motion, Reset |
| `/analytics` | `AnalyticsDashboard` (lazy) | `canViewAnalytics` | 7D/30D/90D + custom dates, department/priority filters, KPIs, trend chart, SLA ring, distributions, workload table |
| `/approvals` | `ApprovalQueue` | `isApprover` | Pending Stage 1 & Stage 2 tasks, approve/reject modal with ≥15-char reasoning |
| `/queue`, `/queue/:department` | `DepartmentQueue` | `isResolver` | Department triage queue, search/status/priority filters, age & SLA overdue indicators, self-assign |
| `/audit-chain` | `AuditLedger` (lazy) | `isAuditor` | SHA-256 chain verification banner, page search filter, monotonic sequence/hash table |
| `/admin/metrics` | `Dashboard` | `ROLE_ADMIN` | Legacy IAM risk & account status overview |
| `/users`, `/users/new`, `/users/:id/edit` | `UserList`, `UserForm` | `ROLE_ADMIN` | User CRUD, suspend/disable/reactivate/unlock |
| `/roles`, `/roles/new`, `/roles/:id/edit` | `RoleList`, `RoleForm` | `ROLE_ADMIN` | Role CRUD & pagination |
| `/profiles`, `/profiles/new`, `/profiles/:id/edit` | `ProfileList`, `ProfileForm` | `ROLE_ADMIN` / `ROLE_HR_MANAGER` | Employee directory & HR profile management |
| `/user-role-mappings`, `/user-role-mappings/new` | `UserRoleMapping`, `UserRoleMappingForm` | `ROLE_ADMIN` | User-role mapping management |
| `/audit` | `AuditLogs` | `ROLE_ADMIN` | Legacy IAM audit log viewer |
| `*` | Redirect to `/` | All | Catch-all fallback |

---

## 3. Untested / Under-Tested Capabilities Targeted in P9.6

1. Pure deterministic unit tests for `TicketStateMachine` exhaustive invalid-matrix (`11 × 11` pairs), `ApprovalService` edge cases, `AnalyticsService` empty/zero-denominator boundaries, and `NotificationService` deduplication & ownership.
2. Exhaustive API & field-by-field validation across all `44` Request DTO fields + query/path parameters (`null`, empty, whitespace, boundary lengths, invalid types/enums, Unicode/Tamil/emoji, XSS, SQLi, malformed JSON, `415` Content-Type, mass assignment).
3. Complete 11-role RBAC & field-level access matrix (`salary` redaction, `passwordHash` non-disclosure, internal comments hiding, `sortBy` salary oracle defense) + IDOR/BOLA negative suite.
4. Transaction rollback / `AFTER_COMMIT` / `REQUIRES_NEW` audit preservation + concurrent optimistic locking + concurrent audit chain append + scheduled SLA job idempotency & failure isolation.
5. Full 12-journey browser E2E, 6-viewport responsive matrix (`1440×900` down to `320×700`), live `axe-core` across all interactive states, MySQL 8 `EXPLAIN`/concurrency/backup-restore, HTTP load/stress benchmarks, and CI quality gates.
