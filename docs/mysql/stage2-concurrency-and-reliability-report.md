# EPAMS â€” Stage 2 Real MySQL Concurrency, Reliability & Query Plan Report (Stage E)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration (Stage E: Database Reliability, Concurrency & Analytics Performance)
**Target Database:** `epams_stage2_fresh` on real MySQL `8.0.45` (`127.0.0.1:13307`) executing under least-privilege **`epams_app`**
**Test Suite:** `Stage2RealMysqlIntegrationTests` (`7 / 7` passed, `0` failures, `71.695 s`)
**Date:** 2026-11-18

---

## E.1 Audit-Chain Integrity Under Real MySQL Concurrency (`VERIFIED`)

- **Test:** 10 concurrent worker threads (`ExecutorService` + `CountDownLatch`) calling `AuditChainService.recordChainedEvent(...)` simultaneously on real MySQL `8.0.45` InnoDB.
- **Locking & Isolation:** `AuditChainHeadRepository.findByIdForUpdate(1L)` (`SELECT ... FOR UPDATE`) under `@Transactional(isolation = READ_COMMITTED)` serialized all 10 writes with zero deadlocks, zero lock timeouts, and zero duplicate sequence numbers.
- **Monotonicity & Hash Continuity:** `audit_chain_head.last_sequence` advanced monotonically by exact `+10`; `auditChainService.verifyChain()` returned **`valid=true`** with zero violations â€” proving canonical SHA-256 hash continuity across real MySQL `TIMESTAMP` (`serverTimezone=UTC`) round-trips.
- **Rollback Integrity:** Forced `IllegalStateException` after `recordChainedEvent(...)` inside a transaction rolled back both the `audit_log` insert and the `audit_chain_head` update, leaving zero sequence gaps and `verifyChain().isValid() == true`.
- **Runtime Tamper Resistance (`epams_app`):** JDBC `UPDATE audit_log`, `DELETE FROM audit_log`, and `DELETE FROM audit_chain_head` all threw `DataAccessException` (MySQL `ERROR 1142`).

---

## E.2 Notification Deduplication Under Real MySQL Concurrency (`VERIFIED`)

- **Test:** 8 concurrent threads firing `NotificationEventListener.dispatchNotification(...)` simultaneously with identical `(recipient_id, dedup_key)` on real MySQL `8.0.45`.
- **Constraint Enforcement:** Real MySQL unique index `in_app_alert.uk_alert_recipient_dedup` rejected racing inserts (`Duplicate entry '...' for key 'in_app_alert.uk_alert_recipient_dedup'`), caught cleanly inside `NotificationEventListener`.
- **Result:** Exactly **1** thread returned `true` (ensuring at-most-once downstream email trigger), **0** unhandled exceptions occurred, and `SELECT COUNT(*) FROM in_app_alert WHERE recipient_id=? AND dedup_key=?` returned **exact `1`**.
- **Null-Key Semantics:** Verified multiple alerts with `dedup_key = NULL` for the same recipient persist cleanly under MySQL `UNIQUE INDEX` semantics.

---

## E.3 Optimistic Locking (`@Version`) & HTTP `409 Conflict` (`VERIFIED`)

- **`ServiceTicket` (`@Version`):** Two detached reads at `version=0`; tx1 committed (`version=1`); stale tx2 `saveAndFlush` threw `ObjectOptimisticLockingFailureException` against real MySQL `8.0.45`.
- **`ApprovalTask` (`@Version`):** Two detached reads at `version=0`; tx1 committed (`version=1`); stale tx2 `saveAndFlush` threw `ObjectOptimisticLockingFailureException`.
- **Service & HTTP `409` Mapping:** Calling `ApprovalService.decideApproval(...)` with stale `version=0` threw `OptimisticLockingConflictException`, verified mapped by `GlobalExceptionHandler` to **HTTP `409 Conflict`** RFC 7807 Problem Details.

---

## E.4 Transaction Integrity, `AFTER_COMMIT` & Scheduled SLA Jobs (`VERIFIED`)

