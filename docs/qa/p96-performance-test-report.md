# EPAMS — P9.6 Performance & Scalability Benchmark Report (Stage L)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification (Stage L)  
**Execution Date:** 2026-11-18  
**Target Under Test:** Packaged Executable JAR (`springapp/target/springapp-0.0.1-SNAPSHOT.jar`, Spring Boot `3.5.16`, Java `25`, Embedded Tomcat, HikariCP)  
**Hardware / Environment:** Local Windows workstation (`127.0.0.1:18096`), single JVM instance, zero external CDN/cache.  

> **Honesty Boundary:** Benchmarks below represent **single-node local HTTP concurrency benchmarks** (780 authenticated HTTP requests across 8 workload profiles) plus **real MySQL `8.0.45` query execution plans on 1,000+ synthetic tickets**. They validate absence of severe latency regressions, thread-pool starvation, and N+1 bottlenecks at moderate concurrency (`c=1` to `c=40`), but are **not** a distributed multi-hour production soak test or cloud load-balancer capacity certification.

---

## 1. Live HTTP Load, Stress & Spike Benchmark Results

Executed via automated Node.js HTTP concurrency harness against the live packaged JAR (`scratch/p96-live-results.json`, `2026-11-18T07:26:12Z` – `07:27:02Z`):

| Benchmark Profile | Endpoint | Concurrency (`c`) | Requests (`n`) | Throughput (req/s) | Avg (ms) | p50 (ms) | p95 (ms) | p99 (ms) | Errors | Error Rate | Threshold (p95 < 500ms, 0% err) |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Baseline Health** | `GET /api/v1/health` | `1` | `30` | `78.3` | `12.75` | `14.96` | `17.26` | `19.94` | `0` | `0.00%` | **PASS** |
| **Load — Service Catalog** | `GET /api/v1/catalog` | `10` | `100` | `407.6` | `23.74` | `20.90` | `41.60` | `48.44` | `0` | `0.00%` | **PASS** |
| **Load — My Tickets** | `GET /api/v1/tickets/my` | `10` | `100` | `355.3` | `26.83` | `26.23` | `33.42` | `37.92` | `0` | `0.00%` | **PASS** |
| **Load — Admin Metrics** | `GET /api/admin/metrics` | `10` | `80` | `276.3` | `35.44` | `27.20` | `95.99` | `99.79` | `0` | `0.00%` | **PASS** |
| **Load — Executive Analytics** | `GET /api/v1/analytics/dashboard` | `10` | `60` | `231.2` | `40.79` | `37.78` | `60.07` | `86.07` | `0` | `0.00%` | **PASS** |
| **Load — Audit Chain Verify** | `GET /api/v1/audit/verify` | `10` | `60` | `381.3` | `24.91` | `23.73` | `37.00` | `42.54` | `0` | `0.00%` | **PASS** |
| **Stress — Ticket Listing** | `GET /api/v1/tickets/my` | `25` | `150` | `413.7` | `57.33` | `54.06` | `88.43` | `113.63` | `0` | `0.00%` | **PASS** |
| **Spike — Catalog Burst** | `GET /api/v1/catalog` | `40` | `200` | `558.6` | `68.40` | `66.04` | `98.18` | `119.50` | `0` | `0.00%` | **PASS** |
| **Aggregate Total** | **6 distinct API routes** | **1–40** | **`780`** | **Up to `558.6`** | — | **15–66ms** | **17–98ms** | **20–120ms** | **`0`** | **`0.00%`** | **PASS** |

---

## 2. Database-Tier Performance & Query Plan Observations (Real MySQL `8.0.45`)

- **1,000+ Synthetic Tickets + 400 Approvals + 500 Alerts (`Stage2RealMysqlIntegrationTests.test7`):**
  - Full `AnalyticsService.getDashboardBundle(...)` (aggregating KPI overview, SLA compliance, department performance, service popularity, and approval metrics across `1,003` tickets on real MySQL `8.0.45`) executed in **`422 ms`** (`slaRate=33.23%`), with individual SQL queries executing in **`0.35 ms` – `6.08 ms`** under `EXPLAIN ANALYZE`.
  - **Index Usage Findings:**
    - `in_app_alert` unread inbox query (`WHERE recipient_id = ? AND is_read = 0 ORDER BY created_at DESC LIMIT 20`) uses `idx_alert_recipient_read` (`ref`, backward index scan, **`0.349 ms`**, zero filesort).
    - `service_ticket` department/status filters and backlog aggregation (`Q4`) use `idx_ticket_dept_status` (`ref` / covering index scan, **`1.69 ms`**).
    - `service_ticket` popularity join (`Q6`) uses covering index `idx_ticket_catalog_created` (**`6.08 ms`**).
    - `approval_task` approver/status filters use `idx_approval_approver_status` (`ref`, `const,const`).
  - **Documented Future Optimization Note (Non-Blocking):** `Q2_AnalyticsProjection_LowerDeptFilter` uses `LOWER(t.department) = LOWER('IT')`, which prevents direct B-Tree index use on `department` when the dataset scales beyond `50,000+` rows (MySQL `utf8mb4_0900_ai_ci` is already case-insensitive, so removing `LOWER()` in a future tuning pass will unlock full index range scans without schema changes).

---

## 3. Frontend Production Bundle & Browser Rendering Performance

- **React Production Build (`CI=true npm run build`):** `Compiled successfully` — zero ESLint errors, zero build warnings.
  - Main JS chunk: **`107.06 kB`** gzipped (`+4.81 kB` / `2.72 kB` lazy chunks); Main CSS chunk: **`5.89 kB`** gzipped.
- **Live Chrome `157.0.8103.79` Navigation Checks:**
  - All `11` standalone JAR SPA routes (`/`, `/catalog`, `/tickets`, `/approvals`, `/queue/IT`, `/audit-chain`, `/notifications`, `/analytics`, `/profile`, `/preferences`, `/admin/metrics`) served HTTP `200` directly from Spring Boot static resources with `0` console errors and `0` unhandled exceptions.
- **Cold Restart Time (`p96-live-results.json`):** Standalone packaged JAR booted from process start to healthy `GET /api/v1/health == UP` in **`9.82 s`**.

---

## 4. Scalability Bottlenecks & P10 Capacity Recommendations

1. **Audit Chain Serialization Point:** `AuditChainService` intentionally acquires a pessimistic `FOR UPDATE` row lock on `audit_chain_head (id = 1)` to guarantee strict monotonic cryptographic sequencing. Tested cleanly under `8`–`10` concurrent writers (`0` deadlocks, `0` gaps). Above ~200+ audit writes/sec at enterprise scale, evaluate batching non-critical read telemetry outside the hash chain while keeping security/state mutations strictly chained.
2. **HikariCP Pool Sizing (`application-mysql.properties`):** Currently configured at `maximum-pool-size=10`, `minimum-idle=5`, `connection-timeout=20000ms` — appropriate for single-node SME load; tune per CPU core count and MySQL `max_connections` in P10.
3. **Long-Duration Soak Test (`NOT EXECUTED`):** Execute a 2–4 hour staging soak test (e.g., k6 or JMeter) during P10 staging validation to verify JVM heap stability and GC pause profiles under sustained traffic.
