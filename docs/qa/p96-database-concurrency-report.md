# EPAMS — P9.6 Database Concurrency, Locking & Isolation Report (Stage K / M)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification  
**Verification Date:** 2026-11-18  
**Engines Verified:** Real **MySQL `8.0.45`** (InnoDB, `REPEATABLE READ` default) + H2 (`MODE=MySQL`) automated concurrency suites.

---

## 1. Concurrency Control Mechanisms Audited

| Subsystem | Concurrency Control Pattern | Database Primitive / Annotation | Verified Engines | Result |
| :--- | :--- | :--- | :--- | :---: |
| **Tamper-Evident Audit Chain** (`AuditChainService`) | Pessimistic row-level serialization on singleton chain anchor | `SELECT ... FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on `audit_chain_head (id = 1)` + `uk_audit_log_sequence` | MySQL `8.0.45` (`10` threads) & H2 (`8` threads) | **PASS** |
| **Notification Alert Deduplication** (`NotificationEventListener`) | Database-enforced idempotency constraint + `DataIntegrityViolationException` catch | Composite unique constraint `uk_alert_recipient_dedup (recipient_id, dedup_key)` | MySQL `8.0.45` (`8` threads) & H2 | **PASS** |
| **Ticket Concurrent Modification** (`ServiceTicket`) | Optimistic locking with version counter | `@Version Long version` column + `OptimisticLockingConflictException` → HTTP `409 Conflict` | MySQL `8.0.45` & H2 | **PASS** |
| **Approval Decision Race** (`ApprovalTask`) | Optimistic locking + explicit caller version check (`ApprovalDecisionRequestDTO.version`) | `@Version Long version` + service-layer staleness check → HTTP `409 Conflict` | MySQL `8.0.45` & H2 | **PASS** |
| **Scheduled SLA Alert Scanner** (`SlaScheduledProcessor`) | Non-blocking in-JVM `AtomicBoolean` re-entrancy guard + DB dedup constraint | Skips overlapping tick (`SLA alert scanner skipped: previous execution is still active`) + `uk_alert_recipient_dedup` | MySQL `8.0.45` (`2`-thread overlap + sequential retry) & H2 | **PASS** |
| **Transactional Event Dispatch** (`NotificationEventListener`) | Commit-bound event phase | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `REQUIRES_NEW` | MySQL `8.0.45` & H2 | **PASS** |

---

## 2. Empirical Concurrency Execution Evidence

### 2.1 10-Thread Concurrent Audit Chain Writes on Real MySQL `8.0.45` (`Stage2RealMysqlIntegrationTests.test3`)
- **Setup:** 10 worker threads synchronized via `CountDownLatch` simultaneously invoke `auditChainService.recordChainedEvent(...)` against real MySQL `8.0.45` InnoDB (`epams_app`).
- **Observed Result:**
  - `10 / 10` threads completed within timeout with **zero deadlocks**, **zero lock wait timeouts**, and **zero duplicate sequence numbers**.
  - `audit_chain_head.last_sequence` advanced by exact `+10`.
  - Subsequent forced transaction rollback after `recordChainedEvent` left **zero sequence gap** (`afterRollbackSeq == afterConcurrentSeq`).
  - Full cryptographic verification `auditChainService.verifyChain().isValid()` returned **`true`** across MySQL `TIMESTAMP` round-trips.

### 2.2 8-Thread Concurrent Notification Deduplication Race on Real MySQL `8.0.45` (`Stage2RealMysqlIntegrationTests.test4`)
- **Setup:** 8 synchronized threads dispatch identical `TicketNotificationEvent` carrying the same `(recipient_id, dedup_key)` simultaneously.
- **Observed Result:**
  - Exact **`1`** thread returned `true` (persisted & dispatched); `7` losing threads suppressed duplicate gracefully.
  - **`0`** unhandled exceptions leaked (`exceptions.get() == 0`).
  - Exact **`1`** row persisted in `in_app_alert` (`SELECT COUNT(*) == 1`).
  - Verified MySQL standard unique-index behavior for `NULL`: two sequential events with `dedup_key = NULL` both persisted (`true`, `true`).

### 2.3 Optimistic Locking Race on `ServiceTicket` & `ApprovalTask` (`Stage2RealMysqlIntegrationTests.test5`)
- **Setup:** Two concurrent loads of the same `ServiceTicket` and `ApprovalTask` attempt conflicting updates; third test submits stale `version=0L` to `approvalService.decideApproval(...)`.
- **Observed Result:**
  - Both stale JPA writes threw `ObjectOptimisticLockingFailureException` on real MySQL `8.0.45`.
  - Stale API DTO version threw `OptimisticLockingConflictException` and mapped cleanly via `GlobalExceptionHandler` to **HTTP `409 Conflict`** (RFC 7807 ProblemDetail).

### 2.4 Overlapping Scheduled SLA Scanner on Real MySQL `8.0.45` (`Stage2RealMysqlIntegrationTests.test6`)
- **Setup:** Two concurrent threads submitted `slaScheduledProcessor.processSlaAlerts(evalTime)` simultaneously, followed by a third sequential retry.
- **Observed Log Evidence (`task-4782`):**
  - Thread 2 logged `WARN ... SLA alert scanner skipped: previous execution is still active`.
  - Thread 1 persisted exact `1` `SLA_BREACHED` alert (`sla_alert_scan scanned=2 alerted=1 skipped=1 failed=0`).
  - Third retry persisted `0` additional rows; `SELECT COUNT(*) FROM in_app_alert WHERE dedup_key = 'SLA_BREACH_<id>'` = **`1`**.

---

## 3. Scale & Multi-Replica Concurrency Considerations for P10

1. **Multi-Instance Scheduler Guard:** The current `AtomicBoolean` overlap guard prevents concurrent runs inside a single JVM, and `uk_alert_recipient_dedup` prevents duplicate alerts across multiple JVMs. If P10 runs `>= 2` backend instances, adding **ShedLock** (DB-backed lock) will prevent redundant scan queries across replicas.
2. **Audit Chain Throughput Ceiling:** Pessimistic locking on `audit_chain_head (id = 1)` guarantees strict cryptographic ordering without gaps. Verified smooth at `10` concurrent writers; monitor `Innodb_row_lock_time_avg` in P10 if write concurrency grows beyond `50+` simultaneous audit-producing transactions/sec.
