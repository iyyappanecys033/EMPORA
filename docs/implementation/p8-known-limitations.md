# EPAMS P8 — Known Limitations & Deferred Work

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Notifications, Performance & Analytics  
**Date:** 2026-11-17  

---

## Honest Classification of Unimplemented, Blocked, or Deferred Capabilities

| Item | Classification | Actual State & Technical Justification | Recommended Future Phase |
| :--- | :---: | :--- | :---: |
| **Live MySQL 8.0+ Migration & EXPLAIN Validation** | **BLOCKED (Environment)** | All 177 backend tests and V1–V4 Flyway migrations verified against H2 in MySQL compatibility mode (`MODE=MySQL`). No live MySQL 8.0 daemon/container is provisioned in this sandbox. | **P9 (Containerization & Staging CI/CD)** |
| **External Email / SMTP Notification Delivery** | **DEFERRED** | In-app notifications (`in_app_alert` + REST API + React Bell/Center) are 100% implemented and verified. Bounded `notificationTaskExecutor` is configured, but external SMTP delivery, email templates, and retry dead-letter queues require production SMTP infrastructure. | **P9 / Post-P8 Infrastructure** |
| **Scheduled SLA Warning / Overdue Scanner Job** | **DEFERRED** | Overdue tickets are calculated live in real time on ticket queries and analytics (`sla_target_date < NOW`), and `dedup_key` duplicate suppression is ready for `SLA_WARNING` / `TICKET_OVERDUE` alerts. A background `@Scheduled` cron scanner is deferred until multi-instance leader-locking / deployment topology is finalized in P9. | **P9 (Deployment & Operations)** |
| **Historical Point-in-Time Backlog Snapshots** | **DEFERRED (Data Model)** | `service_ticket` stores current status and lifecycle timestamps (`created_at`, `resolved_at`, `closed_at`), not a separate status-transition history table. Consequently, open backlog is accurately reported as a live snapshot, while submission/resolution trends are reported historically without fabricating past backlog counts. | **Post-P9 Data Warehouse / Reporting** |
| **Browser-Based Automated E2E Suite (Playwright/Cypress)** | **NOT TESTED (Tooling)** | 28 React Testing Library integration/component tests + 177 Spring Boot MockMvc integration tests verify full request/response flows, but no headless browser E2E runner is installed in `package.json`. | **P9 CI/CD Pipeline** |
| **Legacy Phase 1 Admin Metrics N+1 (`DashboardController`)** | **DEFERRED (Legacy Debt)** | Pre-existing Phase 1 `GET /api/dashboard/metrics` loops over `UserAccount` calling `assessRisk()`. Left untouched to avoid unauthorized modification of Phase 1 IAM risk logic. | **Post-P8 IAM Optimization** |

