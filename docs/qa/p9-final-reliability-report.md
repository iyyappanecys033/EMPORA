# EPAMS — P9 Final Reliability & Failure-Recovery Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Consolidated Reliability Engineering Validation  
**Date:** 2026-11-18  

---

## 1. Failure-Mode Verification Matrix

| Failure Mode | Verified Resilient Behavior | Evidence | Status |
| :--- | :--- | :--- | :---: |
| **Mid-Transaction Rollback** | Zero partial business tickets committed, zero orphan `AFTER_COMMIT` notifications persisted. | `NotificationTests.test8`, `P7P9ConsolidatedAcceptanceTests.scenario5` | `VERIFIED` |
| **External SMTP Outage** | Closed/unreachable SMTP relay fails cleanly with bounded retry and error log; in-app alert persists unaffected. | `P7P9ConsolidatedAcceptanceTests.scenario5` | `VERIFIED` |
| **Duplicate Event / Retry Delivery** | Atomic `uk_alert_recipient_dedup` constraint + `saveAndFlush` prevents duplicate in-app alerts AND prevents duplicate outbound emails. | `P9SecurityAndWorkflowQaTests.test8`, `P7P9ConsolidatedAcceptanceTests.scenario5` | `VERIFIED` |
| **Scheduled Job Concurrency & Partial Failure** | `SlaScheduledProcessor` isolates each ticket in `REQUIRES_NEW` tx, handles `ObjectOptimisticLockingFailureException`, uses `AtomicBoolean` re-entrancy guard, and logs structured run summaries. | `P7P9ConsolidatedAcceptanceTests.scenario4` | `VERIFIED` |
| **Concurrent Ticket Mutation** | Optimistic locking (`@Version`) rejects stale updates with RFC 7807 `409 Conflict`. | `P9SecurityAndWorkflowQaTests.test6` | `VERIFIED` |
| **Concurrent Audit Appends (6 Threads)** | `audit_chain_head` row lock serializes sequence numbers cleanly (`verifyChain().isValid() == true`). | `P9SecurityAndWorkflowQaTests.test7` | `VERIFIED` |
| **Executor Saturation (24 Tasks)** | Bounded `notificationTaskExecutor` applies `CallerRunsPolicy`; 0 dropped tasks, 0 `RejectedExecutionException`. | `P9SecurityAndWorkflowQaTests.test8` | `VERIFIED` |
| **Suspended User Active JWT** | `JwtFilter` rejects suspended account's valid JWT immediately (`401`). | `P9SecurityAndWorkflowQaTests.test9` | `VERIFIED` |
| **Malformed JSON & Health Probe** | Malformed JSON returns RFC 7807 `400`; `/api/v1/health` leaks zero secrets/JDBC URLs. | `P9SecurityAndWorkflowQaTests.test10` | `VERIFIED` |
| **Frontend Offline / Session Expiry** | `session-expired` clears `AuthContext`; `formatApiError` handles `status: 0` offline fallback; `CreateTicketModal` prevents double-submit. | `P9FrontendQa.test.js` (`P9-FE-01`..`06`) | `VERIFIED` |
