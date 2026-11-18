# EPAMS P8 — Repository & Architecture Reconnaissance Baseline

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Notifications, Performance & Analytics  
**Date:** 2026-11-17  
**Status:** VERIFIED AGAINST LIVE SOURCE CODE `[VERIFIED]`  

---

## 1. Existing Technology Stack & Runtime Baseline

Verified directly from `springapp/pom.xml`, `springapp/springbackend/package.json`, and `src/test/resources/application.properties`:

| Layer | Technology / Version | Verification Source |
| :--- | :--- | :--- |
| **Java Runtime** | Java 25 LTS (`<java.version>25</java.version>`) | `springapp/pom.xml:23` |
| **Backend Framework** | Spring Boot 3.4.12 (Modular Monolith) | `springapp/pom.xml:12` |
| **ORM / Validation** | Spring Data JPA / Hibernate 6.6.x (`ddl-auto=validate`) | `application-mysql.properties:8`, `test/application.properties:13` |
| **Schema Migrations** | Flyway Core + Flyway MySQL (V1, V2, V3 applied) | `springapp/src/main/resources/db/migration/` |
| **Authentication** | Stateless JWT in HTTP-Only cookies (`accessToken`, `refreshToken`) | `AuthController.java`, `JwtFilter.java` |
| **CSRF Protection** | `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `X-XSRF-TOKEN` | `SecurityConfig.java:53`, `api.js:19-31` |
| **Application Clock** | Injectable `Clock.system(ZoneId.of("Asia/Kolkata"))` | `AppConfig.java:14` |
| **Frontend** | React 18.2.0, React Router 6.11.1, Axios 1.4.0 (CRA 5.0.1) | `springapp/springbackend/package.json` |
| **Charting Libraries** | None installed in `package.json` | `springapp/springbackend/package.json` |
| **Regression Baseline** | 152 backend tests (9 classes) + 21 frontend tests (1 suite) | Verified in P7 completion |

---

## 2. Existing Notification Architecture Assessment

Inspection of `model/InAppAlert.java`, `repository/InAppAlertRepository.java`, `notification/TicketNotificationEvent.java`, `notification/NotificationEventListener.java`, and `V3__approvals_and_audit_chain.sql`:

### What Already Exists
1. **Database Table (`in_app_alert` in V3):**
   - Columns: `id`, `recipient_id`, `ticket_id`, `alert_type`, `title`, `message`, `is_read`, `created_at`.
   - Foreign Keys: `fk_alert_recipient` (`ON DELETE CASCADE`), `fk_alert_ticket` (`ON DELETE SET NULL`).
   - Index: `idx_alert_recipient_read (recipient_id, is_read, created_at)`.
2. **Spring Application Events (`TicketNotificationEvent` & `NotificationEventListener`):**
   - Listener uses `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` combined with `@Transactional(propagation = Propagation.REQUIRES_NEW)` and exception isolation (`try/catch`), ensuring notifications never persist before commit and never roll back committed business transactions.
3. **Existing Event Publication Points:**
   - `TicketCommandService.createTicket` $\rightarrow$ `TICKET_SUBMITTED` (to requester)
   - `TicketCommandService.assignTicket` $\rightarrow$ `TICKET_ASSIGNED` (to assignee)
   - `TicketCommandService.updateTicketStatus` $\rightarrow$ `STATUS_CHANGED` (to requester)
   - `ApprovalService.createStage1Approval` $\rightarrow$ `APPROVAL_REQUIRED` (to stage 1 approver)
   - `ApprovalService.decideApproval` $\rightarrow$ `APPROVAL_REJECTED` (to requester), `APPROVAL_REQUIRED` (to stage 2 approver), `TICKET_APPROVED` (to requester)

### Gaps Identified for P8
1. **Missing REST API Layer:** No `NotificationController` or `NotificationService` exists to query notifications, fetch unread count, mark read, or mark all read.
2. **Missing Schema Metadata:** `in_app_alert` lacks `read_at` timestamp, `related_entity_type`, `related_entity_id`, and `dedup_key` for duplicate suppression.
3. **Missing Event Publications:** `TICKET_RESOLVED` (specific type), `TICKET_REOPENED` (to assignee), and `TICKET_CANCELLED` (to assignee if assigned) are not published.
4. **Structured Logging Gap:** `NotificationEventListener` uses `System.err.println` instead of SLF4J structured logging.
5. **Missing Frontend Notification Experience:** `Navbar.js` has no notification bell, unread badge, popover, or dedicated `/notifications` page.

---

## 3. Existing Analytics Capabilities Assessment

Inspection of `controller/DashboardController.java` and `dto/DashboardMetricsDTO.java`:
- Only a Phase 1 IAM risk metrics endpoint exists (`GET /api/dashboard/metrics`, restricted to `ROLE_ADMIN`), which loads all `UserAccount` rows into memory (`userAccountRepo.findAll()`) and runs an N+1 loop calling `riskAssessmentService.assessRisk(user.getId())`.
- **Zero Operational Service Analytics Exist:** No endpoints or aggregations exist for ticket volume, status/priority distribution, trends over time, department backlog/workload, service popularity, SLA compliance percentage, average resolution time, or approval pipeline overview.
- **Database Index Gaps for Analytics:** `service_ticket` has `(department, status)`, `(requester_id, created_at)`, `(status, sla_target_date)`, and `(status, resolved_at)`, but lacks composite indexes on `(created_at, department, status)`, `(priority, department)`, and `(catalog_item_id, created_at)` needed for date-bounded operational aggregations.

---

## 4. Existing Performance & Observability Characteristics

1. **HikariCP Configuration:** Configured in `application-mysql.properties` (`maximum-pool-size=10`, `minimum-idle=5`, `connection-timeout=20000`, `idle-timeout=300000`).
2. **Observability Gaps:**
   - No request correlation ID filter (`X-Correlation-Id` / SLF4J MDC).
   - No lightweight health endpoint (`/api/v1/health`) reporting database readiness.
   - No repeatable API latency / query performance benchmark suite.
3. **Frontend Bundle:**
   - Single monolithic JS bundle (`89.91 kB` gzip) because all routes including `AuditLedger` and future `AnalyticsDashboard` are eagerly imported in `App.js`.

---

## 5. P8 Implementation Plan Dependencies

1. **Flyway V4 Migration (`V4__notifications_and_analytics_indexes.sql`):** Strictly additive columns on `in_app_alert` + composite indexes on `in_app_alert`, `service_ticket`, and `approval_task`. Zero changes to V1–V3.
2. **Notification Backend & REST API:** Entity/Repo/Listener/Service/Controller + Security ownership isolation.
3. **Analytics Backend & REST API:** JPQL DB-side aggregation queries + `AnalyticsService` + `AnalyticsController` + Department scoping.
4. **Observability & Performance:** `CorrelationIdFilter`, `HealthController`, bounded `TaskExecutor`, and `PerformanceBenchmarkTests`.
5. **Frontend Notification & Analytics UI:** `NotificationBell`, `NotificationsPage`, lazy-loaded `AnalyticsDashboard` with accessible zero-bloat SVG/CSS charts.
6. **Testing & Documentation:** Full backend/frontend test suites + 12 required P8 documents.

