# EPAMS — P8 Final Analytics Correctness Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Operational Business Analytics Validation  
**Date:** 2026-11-18  

---

## 1. Deterministic Metric Validation

Validated across `AnalyticsTests` (8 tests), `P9LiveServerE2EIntegrationTests`, `P7P9ConsolidatedAcceptanceTests` (Scenarios 1, 2, 6), and `P8Features.test.js`:

| Endpoint / Metric | Verified Formula & Edge-Case Behavior | Status |
| :--- | :--- | :---: |
| `/overview` & `/dashboard` | Single-pass `getDashboardBundle()` (~8 SQL queries); period counts anchored to `Asia/Kolkata` date bounds; rejects `startDate > endDate` (`400`). | `VERIFIED` |
| **SLA Compliance Rate** (`/sla/compliance`) | `ROUND(metSlaCount * 100.0 / evaluatedTickets, 2)`; excludes `CANCELLED` & `REJECTED`; subtracts `totalPausedMinutes` (`ON_HOLD`); returns `null` (`N/A` in UI) when denominator is 0. | `VERIFIED` |
| **Resolved vs Closed & Reopened** | Reopened tickets currently in `IN_PROGRESS` count once in open backlog snapshot and are excluded from period resolved/closed totals (no double-counting). | `VERIFIED` |
| **5 Priority Tiers** (`/tickets/priority-distribution`) | Covers `LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL` end-to-end. | `VERIFIED` |
| **Approval Overview** (`/approvals/overview`) | Pending Stage 1/Stage 2 snapshots + period approved/rejected counts verified against real approval mutations. | `VERIFIED` |
| **Role & Department Scoping** | `EMPLOYEE` blocked (`403`); Specialists/Managers locked to own department (cross-dept `?department=` rejected `403`); `ADMIN`/`AUDITOR` org-wide. Zero employee PII in any analytics DTO. | `VERIFIED` |

---

## 2. Explicit Historical Limitation Disclosure

Open-backlog and pending-approval KPIs are explicitly labeled as **current point-in-time snapshots** (`openTicketsSnapshot`, `pendingApprovalsSnapshot`) because no historical status-snapshot table exists in V1–V4. Historical values are never fabricated from current state.
