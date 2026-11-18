# EPAMS — Final Multi-Year Business Holiday Calendar Management (`GAP-CAL-2027`)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Finding ID:** `GAP-CAL-2027`  
**Date:** 2026-11-18  
**Status:** **`FIXED & VERIFIED`**  

---

## 1. Design Choice (Least Complex Maintainable Strategy)

Rather than inventing speculative national or company-specific holidays for 2027+ or introducing an unnecessary database migration for a low-frequency annual list, `BusinessCalendar` was upgraded to support **externalized environment/property-driven multi-year holiday configuration**:

- **Property:** `epams.calendar.holidays=${EPAMS_HOLIDAYS:}` (comma-separated ISO-8601 `YYYY-MM-DD` dates, configured in `application-mysql.properties`).
- **Baseline Flag:** `epams.calendar.include-default-2026=${EPAMS_INCLUDE_DEFAULT_2026:true}` preserves existing 2026 gazetted baseline dates (`2026-01-26`, `2026-08-15`, `2026-10-02`, `2026-12-25`) without breaking any existing test.
- **Thread-Safe Runtime Operations:** Backed by `ConcurrentHashMap.newKeySet()` with `loadExternalHolidays(String csv)`, `addHoliday(LocalDate)`, `addHolidays(Collection<LocalDate>)`, and `removeHoliday(LocalDate)`. Duplicate entries are automatically deduplicated, and malformed date strings log a warning without crashing application startup.

---

## 2. Verified Multi-Year & Leap-Year Behavior

Verified in `Stage1RemediationVerificationTests.test2_MultiYearExternalizedHolidayCalendarAndLeapYear` and `BusinessCalendarReopenWindowTests`:
- `2026` → `2027` year-end rollover across Christmas (`2026-12-25`) into `2027-01-01`.
- `2027` externalized holiday (`2027-01-26`) loaded cleanly; duplicates and malformed strings ignored safely.
- **2028 Leap Year (`2028-02-29`):** Resolved `Mon 2028-02-28 14:00` with `2028-02-29` configured as a holiday accurately skips leap day and weekends to `Tue 2028-03-07 17:00`; runtime removal of `2028-02-29` immediately shifts the deadline to `Mon 2028-03-06 17:00`.

---

## 3. Annual Operational Procedure (P10+)

Before December each year, operations supplies the approved corporate holiday CSV via `EPAMS_HOLIDAYS` environment configuration (e.g., `EPAMS_HOLIDAYS=2027-01-26,2027-08-15,2027-10-02,2027-12-25`) without recompiling code or running schema migrations.
