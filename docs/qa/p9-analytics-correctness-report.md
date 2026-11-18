# EPAMS P9 — Business Analytics Mathematical Correctness & Scoping Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  

---

## 1. Metric Formula & Correctness Audit

All 8 analytics endpoints under `/api/v1/analytics/**` and the composite `/api/v1/analytics/dashboard` bundle were independently audited against `AnalyticsService.java`, `ServiceTicketRepository.java`, `ApprovalTaskRepository.java`, `AnalyticsTests.java` (10 tests), `P9ScalePerformanceTests.java`, and `P8Features.test.js`.

| Metric / Endpoint | Formula & Boundary Definition | Edge-Case Handling Audited | P9 Status |
| :--- | :--- | :--- | :---: |
| **Period Total & Lifecycle Counts** (`/overview`) | Counts tickets with `createdAt` inside `[startDate 00:00:00, endDate 23:59:59.999999999]` (`Asia/Kolkata` business timezone). Open backlog & overdue counts reflect live current snapshots. | Default 30-day window when omitted; rejects `startDate > endDate` with `400 Bad Request`; enforces maximum range cap. | `VERIFIED` |
| **SLA Compliance Rate** (`/sla/compliance`) | $\text{compliance\%} = \frac{\text{metSlaCount}}{\text{evaluatedTickets}} \times 100$ rounded to 2 decimals. | Returns `null` (rendered as `N/A` in `AnalyticsDashboard.js`) when `evaluatedTickets == 0` — zero divide-by-zero / `NaN` / `Infinity`. | `VERIFIED` |
| **Average Resolution Hours** (`/sla/compliance`, `/overview`) | Mean elapsed hours (`Duration.between(createdAt, resolvedAt)`) across resolved/closed tickets in range. | Returns `null` (`N/A` in UI) when no resolved tickets exist in range. | `VERIFIED` |
| **5-Tier Priority Distribution** (`/tickets/by-priority`) | Aggregates all 5 approved enum tiers: `LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL`. | Verified `URGENT` tier is preserved end-to-end across backend DTOs and frontend `AnalyticsDashboard.js`. | `VERIFIED` |
| **Status Distribution & Daily Trends** (`/tickets/by-status`, `/tickets/trends`) | SQL `GROUP BY` status and daily date buckets (`submittedCount`, `resolvedCount`). | Zero N+1 queries; empty date ranges return empty arrays `[]` cleanly. | `VERIFIED` |
| **Department Performance & Service Popularity** (`/departments/performance`, `/services/popularity`) | Grouped by department and catalog item code/title. | Excludes all employee PII (no emails, salaries, phone numbers, or personal names in any analytics DTO). | `VERIFIED` |
| **Approval Bottlenecks** (`/approvals/overview`) | Pending snapshots (total, Stage 1, Stage 2, overdue) + approved/rejected counts in range. | Verified against multi-stage approval tickets in `AnalyticsTests` and 100-approval dataset in `P9ScalePerformanceTests`. | `VERIFIED` |

---

## 2. Role-Based Department Scoping Verification

| Persona | Requested `?department=` | Server-Side Enforced Scope | Verified Result |
| :--- | :--- | :--- | :---: |
| `EMPLOYEE` | Any | **Blocked (`403 Forbidden`)** at controller + frontend route guard | `VERIFIED` |
| `IT_SUPPORT` (Dept: `IT`) | None or `IT` | Scoped strictly to `IT` (`scopedDepartment="IT"`) | `VERIFIED` |
| `IT_SUPPORT` (Dept: `IT`) | `HR` or `Facilities` | **Blocked (`403 Forbidden`)** (cross-department escalation rejected) | `VERIFIED` |
| `MANAGER` / `HR_MANAGER` | Own department | Scoped to manager's own department | `VERIFIED` |
| `AUDITOR` / `ADMIN` | None or any department | Organization-wide (`scopedDepartment="ALL"`) or filtered to requested department | `VERIFIED` |

---

## 3. Scaled Analytics Performance (480 Tickets Across 4 Departments)

From `P9ScalePerformanceTests` (final clean run `task-3610`):
- `GET /api/v1/analytics/overview`: avg **`12.95 ms`**, p50 **`12.57 ms`**, p95 **`14.69 ms`**, throughput **`77.2 req/s`**.
- `GET /api/v1/analytics/dashboard` (full composite bundle): avg **`14.78 ms`**, p50 **`14.73 ms`**, p95 **`16.79 ms`**, throughput **`67.8 req/s`**.
- Under 12-thread concurrent mixed load (120 requests including `/analytics/dashboard`): **0 errors**, overall p95 **`47.14 ms`**, **`382.2 req/s`**.

