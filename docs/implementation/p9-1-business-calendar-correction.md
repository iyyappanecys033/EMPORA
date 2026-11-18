# EPAMS — P9.1 Business Calendar Reopen Window Correction Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Task:** P9.1 — Business Calendar Reopen Window Correction (`FUNC-P9-01` Remediation)  
**Date:** 2026-11-18  
**Verdict:** **`APPROVED`**  

---

## 1. Root Cause Analysis

During P9 source inspection (`FUNC-P9-01`), three aligned defects were identified in the ticket dispute/reopen window enforcement (`BR-003` / `FR-012` / `HLD.md` §5.2–5.3 / `ADR-007`):

1. **Wall-Clock Bypass in `TicketStateMachine.java:80`:** `TicketStateMachine` did not inject `BusinessCalendar` and evaluated `ticket.getResolvedAt().plusHours(48).isBefore(now)` (48 wall-clock hours instead of 48 accumulated business hours) while using `Clock.systemDefaultZone()` in its fallback constructor. This originated from an early code snippet in `docs/architecture/LLD.md:271`.
2. **Wall-Clock Bypass in `BusinessCalendar.java:153-163`:** Even though `BusinessCalendar` already implemented `calculateTargetDate(LocalDateTime, int)` over `Asia/Kolkata` (Mon–Fri, 09:00–18:00, excluding weekends and configured corporate holidays), its helper methods `isDisputeWindowExpired(LocalDateTime)` and `isAutoClosureEligible(LocalDateTime)` mistakenly used `resolvedAt.plusHours(48).isBefore(now)`.
3. **Sub-Minute Truncation in `BusinessCalendar.calculateTargetDate(...)`:** `Duration.between(currentTime, WORK_END).toMinutes()` truncated fractional minutes, causing timestamps near end-of-day (e.g., `17:59:30`) to drop seconds when accumulating multi-day business hours.
4. **Outdated Regression Test (`TicketWorkflowTests.java:286`):** `test5_ReopenWithinDisputeWindow()` asserted that `LocalDateTime.now().minusHours(50)` was expired, codifying the wall-clock bug rather than the approved 48-business-hour business requirement.

---

## 2. Exact Business Calendar Algorithm & Inclusive Deadline Rule

No duplicate calendar implementation was created. All reopen-window and auto-closure evaluations now delegate strictly to the existing `BusinessCalendar` bean and its existing configured holiday source (`configuredHolidays`: `2026-01-26` Republic Day, `2026-08-15` Independence Day, `2026-10-02` Gandhi Jayanti, `2026-12-25` Christmas, plus dynamic `addHoliday(LocalDate)`).

### Algorithm Specification (`BusinessCalendar.calculateReopenDeadline(resolvedAt)`)
1. **Timezone Normalization:** Evaluation timestamps are always resolved via `clock.withZone(ZoneId.of("Asia/Kolkata"))` (`IST`, fixed `UTC+05:30`, zero DST shifts).
2. **Start Normalization (`normalizeToBusinessHours`):**
   - If `resolvedAt` falls on Saturday, Sunday, a configured holiday, or before `09:00`, advance to `09:00:00` on the next business day.
   - If `resolvedAt` is at or after `18:00:00`, advance to `09:00:00` on the next business day.
3. **Exact `Duration` Accumulation (`DISPUTE_WINDOW_BUSINESS_HOURS = 48`):**
   - Initialize `remaining = Duration.ofHours(48)`.
   - While `!remaining.isZero()`:
     - Skip non-business days (weekends and `configuredHolidays`) to `09:00:00`.
     - Compute `untilEndOfDay = Duration.between(current.toLocalTime(), WORK_END)` (exact second/nanosecond precision).
     - If `remaining <= untilEndOfDay`, return `current.plus(remaining)`.
     - Otherwise subtract `untilEndOfDay` from `remaining` and advance `current` to `09:00:00` of the next day.
4. **Consistent Inclusive / Exclusive Boundary Rule:**
   - **Reopen Window (`isWithinReopenWindow`):** **INCLUSIVE** of the exact deadline instant (`evaluationTime <= deadline`, i.e., `!evaluationTime.isAfter(deadline)`).
   - **Dispute Expiration & Auto-Closure Eligibility (`isDisputeWindowExpired`, `isAutoClosureEligible`):** **STRICTLY EXCLUSIVE** of the exact deadline instant (`evaluationTime > deadline`, i.e., `evaluationTime.isAfter(deadline)`).
   - Guarantee: Zero overlap and zero gap between reopen permission and auto-closure eligibility at every instant.

---

## 3. Files Changed

### Production Code (2 files — zero changes to DB migrations, API contracts, auth, approvals, notifications, or audit chain)
- `springapp/src/main/java/com/examly/springapp/workflow/BusinessCalendar.java`
- `springapp/src/main/java/com/examly/springapp/workflow/TicketStateMachine.java`

### Automated Test Code (2 files)
- `springapp/src/test/java/com/examly/springapp/BusinessCalendarReopenWindowTests.java` *(New — 10 deterministic `Clock`-based boundary tests)*
- `springapp/src/test/java/com/examly/springapp/TicketWorkflowTests.java` *(Updated `test5_ReopenWithinDisputeWindow` to assert 50 wall-clock hours succeeds and 14 calendar days / >48 business hours fails)*

