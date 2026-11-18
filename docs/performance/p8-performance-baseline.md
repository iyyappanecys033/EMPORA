# EPAMS P8 — Performance Engineering Baseline Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Notifications, Performance & Analytics  
**Date:** 2026-11-17  
**Classification:** `[VERIFIED — LOCAL MOCKMVC/H2 BENCHMARK]`  

---

## 1. Test Environment & Conditions

> [!IMPORTANT]
> These measurements were captured in a repeatable local Spring Boot MockMvc + H2 (MySQL compatibility mode) benchmark harness (`PerformanceBenchmarkTests.java`). They establish a reproducible local regression baseline and **do not constitute production MySQL 8.0+ SLA guarantees**.

| Parameter | Specification |
| :--- | :--- |
| **Runtime** | OpenJDK 25.0.2 + Spring Boot 3.4.12 + Hibernate 6.6.x |
| **Database Engine** | In-memory H2 2.3.232 (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`) with Flyway V1–V4 |
| **Seeded Dataset** | 120 service tickets (across IT, HR, Facilities; 5 priorities; 7 active/terminal statuses), 150 in-app notifications (40 unread, 110 read), 30 approval tasks (15 pending, 15 approved), 3 service catalog offerings |
| **Warmup / Iterations** | 3 warmup requests + 25 timed requests per endpoint |
| **Concurrent Load Mix** | 8 worker threads × 10 requests = 80 concurrent mixed reads (`unread-count`, `notifications/my`, `analytics/overview`, `departments/IT/queue`) |

---

## 2. Single-Endpoint Baseline Latency & Throughput

Measured prior to `AnalyticsService.getDashboardBundle()` single-pass consolidation (`task-3283`, quiet host):

| Endpoint | Average (ms) | Median P50 (ms) | P95 (ms) | Throughput (req/s) | Error Rate |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `GET /api/v1/catalog?page=0&size=10` | 21.56 | 20.46 | 26.95 | 46.4 | 0.0% |
| `GET /api/v1/analytics/overview` | 26.99 | 26.54 | 37.55 | 37.1 | 0.0% |
| `GET /api/v1/notifications/my/unread-count` | 28.87 | 28.32 | 39.59 | 34.7 | 0.0% |
| `GET /api/v1/notifications/my?page=0&size=20` | 31.56 | 28.82 | 47.95 | 31.7 | 0.0% |
| `GET /api/v1/departments/IT/queue?page=0&size=10` | 37.23 | 31.22 | 71.65 | 26.9 | 0.0% |
| `GET /api/v1/analytics/dashboard` *(pre-consolidation)* | 62.68 | 56.22 | 114.15 | 16.0 | 0.0% |

---

## 3. Concurrent Load Baseline (8 Threads, 80 Requests)

- **Quiet Run (`task-3283`):** Elapsed `1604 ms`, Errors `0 / 80` (**0.0%**), Avg `151.42 ms`, P50 `134.38 ms`, P95 `271.91 ms`, Throughput `49.9 req/s`.
- **Full Clean-Build Run (`task-3340`):** Elapsed `2343 ms`, Errors `0 / 80` (**0.0%**), Avg `226.79 ms`, P50 `224.56 ms`, P95 `400.92 ms`, Throughput `34.1 req/s`.

---

## 4. Bottlenecks Identified in Baseline Inspection

1. **Dashboard Multi-Query Duplication (Backend):** Initial `AnalyticsService.getDashboardBundle()` delegated to 8 separate service methods, triggering 8 redundant `resolveAuthorizedDepartmentScope()` DB lookups (`user_role_mapping` + `profile`) and 4 duplicate `findAnalyticsProjectionInPeriod()` scans (~25 SQL statements per dashboard load).
2. **Notification Ticket Lazy N+1 Risk (Backend):** Standard JPA pagination over `InAppAlert` would execute 1 additional `SELECT` per row to read `ticket.ticketNumber` unless joined explicitly.
3. **Analytics Table Scan Risk on MySQL (Database):** `service_ticket` lacked composite indexes matching `(created_at, department, status)`, `(priority, department)`, and `(catalog_item_id, created_at)`.
4. **Monolithic Frontend JS Bundle (Frontend):** Eagerly importing `AuditLedger` and `AnalyticsDashboard` in `App.js` increased initial bundle download size for standard employees who never open operational dashboards.
5. **Legacy `DashboardController` N+1 (`GET /api/dashboard/metrics`):** Phase 1 admin metrics endpoint loads all users and invokes `assessRisk(user.getId())` in a loop (documented as retained Phase 1 technical debt outside P8 scope).

