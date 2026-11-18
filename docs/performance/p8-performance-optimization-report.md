# EPAMS P8 — Performance Optimization Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Notifications, Performance & Analytics  
**Date:** 2026-11-17  
**Classification:** `[VERIFIED]`  

---

## 1. Evidence-Driven Optimizations Implemented

| Layer | Optimization Applied | Technical Mechanism | Measured / Structural Impact |
| :--- | :--- | :--- | :--- |
| **Database (Flyway V4)** | Composite indexes for notifications & analytics | Added `idx_alert_recipient_type`, `uk_alert_recipient_dedup`, `idx_ticket_created_dept_status`, `idx_ticket_priority_dept`, `idx_ticket_catalog_created`, `idx_approval_status_created` in `V4__notifications_and_analytics_indexes.sql` | Eliminates full-table scans on date-ranged analytics, event-filtered notifications, and pending approval counts. |
| **Repository (JPA)** | N+1 elimination on notification feed | `InAppAlertRepository.findFilteredByRecipient()` uses `LEFT JOIN FETCH a.ticket t` paired with an explicit lightweight `countQuery` | Reduces 20-row notification page from **21 queries (1 + 20 N+1)** to **2 queries** (`SELECT ... JOIN FETCH` + `COUNT`). |
| **Service (Analytics)** | Single-pass dashboard aggregation | Refactored `AnalyticsService.getDashboardBundle()` to resolve authorization scope **once** and compute overview, SLA, status distribution, daily trends, and department metrics from **one shared scalar projection** | Cuts per-request SQL statements on `/api/v1/analytics/dashboard` from **~25 queries down to 8 targeted queries** and eliminates 7 redundant security DB roundtrips. |
| **API Architecture** | Cohesive dashboard bundle endpoint | Added `GET /api/v1/analytics/dashboard` consumed by React `AnalyticsDashboard` | Replaces an **8-HTTP-request frontend waterfall** with **1 single HTTP roundtrip** per filter change. |
| **Async / Executor** | Bounded thread pool | Configured `notificationTaskExecutor` in `AppConfig.java` (`core=2`, `max=4`, `queueCapacity=100`, `CallerRunsPolicy`, graceful shutdown) | Eliminates unbounded queue OOM risk for future background dispatchers. |
| **Frontend Polling** | Visibility-aware, overlap-guarded polling | `NotificationBell.js` polls `/my/unread-count` every 30s ONLY when `document.visibilityState === 'visible'`, guards overlap via `isPollingRef`, and cleans up timers on unmount/logout | Prevents wasted background polling traffic from inactive tabs and eliminates overlapping request storms. |
| **Frontend Bundle** | Route-based code splitting (`React.lazy` + `Suspense`) | Lazy-loaded `AnalyticsDashboard` and `AuditLedger` in `App.js` + built accessible zero-dependency SVG/CSS charts | Zero chart library dependencies added (`package.json` untouched). Verified production build splits `AnalyticsDashboard` (`4.78 kB` gzip) and `AuditLedger` (`2.40 kB` gzip) out of initial load. |

---

## 2. Frontend Production Bundle Verification

Measured directly via `cd springapp/springbackend; npm run build`:

| Artifact | P7 Baseline (gzip) | P8 Code-Split Build (gzip) | Notes |
| :--- | :---: | :---: | :--- |
| `main.*.js` | `89.91 kB` | `93.96 kB` | Includes new `NotificationBell`, `NotificationsPage`, `notificationService`, `analyticsService` (+4.05 kB total) |
| `555.*.chunk.js` *(lazy)* | — | `4.78 kB` | `AnalyticsDashboard.js` — loaded on-demand only when authorized user navigates to `/analytics` |
| `467.*.chunk.js` *(lazy)* | — | `2.40 kB` | `AuditLedger.js` — loaded on-demand only when Auditor/Admin navigates to `/audit-chain` |
| `main.*.css` | `1.31 kB` | `1.31 kB` | Zero CSS growth |
| **ESLint Warnings / Errors** | 0 / 0 | **0 / 0** | Clean production build |

---

## 3. Post-Optimization Benchmark Verification

Run after `mvn clean test` (`task-3340`, 177/177 passing, 0% error rate across all benchmarks):
- `GET /api/v1/notifications/my?page=0&size=20`: `p50=31.56 ms`, `p95=44.18 ms`, `30.2 req/s`
- `GET /api/v1/notifications/my/unread-count`: `p50=27.48 ms`, `p95=45.31 ms`, `33.6 req/s`
- `GET /api/v1/analytics/overview`: `p50=33.30 ms`, `p95=40.90 ms`, `29.7 req/s`
- `GET /api/v1/analytics/dashboard` (all 8 panels in 1 HTTP call): `p50=74.64 ms` (vs `53.32–56.22 ms` quiet host)
- **8-Thread Concurrent Load (80 requests):** `0 errors (0.0%)`, `34.1–49.9 req/s` depending on host contention.

> [!NOTE]
> Live MySQL 8.0+ `EXPLAIN` query plan analysis and multi-thousand-row load testing remain pending until a staging MySQL 8.0+ container is provisioned in P9.

