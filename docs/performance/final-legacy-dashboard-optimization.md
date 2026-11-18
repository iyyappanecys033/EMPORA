# EPAMS — Final Legacy Dashboard N+1 Optimization Report (`TECH-P8-01`)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Finding ID:** `TECH-P8-01`  
**Date:** 2026-11-18  
**Status:** **`FIXED & VERIFIED`**  

---

## 1. Consumer & Necessity Audit

- **Endpoint:** `GET /api/dashboard/metrics` (`DashboardController.java`)
- **Frontend Consumer:** `springapp/springbackend/src/components/Dashboard.js` (mounted at `/admin/metrics` in `App.js`, protected by `<ProtectedRoute condition={isAdmin}>`).
- **Authorization:** `@PreAuthorize("hasRole('ADMIN')")` (verified `403 Forbidden` for non-admin callers).
- **Overlap Assessment with `AnalyticsController`:** Does **not** duplicate P8 operational ticket analytics (`/api/v1/analytics/**`). `DashboardController` reports IAM identity risk counts (`totalUsers`, `activeUsers`, `suspendedUsers`, `disabledUsers`, `lockedUsers`, `highRiskUsers`, `criticalRiskUsers`, `totalAuditEvents`), so retaining and optimizing it preserves administrative IAM observability without competing dashboard endpoints.

---

## 2. Root Cause & Optimization Applied

- **Before (`3N + 2` SQL Queries):** Loaded `userAccountRepo.findAll()` and invoked `riskAssessmentService.assessRisk(user.getId())` per user, executing `userAccountRepo.findById(id)` + `userRoleMappingRepo.findByUserId(id)` + lazy `Role` fetches inside the loop ($> 60$ SQL statements for 20 users; $> 3{,}000$ for 1,000 users).
- **After (Constant `3` SQL Queries — $O(1)$ DB Roundtrips):**
  1. `UserRoleMappingRepo.findAllUserIdAndRoleNames()` executes a single scalar JPQL projection `SELECT m.user.id, r.roleName FROM UserRoleMapping m JOIN m.role r` (zero entity graph hydration or lazy proxies).
  2. Extracted pure reusable `RiskAssessmentService.assessRisk(UserAccount, Collection<String>, LocalDateTime)` shared by both single-user `assessRisk(Long)` and bulk `DashboardController.getDashboardMetrics()` (zero scoring logic duplication).
  3. `DashboardController.getDashboardMetrics()` executes exactly 3 constant queries: `userAccountRepo.findAll()`, `userRoleMappingRepo.findAllUserIdAndRoleNames()`, and `auditLogRepo.count()`.

---

## 3. Automated Regression & Query-Count Verification

Verified in `Stage1RemediationVerificationTests.test1_LegacyDashboardEliminatesNPlusOneWithConstantQueries`:
- Seeded 20+ diverse users (active, suspended, disabled, locked, admin/manager/employee roles).
- Confirmed `ROLE_EMPLOYEE` receives `403 Forbidden`.
- Measured SQL statements via Hibernate `SessionFactory.getStatistics().getPrepareStatementCount()`: total SQL statements (including JWT authentication filter lookup + all dashboard aggregation) remained $\le 10$ (constant $O(1)$ regardless of user count, eliminating the $3N+2$ N+1 pattern) with exact risk-count accuracy.
