# EPAMS — P8 Notification, Email & Scheduled SLA Architecture

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P8 — Consolidated Notification, Email & SLA Processing Architecture  
**Date:** 2026-11-18  

---

## 1. Event-Driven Notification Pipeline

1. **Originating Transaction:** `TicketCommandService`, `ApprovalService`, and `SlaScheduledProcessor` publish `TicketNotificationEvent` inside active `@Transactional` business boundaries.
2. **Post-Commit Listener (`NotificationEventListener`):** Annotated with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` and `@Transactional(propagation = Propagation.REQUIRES_NEW)`. Rolled-back transactions never fire notifications.
3. **Concurrency-Safe Atomic Deduplication (`V4`):**
   - `V4__notifications_and_analytics_indexes.sql` enforces `UNIQUE INDEX uk_alert_recipient_dedup ON in_app_alert (recipient_id, dedup_key)`.
   - `NotificationEventListener.dispatchNotification()` checks `existsByRecipientIdAndDedupKey` as a fast path and flushes via `saveAndFlush(alert)`, catching `DataIntegrityViolationException` cleanly so concurrent races across 8+ threads persist **exactly 1 row** with 0 errors.
4. **Bounded Async Executor (`AppConfig.notificationTaskExecutor`):** `corePoolSize=2`, `maxPoolSize=4`, `queueCapacity=100`, `CallerRunsPolicy`, `waitForTasksToCompleteOnShutdown=true`, `awaitTerminationSeconds=15`.

---

## 2. Provider-Neutral External Email Delivery (`NFR-013`)

- **Interface & Implementation:** `EmailDeliveryAdapter` + zero-dependency RFC 5321 `SmtpEmailDeliveryAdapter` (`com.examly.springapp.notification`).
- **Configuration (Zero Hardcoded Credentials):** `epams.email.enabled` (default `false`), `epams.email.host` (`localhost`), `epams.email.port` (`1025`, Mailpit-compatible), `epams.email.from` (`no-reply@epams.local`), `epams.email.timeout-ms`, `epams.email.max-retries`.
- **Failure & Privacy Isolation:**
  - Invoked strictly **after** `inAppAlertRepository.saveAndFlush(alert)` succeeds, so suppressed duplicates never send duplicate emails.
  - Wrapped in an isolated `try/catch`; SMTP outages never fail or roll back in-app notification persistence.
  - Strips CR/LF/null characters (`sanitizeHeader`) to prevent SMTP header injection.
  - Uses minimal templates containing only ticket reference and title — never leaks internal comments, PII, or credentials.

---

## 3. Scheduled SLA & Automatic Closure Processor (`SlaScheduledProcessor`, `ADR-016`)

Enabled via `SchedulingConfig` (`@EnableScheduling`, controlled by `epams.scheduling.enabled=true` in runtime, `false` in tests for deterministic invocation):

| Job | Schedule (`Asia/Kolkata`) | Eligible States | Rules & Guarantees |
| :--- | :--- | :--- | :--- |
| **`processAutoClosures()`** | `0 0 * * * *` (Hourly) | `RESOLVED` only | Evaluates `BusinessCalendar.isAutoClosureEligible(resolvedAt, now)` (strictly `now > calculateReopenDeadline(resolvedAt)`), validates `RESOLVED -> CLOSED` via `TicketStateMachine`, runs per-ticket `REQUIRES_NEW` tx with optimistic lock handling, writes `SYSTEM_AUTO_CLOSURE` SHA-256 audit entry, emits deduplicated `TICKET_AUTO_CLOSED` alert (`dedupKey = AUTO_CLOSE_{id}`). |
| **`processSlaAlerts()`** | `0 */30 * * * *` (Every 30m) | `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS` (**excludes `ON_HOLD`**) | Emits deduplicated `SLA_BREACHED` (`SLA_BREACH_{id}`) when `now > slaTargetDate` and `SLA_WARNING` (`SLA_WARN_{id}`) within 2h of `slaTargetDate` to assigned specialist. |

Both jobs use `AtomicBoolean` re-entrancy guards and log structured run summaries (`scanned`, `processed`, `skipped`, `failed`).
