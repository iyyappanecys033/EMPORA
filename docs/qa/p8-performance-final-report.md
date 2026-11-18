# EPAMS — P8 Final Performance & Scalability Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Performance Engineering Verification  
**Date:** 2026-11-18  
**Environment Disclaimer:** Local Windows workstation, OpenJDK 25.0.2, Spring Boot 3.4.12, H2 2.3.232 in-memory MySQL mode. Reproducible regression baseline only — **NOT a production MySQL 8.0+ capacity guarantee**.  

---

## 1. Reproducible Benchmark Results (1x P8 vs 4x P9 Scale)

| Endpoint | P8 1x Pre-Opt Avg / P95 | P9 4x Scale (480 tickets, 600 alerts, 100 approvals) Avg / P50 / P95 | P9 4x Throughput | Error Rate |
| :--- | :---: | :---: | :---: | :---: |
| `GET /api/v1/notifications/my?page=0&size=20` | 31.56 / 47.95 ms | **10.49 / 10.45 / 12.86 ms** | 95.7 req/s | 0.0% |
| `GET /api/v1/notifications/my/unread-count` | 28.87 / 39.59 ms | **9.44 / 9.35 / 12.82 ms** | 106.4 req/s | 0.0% |
| `GET /api/v1/analytics/overview` | 26.99 / 37.55 ms | **12.95 / 12.57 / 14.69 ms** | 77.2 req/s | 0.0% |
| `GET /api/v1/analytics/dashboard` | 62.68 / 114.15 ms | **14.78 / 14.73 / 16.79 ms** | 67.8 req/s | 0.0% |
| `GET /api/v1/departments/IT/queue?page=0&size=20` | 37.23 / 71.65 ms | **11.72 / 11.59 / 14.22 ms** | 85.5 req/s | 0.0% |

- **12-Thread Concurrent Mixed Load (120 requests):** Elapsed `314 ms`, Errors `0 / 120` (**0.0%**), Avg `28.02 ms`, P50 `26.19 ms`, P95 `47.14 ms`, Throughput **`382.2 req/s`**.
- **Query Optimizations Preserved:** `V4` composite indexes, single-pass `getDashboardBundle()` (~8 SQL queries vs ~25 pre-opt), `LEFT JOIN FETCH` eliminating notification ticket N+1, `React.lazy` chunks (`AnalyticsDashboard` `4.78 kB`, `AuditLedger` `2.40 kB` gzip).
- **Retained Phase 1 Debt (`TECH-P8-01`):** Legacy `DashboardController` (`GET /api/dashboard/metrics`, `ROLE_ADMIN` only) retains its Phase 1 per-user risk loop; isolated from operational paths.
- **Pending P10 Validation:** Live MySQL 8.0+ `EXPLAIN` index plans (`BLK-01`).