- **Atomicity:** Verified `TicketCommandService.createTicket(...)` and `ApprovalService.decideApproval(...)` atomically persist domain mutations + synchronous `AuditChainService` records on real MySQL.
- **Rollback Notification Suppression:** Rolled-back `REQUIRES_NEW` transaction publishing a `TicketNotificationEvent` produced **`0` rows** in `in_app_alert` (`TransactionPhase.AFTER_COMMIT` verified on real MySQL).
- **Scheduled Auto-Closure Retry Idempotency:** First run of `SlaScheduledProcessor.processAutoClosures(...)` closed eligible resolved ticket (`scanned=1 closed=1`) and emitted 1 `AUTO_CLOSE_*` alert; immediate retry closed `0` (`scanned=0 closed=0`) with alert count remaining exact `1`.
- **Overlapping SLA Alert Scan Safety:** Two concurrent threads invoking `processSlaAlerts(...)` + sequential third retry logged `SLA alert scanner skipped: previous execution is still active` on thread 2 and persisted exact **`1`** deduplicated `SLA_BREACH_*` alert.

---

## E.5 Real MySQL `EXPLAIN` & `EXPLAIN ANALYZE` on Synthetic Dataset (`VERIFIED`)

**Synthetic Dataset Seeded Into Real MySQL `8.0.45` (`epams_stage2_fresh`):**
- `service_ticket`: **`1,003` rows** (1,000 synthetic across 5 departments, 4 priorities, 6 statuses, 30-day window + 3 workflow test rows)
- `approval_task`: **`402` rows**
- `in_app_alert`: **`500+` rows**
- **Full `AnalyticsService.getDashboardBundle(...)` Execution Time on Real MySQL `8.0.45`:** **`508 ms`** (`periodTotalTickets=1003`, `slaRate=33.27%`).

### Actual Captured MySQL `8.0.45` Query Plans (`EXPLAIN` / `EXPLAIN ANALYZE`)

| Query | Access Type / Key Used | Rows Examined | Actual Time (`EXPLAIN ANALYZE`) | Engineering Assessment |
| :--- | :--- | :---: | :---: | :--- |
| **Q1: `findAnalyticsProjectionInPeriod` (all depts, 30d)** | `ALL` (`possible_keys=idx_ticket_created_dept_status`, `key=NULL`) | `1,003` | `1.63 ms` | Optimal on synthetic dataset because 100% of rows fell inside the 30-day filter (`selectivity = 1.0`). |
| **Q2: `findAnalyticsProjectionInPeriod` (`LOWER(dept)='it'`)** | `ALL` (`possible_keys=idx_ticket_created_dept_status`, `key=NULL`) | `1,003` (202 matched) | `2.14 ms` | **Actionable Finding:** `LOWER(t.department)` prevents B-tree pushdown on `department` even though MySQL `utf8mb4` collation is already case-insensitive. Non-blocking at current scale (`2.14 ms`), recommended P10 scale optimization to rely on case-insensitive collation directly. |
| **Q3: `countGroupedByPriority`** | `index` via **`idx_ticket_priority_dept`** | `1,003` â†’ `4` groups | `3.77 ms` | Uses V4 composite index cleanly. |
| **Q4: `countOpenBacklogByDepartment`** | `index` (**Covering Index Scan** via **`idx_ticket_dept_status`**, `Using index`) | `1,003` â†’ `5` groups | **`1.73 ms`** | Zero table lookups (100% covering index scan). |
| **Q5: `countOverdueOpenByDepartment`** | `index` via **`idx_ticket_dept_status`** | `1,003` â†’ `5` groups | `4.85 ms` | Uses composite index cleanly; evaluates `sla_target_date` filter. |
| **Q6: `findServicePopularity` (`JOIN service_catalog`)** | `c: ALL` (6 rows) â†’ `t: ref` (**Covering Index Lookup** via **`idx_ticket_catalog_created`**, `Using index`) | `6 Ã— ~167` | **`6.27 ms`** | V4 index `idx_ticket_catalog_created` eliminates full ticket table scan during JOIN. |
| **Q7: `countOverduePendingApprovals` (`JOIN service_ticket`)** | `a: ALL` (402 rows) â†’ `t: eq_ref` (**Covering `PRIMARY` lookup**) | `402` â†’ `134` | **`1.29 ms`** | Optimizer skipped `idx_approval_status_created` at 33% selectivity on 402 rows; single-row PK lookup on `service_ticket`. |
| **Q8: Unread Notification Inbox (`LIMIT 20`)** | `ref` (**Backward Index Scan** via **`idx_alert_recipient_read`**) | `20` fetched (of `341`) | **`0.532 ms`** | Zero filesort; sub-millisecond reverse index lookup. |