### Documentation Updated (7 files)
- `docs/architecture/HLD.md` (§5.3 Clock 2 & Clock 3 inclusive/exclusive specification)
- `docs/architecture/LLD.md` (§4 `TicketStateMachine` dispute window snippet corrected from `plusHours(48)` to `BusinessCalendar`)
- `docs/api/task-07e-rest-api-contracts.md` (§4.6 `/reopen` 48-business-hour inclusive rule)
- `docs/implementation/task-07d-workflow-orchestration-report.md` (P9.1 BusinessCalendar & TicketStateMachine alignment)
- `docs/product-discovery/requirements-traceability-matrix.md` (`FR-012` test evidence updated)
- `docs/qa/p9-requirements-traceability-matrix.md` (`REQ-TKT-04` updated to `VERIFIED (Corrected in P9.1)`)
- `docs/qa/p9-known-limitations.md` (`FUNC-P9-01` marked `RESOLVED (P9.1)`)
- `docs/implementation/p9-completion-report.md` (updated with 200-test backend / 233-test total results)
- `docs/implementation/p9-1-business-calendar-correction.md` *(This report)*

---

## 4. Boundary Test Cases & Verified Results (`BusinessCalendarReopenWindowTests`)

All 10 deterministic boundary scenarios executed via injectable `Clock.fixed(...)` (`task-3656`, `0.014 s`, **10/10 passing**):

| # | Test Scenario | Fixed Input (`Asia/Kolkata`) | Expected Result | Status |
| :-: | :--- | :--- | :--- | :---: |
| 1 | Mid-working-day resolution | Mon `2026-03-02 13:30:00` | Deadline Mon `2026-03-09 16:30:00` (`!= plusHours(48)`) | `PASS` |
| 2 | Near end of working hours (sub-minute precision) | Mon `2026-03-02 17:59:30` | Deadline Tue `2026-03-10 11:59:30` | `PASS` |
| 3 | Friday working-hours resolution | Fri `2026-03-06 14:00:00` | Deadline Fri `2026-03-13 17:00:00` (skips Sat/Sun) | `PASS` |
| 4 | Resolution immediately before weekend | Fri `2026-03-06 18:00:00` | Normalizes to Mon `09:00` → Deadline Mon `2026-03-16 12:00:00` | `PASS` |
| 5 | Resolution before configured holiday | Thu `2026-10-01 14:00:00` (before `2026-10-02` Gandhi Jayanti) | Deadline Fri `2026-10-09 17:00:00`; +custom holiday `2026-10-07` → Mon `2026-10-12 17:00:00` | `PASS` |
| 6 | Reopen within 48-business-hour window | Fri `14:00` evaluated Mon `16:00` (+74 wall-clock hrs) & `deadline - 1s` | Allowed by `BusinessCalendar` and `TicketStateMachine`; not auto-closure eligible | `PASS` |
| 7 | Reopen **exactly at deadline** | `now == 2026-03-13 17:00:00` | **INCLUSIVE — ALLOWED** (`isWithinReopenWindow=true`, `isDisputeWindowExpired=false`, `isAutoClosureEligible=false`) | `PASS` |
| 8 | Reopen **after deadline** (+null guard) | `now == 2026-03-13 17:00:01` & `resolvedAt == null` | **REJECTED** (`BusinessRuleViolationException`, `isAutoClosureEligible=true`) | `PASS` |
| 9 | Resolution outside working hours | Fri `20:30`, Sat `11:00`, Sun `16:45`, Mon `06:15`, Holiday `2026-01-26 11:00` | All normalize to next business day `09:00` (including Jan → Feb rollover `2026-02-03 12:00:00`) | `PASS` |
| 10 | Timezone, non-DST stability & year-end holiday rollover | Fixed UTC `Clock` (`2026-03-13T11:30:00Z == 17:00:00 IST`, `+1s` expires); Thu `2026-12-24 15:00` across Christmas `2026-12-25` | Accurate IST conversion, zero DST drift, year rollover to Fri `2027-01-01 18:00:00` | `PASS` |

---

## 5. Full Backend & Frontend Regression Verification

| Suite | Command Executed | Exit Code | Results |
| :--- | :--- | :---: | :--- |
| **Backend Maven Clean Suite** (`task-3656`) | `mvn clean test "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml` | `0` | **200 / 200 passing** across **16 test classes** (`0 failures, 0 errors, 0 skipped`, `01:56 min`) |
| **Frontend Jest Suite** (`task-3657`) | `Set-Location "springapp/springbackend"; npm test -- --watchAll=false` | `0` | **33 / 33 passing** across **3 suites** (`22.7 s`) |
| **Frontend Production Build** (`task-3665`) | `Set-Location "springapp/springbackend"; npm run build` | `0` | **Compiled successfully** (`0 errors, 0 warnings`; `93.96 kB` main + `4.78 kB` Analytics + `2.40 kB` Audit gzip) |

---

## 6. Remaining Limitations

- **Annual Holiday Calendar Updates:** Default corporate holidays in `BusinessCalendar` currently seed 2026 gazetted holidays (with programmatic `addHoliday(LocalDate)` support). Multi-year externalized/database-backed holiday configuration can be introduced in a future enhancement if required.
- **Unchanged P9 Environment Blockers:** Live MySQL 8.0+ runtime verification (`BLK-P9-01`), headless Chromium Playwright execution (`BLK-P9-02`), and online CVE scanning (`BLK-P9-03`) remain P10 pre-deployment gates (unaffected by P9.1).

---

## 7. Verdict

### **`APPROVED`**

The P9.1 business-calendar reopen window correction fully satisfies `BR-003` / `FR-012`, passes 100% of deterministic boundary scenarios and all **233 / 233 automated backend + frontend tests**, and preserves all existing P0–P9 architecture, API contracts, security controls, and migrations.

**STOP RULE:** P9.1 is complete. P10 has not been started.
