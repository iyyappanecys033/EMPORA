# EPAMS P9 — System Reliability, Failure-Mode & Resilience Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  

---

## 1. Failure-Mode & Edge-Case Verification Matrix

| Failure Scenario | Expected Resilient Behavior | Automated Verification Evidence | P9 Status |
| :--- | :--- | :--- | :---: |
| **Malformed / Truncated JSON Request Body** | Returns RFC 7807 `400 Bad Request` (`HttpMessageNotReadableException` handled in `GlobalExceptionHandler`), zero stack trace exposure, never `500`. | `P9SecurityAndWorkflowQaTests.test10_MalformedJsonAndHealthEndpointSafety` | `VERIFIED` |
| **Stale Concurrent Ticket Mutation (Optimistic Lock)** | `@Version` conflict throws `OptimisticLockingConflictException` mapped to RFC 7807 `409 Conflict`, preserving first writer's state cleanly. | `P9SecurityAndWorkflowQaTests.test6_ConcurrentOptimisticLockingConflict` | `VERIFIED` |
| **Concurrent Audit Hash-Chain Appends (6 Threads)** | Serialized chain sequence allocation maintains strictly monotonic `chain_sequence` and valid SHA-256 links across all 6 concurrent writers (`verifyChain().isValid() == true`). | `P9SecurityAndWorkflowQaTests.test7_ConcurrentAuditChainPreservesCryptographicIntegrity` | `VERIFIED` |
| **Concurrent Duplicate Notification Race (8 Threads)** | `in_app_alert.uk_alert_recipient_dedup` atomically enforces single persistence; duplicate races swallowed cleanly without bubbling `500`. | `P9SecurityAndWorkflowQaTests.test8_ConcurrentNotificationDedupAndExecutorSaturation` | `VERIFIED` |
| **Async Executor Queue Saturation (24 Burst Tasks)** | Bounded `notificationTaskExecutor` (`core=2, max=4, queue=100`) applies `ThreadPoolExecutor.CallerRunsPolicy` backpressure; 100% tasks complete, 0 dropped, 0 `RejectedExecutionException`. | `P9SecurityAndWorkflowQaTests.test8_ConcurrentNotificationDedupAndExecutorSaturation` | `VERIFIED` |
| **Mid-Transaction Rollback Isolation** | Rolled-back ticket mutations do not emit orphan `AFTER_COMMIT` notifications or orphan audit chain records. | `NotificationTests.test8_RollbackSafetyPreventsOrphanNotification`, `AuditLogTests` | `VERIFIED` |
| **Suspended Account With Active Non-Expired JWT** | `JwtFilter` checks live DB account status (`AccountStatus.ACTIVE`) on every request and immediately rejects suspended user's valid JWT with `401`. | `P9SecurityAndWorkflowQaTests.test9_SuspendedUserJwtRejected` | `VERIFIED` |
| **Mandatory Business Reason Boundary Cases** | 14-char and whitespace-padded (`"   short   "`) reasons rejected with `400`; exact 15-char trimmed reason accepted cleanly. | `P9SecurityAndWorkflowQaTests.test5_MandatoryReasonBoundaryValidation` | `VERIFIED` |
| **Unauthenticated Readiness Probe (`GET /api/v1/health`)** | Returns `200 OK` with minimal status payload (`status=UP`, `service=EPAMS`) and leaks zero JDBC URLs, credentials, stack traces, or host secrets. | `P9SecurityAndWorkflowQaTests.test10_MalformedJsonAndHealthEndpointSafety` | `VERIFIED` |
| **Correlation ID Propagation & Log Sanitization** | `CorrelationIdFilter` echoes/generates `X-Correlation-Id` and logs structured `method`, `path`, `status`, `durationMs` without logging cookies, passwords, or JWTs. | Verified in `CorrelationIdFilter.java` and live-server logs (`task-3519`, `task-3543`). | `VERIFIED` |
| **Frontend Session Expiry & Offline Network Errors** | `api.js` dispatches `session-expired` on failed refresh clearing `AuthContext` immediately; `formatApiError` returns structured `status: 0` fallback when offline. | `P9FrontendQa.test.js` (`P9-FE-01`, `P9-FE-03`) | `VERIFIED` |
| **Frontend Duplicate Submission Prevention** | `CreateTicketModal` blocks whitespace-only justification before network call and disables the submit button (`disabled={submitting}`) while request is in flight. | `P9FrontendQa.test.js` (`P9-FE-04`) | `VERIFIED` |

---

## 2. Concurrency & Resource Utilization Summary

Under `P9ScalePerformanceTests` (final clean run `task-3610`, 12 concurrent worker threads executing 120 mixed notification, analytics dashboard, and department queue requests against 480 tickets / 600 alerts / 100 approvals):
- **HTTP Error Rate:** `0 / 120` (**0.0%**)
- **Total Wall-Clock Time:** `314 ms` (**`382.2 req/s`**)
- **Latency Distribution:** Avg `28.02 ms`, P50 `26.19 ms`, P95 `47.14 ms`
- **JVM Heap Used Post-Load:** `180 MB` (stable across full 190-test JVM run, zero GC thrashing or OOM)

---

## 3. Infrastructure Failure Modes Deferred to P10

- **Database Connection Pool Exhaustion / Failover on Live MySQL 8.0+:** `BLOCKED` (requires live MySQL container/service in P10).
- **Reverse Proxy / Load Balancer Graceful Shutdown & Rolling Restarts:** `DEFERRED` to P10 deployment engineering.

