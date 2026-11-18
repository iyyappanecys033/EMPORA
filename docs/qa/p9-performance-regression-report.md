# EPAMS P9 — Performance Regression & 4x Scale Verification Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  
**Classification:** `VERIFIED` (Local H2/MockMvc & Production Bundle); Live MySQL 8.0+ Query Plans `BLOCKED`  

---

## 1. Benchmark Comparison: P8 Baseline (1x) vs P9 Scaled Suite (4x)

| Parameter | P8 Baseline (`PerformanceBenchmarkTests`) | P9 Scaled Suite (`P9ScalePerformanceTests`) | Scaling Factor |
| :--- | :---: | :---: | :---: |
| **Service Tickets** | 120 (3 departments) | **480** (4 departments) | **4.0x** |
| **In-App Alerts** | 150 | **600** | **4.0x** |
| **Approval Tasks** | 30 | **100** | **3.3x** |
| **Concurrent Load** | 8 threads × 10 reqs = 80 reqs | **12 threads × 10 reqs = 120 reqs** | **1.5x** |

### Single-Endpoint Latency Comparison (Warm Runs, Final Clean Run `task-3610`)

| Endpoint | P8 Pre-Opt Avg / P95 (1x) | P9 4x Scale Avg (ms) | P9 4x Scale P50 (ms) | P9 4x Scale P95 (ms) | P9 4x Throughput | Regression Verdict |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `GET /api/v1/notifications/my?page=0&size=20` | 31.56 / 47.95 ms | **10.49 ms** | 10.45 ms | **12.86 ms** | 95.7 req/s | **NO REGRESSION** |
| `GET /api/v1/notifications/my/unread-count` | 28.87 / 39.59 ms | **9.44 ms** | 9.35 ms | **12.82 ms** | 106.4 req/s | **NO REGRESSION** |
| `GET /api/v1/analytics/overview` | 26.99 / 37.55 ms | **12.95 ms** | 12.57 ms | **14.69 ms** | 77.2 req/s | **NO REGRESSION** |
| `GET /api/v1/analytics/dashboard` | 62.68 / 114.15 ms | **14.78 ms** | 14.73 ms | **16.79 ms** | 67.8 req/s | **NO REGRESSION** |
| `GET /api/v1/departments/IT/queue?page=0&size=20` | 37.23 / 71.65 ms | **11.72 ms** | 11.59 ms | **14.22 ms** | 85.5 req/s | **NO REGRESSION** |

### Concurrent Load Comparison

- **P8 Baseline (8 threads, 80 requests):** Avg `151.42 ms`, P95 `271.91 ms`, Throughput `49.9 req/s`, Errors `0 / 80`.
- **P9 4x Scaled Load (12 threads, 120 mixed requests, `task-3610`):** Elapsed **`314 ms`**, Avg **`28.02 ms`**, P50 **`26.19 ms`**, P95 **`47.14 ms`**, Throughput **`382.2 req/s`**, Errors **`0 / 120`** (**0.0%**), Used JVM Heap **`180 MB`**.

---

## 2. Frontend Production Bundle Regression Check

Executed `npm run build` inside `springapp/springbackend/` (`task-3487`, exit code `0`, 0 errors, 0 ESLint warnings):

| Bundle Asset (Gzipped) | P8 Baseline Size | P9 Verified Size | Delta |
| :--- | :---: | :---: | :---: |
| `build/static/js/main.*.js` | `93.96 kB` | **`93.96 kB`** | `0.00 kB` |
| `build/static/js/555.*.chunk.js` (`AnalyticsDashboard`) | `4.78 kB` | **`4.78 kB`** | `0.00 kB` |
| `build/static/js/467.*.chunk.js` (`AuditLedger`) | `2.40 kB` | **`2.40 kB`** | `0.00 kB` |
| `build/static/css/main.*.css` | `1.31 kB` | **`1.31 kB`** | `0.00 kB` |

Lazy code-splitting for heavy operational workspaces (`AnalyticsDashboard`, `AuditLedger`) remains 100% intact.

---

## 3. Performance Boundaries & Retained Debt

- **Local H2 vs Production MySQL 8.0+:** All latency metrics above reflect in-memory H2 + MockMvc on a single Windows workstation. Real MySQL 8.0+ disk I/O, InnoDB buffer pool sizing, and `EXPLAIN` execution plans are `BLOCKED` in this sandbox and must be measured in P10 staging.
- **Retained Phase 1 Legacy Debt:** `DashboardController` (`GET /api/dashboard/metrics`) retains its Phase 1 N+1 user risk loop (`assessRisk(user.getId())` per user). It is restricted strictly to `ROLE_ADMIN` and does not affect operational ticket, notification, or P8 analytics paths.

