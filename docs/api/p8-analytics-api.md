# EPAMS P8 — Business Analytics REST API Specification

**Base Path:** `/api/v1/analytics`  
**Controller:** `com.examly.springapp.controller.AnalyticsController`  
**Authentication:** Mandatory HTTP-Only JWT Cookie (`accessToken`)  
**Authorization:** `ADMIN` / `AUDITOR` (organization-wide); `MANAGER` / `HR_MANAGER` / `DEPT_HEAD` / `RESOLVER` / `IT_SUPPORT` / `HR_SPECIALIST` / `FACILITIES_CREW` (strictly locked to own profile department); standard `EMPLOYEE` receives `403 Forbidden`.

---

## Common Query Parameters

| Parameter | Type | Default / Validation |
| :--- | :--- | :--- |
| `startDate` | ISO Date (`YYYY-MM-DD`) | Defaults to `endDate - 29 days` (`Asia/Kolkata`). Must be $\le$ `endDate`. Max span 366 days. |
| `endDate` | ISO Date (`YYYY-MM-DD`) | Defaults to today (`Asia/Kolkata`). |
| `department` | String | Optional for `ADMIN`/`AUDITOR`. Must match own profile department (or be omitted) for Managers/Specialists; cross-department requests return `403 Forbidden`. |
| `priority` | Enum (`LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL`) | Optional filter on supported endpoints. |

---

## Endpoints Inventory

| Method & Path | Response DTO | Description |
| :--- | :--- | :--- |
| `GET /api/v1/analytics/dashboard` | `AnalyticsDashboardBundleDTO` | Cohesive single-roundtrip bundle returning overview, status/priority distributions, daily trends, department performance, service popularity, SLA compliance, and approval overview. |
| `GET /api/v1/analytics/overview` | `AnalyticsOverviewDTO` | Summary KPIs: `periodTotalTickets`, `openTicketsSnapshot`, `resolvedTicketsInPeriod`, `closedTicketsInPeriod`, `cancelledOrRejectedInPeriod`, `overdueOpenTicketsSnapshot`, `slaComplianceRate`, `averageResolutionHours`, `pendingApprovalsSnapshot`, `scopedDepartment`, `startDate`, `endDate`, `timezone`. |
| `GET /api/v1/analytics/tickets/status-distribution` | `List<StatusDistributionDTO>` | Count & percentage per lifecycle status in period, sorted descending by count. |
| `GET /api/v1/analytics/tickets/priority-distribution` | `List<PriorityDistributionDTO>` | Count & percentage across all 5 priorities (`LOW`, `MEDIUM`, `HIGH`, `URGENT`, `CRITICAL`). Supports optional `status` filter. |
| `GET /api/v1/analytics/tickets/trends` | `List<TicketTrendPointDTO>` | Zero-filled daily series `{ date, submittedCount, resolvedCount }` from `startDate` through `endDate`. |
| `GET /api/v1/analytics/departments/performance` | `List<DepartmentPerformanceDTO>` | Per-department `{ department, totalSubmittedInPeriod, openBacklogSnapshot, resolvedInPeriod, overdueOpenSnapshot, slaComplianceRate }`, sorted by highest open backlog. |
| `GET /api/v1/analytics/services/popularity` | `List<ServicePopularityDTO>` | Top requested service catalog items (`limit` bounded `1..25`, optional `category` filter). |
| `GET /api/v1/analytics/sla/compliance` | `SlaComplianceDTO` | `{ evaluatedTickets, metSlaCount, breachedSlaCount, activeOverdueCount, compliancePercentage, averageResolutionHours }`. |
| `GET /api/v1/analytics/approvals/overview` | `ApprovalOverviewDTO` | `{ pendingSnapshot, stage1PendingSnapshot, stage2PendingSnapshot, overduePendingSnapshot, approvedInPeriod, rejectedInPeriod }`. |

---

## Error Responses (RFC 7807 Problem Details)

- `400 Bad Request` — `startDate > endDate`, range exceeds 366 days, invalid date format, or invalid enum value.
- `401 Unauthorized` — Missing or expired JWT cookie.
- `403 Forbidden` — Standard `ROLE_EMPLOYEE` caller, missing department profile, or cross-department parameter spoofing.

