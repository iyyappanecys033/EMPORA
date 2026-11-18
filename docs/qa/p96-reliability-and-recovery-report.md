# EPAMS — P9.6 Reliability, Transaction Integrity & Recovery Report (Stage M)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification (Stage M)  
**Verification Date:** 2026-11-18  
**Automated Evidence Sources:** `P96TransactionReliabilityTests` (`5/5` passed), `Stage2RealMysqlIntegrationTests` (real MySQL `8.0.45`), live packaged JAR kill/restart recovery test (`scratch/p96-live-results.json`), isolated `mysqldump` backup & restore test (`scratch/p96-mysql-results.json`).

---

## 1. Reliability & Recovery Verification Matrix

| Reliability Scenario | Verification Method | Expected Outcome | Observed Outcome | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Transaction Rollback — Zero Phantom Notifications** | `P96TransactionReliabilityTests.test1` & `Stage2RealMysqlIntegrationTests.test6` | Rolled-back transaction suppresses `@TransactionalEventListener(AFTER_COMMIT)` alerts | `0` alerts persisted for rolled-back transaction | **PASS** |
| **Transaction Rollback — Zero Audit Sequence Gap** | `Stage2RealMysqlIntegrationTests.test3` | Forced rollback after `recordChainedEvent` leaves `audit_chain_head.last_sequence` unchanged | Exact sequence preserved; `verifyChain().isValid() == true` | **PASS** |
| **Alert Deduplication Under Retry & Concurrency** | `P96TransactionReliabilityTests.test2` & `Stage2RealMysqlIntegrationTests.test4` (8-thread MySQL race) | Unique constraint `uk_alert_recipient_dedup` persists exact `1` row with `0` leaked exceptions | Exact `1` row persisted; `0` exceptions; multiple `NULL` dedup keys still allowed | **PASS** |
| **Optimistic Locking Conflict (`@Version` → HTTP 409)** | `P96TransactionReliabilityTests.test3` & `Stage2RealMysqlIntegrationTests.test5` | Stale `ServiceTicket` / `ApprovalTask` version throws `OptimisticLockingConflictException` → RFC 7807 `409 Conflict` | Verified on both H2 and real MySQL `8.0.45` | **PASS** |
| **Concurrent Audit Chain Writes (`FOR UPDATE`)** | `P96TransactionReliabilityTests.test4` (8 threads H2) & `Stage2RealMysqlIntegrationTests.test3` (10 threads MySQL 8) | Zero duplicate sequence numbers, zero deadlocks, 100% valid SHA-256 chain | `10/10` distinct sequential records; `verifyChain() == true` | **PASS** |
| **Scheduled SLA Breach & Auto-Closure Idempotency** | `P96TransactionReliabilityTests.test5` & `Stage2RealMysqlIntegrationTests.test6` | Repeated & 2-thread overlapping runs process once; `ON_HOLD` tickets excluded from breach | `run1 >= 1`, `run2 == 0`; exact `1` `AUTO_CLOSE` & `1` `SLA_BREACH` alert; `ON_HOLD` skipped | **PASS** |
| **Process Crash & Cold Restart Recovery** | Live packaged JAR killed (`Stop-Process -Force`) & restarted on persistent DB (`p96-live-results.json`) | Server recovers cleanly; committed ticket & SHA-256 audit chain intact | Recovered in **`9.82 s`**; `ticketPreserved=true`; `auditChainValidAfterRestart=true` | **PASS** |
| **MySQL 8 Logical Backup & Full Restore** | Isolated `mysqldump --no-tablespaces --single-transaction` → fresh schema `epams_p96_restore` | All 14 tables & data restored cleanly | `14 / 14` tables restored (`restoreVerified=true`) | **PASS** |
| **Client Double-Submit Resilience** | `P96FrontendComprehensive.test.js` (`TicketSubmissionForm` double-click test) | Submit button disabled during in-flight POST; single API call issued | Exact `1` POST request emitted | **PASS** |
| **API Error Fallback & Retry UX** | `P96FrontendComprehensive.test.js` (`AnalyticsDashboard` error recovery test) | Graceful error state + user retry recovers data without white-screen crash | Error alert displayed cleanly; retry restores full dashboard | **PASS** |

---

## 2. Deterministic Business-Time & Scheduler Verification

- `BusinessCalendar` and `SlaScheduledProcessor` were verified with deterministic fixed `java.time.Clock` injection:
  - Weekend (Saturday/Sunday) and 2026 corporate holiday dates are excluded from business-hour SLA calculations and the 48-business-hour auto-closure window.
  - `ON_HOLD` tickets pause SLA breach processing cleanly (`totalPausedMinutes` tracked on resume).
  - Overlapping scheduled executions (simulating multi-instance or retry overlap) remain idempotent due to terminal state guards + `uk_alert_recipient_dedup`.

---

## 3. Operational Recovery Notes for P10 Production

1. **Automated Backups:** Schedule daily `mysqldump --single-transaction --routines --triggers` (or managed RDS/Cloud SQL automated snapshots + binlog PITR) with periodic restore drills.
2. **Distributed Scheduler Lock (Multi-Replica P10):** Single-node scheduler idempotency is verified; if deploying `>= 2` backend replicas in P10, add ShedLock (DB-backed lock table) or designate a single scheduler replica to avoid redundant DB scans.
3. **Graceful Shutdown:** Enable `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=30s` in P10 production configuration so in-flight transactions complete cleanly during rolling restarts.
4. **Health / Readiness Probes:** `/api/v1/health` checks live database connectivity and returns HTTP `200` (`{"status":"UP","database":"UP",...}`), ready for Kubernetes/load-balancer readiness probes.
