# EPAMS — P8 Final Notification & Email Reliability Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Consolidated Notification & External Email Verification  
**Date:** 2026-11-18  

---

## 1. Concurrency-Safe Deduplication & Rollback Safety

- **Database Constraint (`V4`):** `CREATE UNIQUE INDEX uk_alert_recipient_dedup ON in_app_alert (recipient_id, dedup_key)` in `V4__notifications_and_analytics_indexes.sql`.
- **Concurrency Proof:** 8 simultaneous threads submitting identical `dedup_key` (`P9SecurityAndWorkflowQaTests.test8`) resulted in **exactly 1 persisted row** and 0 unhandled errors (`DataIntegrityViolationException` handled gracefully after `saveAndFlush`).
- **Rollback Safety:** `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `REQUIRES_NEW` verified in `NotificationTests.test8` and `P7P9ConsolidatedAcceptanceTests.scenario5` — rolled-back business transactions emit **0 phantom notifications**.
- **Owner-Only Privacy:** Even `ROLE_ADMIN` and cross-department specialists are blocked (`403 Forbidden`) from reading or mutating another user's private notifications (`NotificationTests.test5`, `P7P9ConsolidatedAcceptanceTests.scenario3`).

---

## 2. External Provider-Neutral Email Delivery (`NFR-013`, `GAP-P8-02` Closed)

- Implemented `EmailDeliveryAdapter` + zero-dependency RFC 5321 `SmtpEmailDeliveryAdapter` (Mailpit-compatible, disabled by default `epams.email.enabled=false`, zero hardcoded credentials, CRLF header-injection sanitization, retry support, minimal privacy-safe template).
- Verified end-to-end in `P7P9ConsolidatedAcceptanceTests.scenario5` against an in-process localhost TCP SMTP `ServerSocket`:
  1. Real SMTP `HELO`/`MAIL FROM`/`RCPT TO`/`DATA`/`QUIT` delivered exactly 1 email for a unique event.
  2. Duplicate event retry with identical `dedupKey` suppressed **both** duplicate in-app alert and duplicate email.
  3. Simulated SMTP server outage (closed socket) failed safely with zero impact on in-app alert persistence.

---

## 3. Executor Saturation & Request Latency (`AppConfig.notificationTaskExecutor`)

- Bounded pool: `corePoolSize=2`, `maxPoolSize=4`, `queueCapacity=100`, `CallerRunsPolicy`, graceful shutdown (`15s`).
- Verified 24-task burst (`P9SecurityAndWorkflowQaTests.test8`): 100% completed, 0 dropped, 0 `RejectedExecutionException`. Note: under sustained extreme saturation, `CallerRunsPolicy` provides deliberate backpressure onto the calling thread rather than exhausting heap; acceptable for current target concurrency (15–30 req/s) and monitored via `CorrelationIdFilter` latency logs.
