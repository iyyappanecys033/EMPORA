# EPAMS P9 — Notification System Reliability & Concurrency Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  

---

## 1. Architecture & Delivery Invariants Audited

| Reliability Control | Implementation Location | Verification Method | P9 Status |
| :--- | :--- | :--- | :---: |
| **Post-Commit Dispatch (`AFTER_COMMIT`)** | `NotificationEventListener.java` (`@TransactionalEventListener(phase = AFTER_COMMIT)`) | Verified notifications are never dispatched on rolled-back ticket transactions (`NotificationTests.test8_RollbackSafetyPreventsOrphanNotification`). | `VERIFIED` |
| **Atomic Database Deduplication** | `V4__notifications_and_analytics_indexes.sql` (`uk_alert_recipient_dedup` on `in_app_alert`), `NotificationEventListener.java` (`DataIntegrityViolationException` graceful catch) | 8 simultaneous threads submitting identical `dedup_key` (`P9SecurityAndWorkflowQaTests.test8`) resulted in **exactly 1 persisted row** and 0 unhandled exceptions. | `VERIFIED` |
| **Bounded Async Executor & Backpressure** | `AppConfig.java` (`notificationTaskExecutor`: `corePoolSize=2, maxPoolSize=4, queueCapacity=100, ThreadPoolExecutor.CallerRunsPolicy`) | Saturation burst of 24 concurrent tasks (`P9SecurityAndWorkflowQaTests.test8`) completed 100% cleanly via `CallerRunsPolicy` without dropping tasks or raising `RejectedExecutionException`. | `VERIFIED` |
| **Owner-Only Privacy (BOLA Prevention)** | `NotificationController.java`, `NotificationService.java` (scopes all reads & mark-as-read mutations strictly to authenticated principal's `recipient.id`) | Verified cross-user `PUT /api/v1/notifications/{id}/read` returns `403 Forbidden` (`NotificationTests.test5_CrossUserNotificationAccessBlocked`). | `VERIFIED` |
| **Idempotent Read-State Transitions** | `NotificationService.markAsRead()`, `markAllAsRead()` (preserves original `readAt` truncated to `ChronoUnit.SECONDS`) | Repeated `PUT /read` calls remain idempotent and never decrement unread counts below 0 (`NotificationTests.test4`). | `VERIFIED` |
| **Frontend Bell & Page Synchronization** | `NotificationBell.js`, `NotificationsPage.js`, `notificationService.js` | Verified unread badge display, popover list, single mark-as-read, and bulk mark-all-read clearing badge (`P8Features.test.js` + `App.test.js`). | `VERIFIED` |
| **Scaled Query Latency (600 Notifications)** | `V4` composite index `(recipient_id, is_read, created_at)` | `P9ScalePerformanceTests` (`task-3610`): `GET /api/v1/notifications/my?page=0&size=20` avg **`10.49 ms`** (p95 `12.86 ms`); `/unread-count` avg **`9.44 ms`** (p95 `12.82 ms`). | `VERIFIED` (H2) |

---

## 2. Lifecycle Event Coverage Matrix

| Workflow Event | Expected Recipient(s) | Trigger Verification |
| :--- | :--- | :---: |
| `TICKET_SUBMITTED` | Ticket Requester | `VERIFIED` (`NotificationTests`, `P9LiveServerE2EIntegrationTests`) |
| `APPROVAL_REQUIRED` | Assigned Stage Approver (Manager / Dept Head) | `VERIFIED` (`NotificationTests`, `P9LiveServerE2EIntegrationTests`) |
| `TICKET_APPROVED` | Ticket Requester | `VERIFIED` (`NotificationTests`, `P9LiveServerE2EIntegrationTests`) |
| `APPROVAL_REJECTED` | Ticket Requester | `VERIFIED` (`NotificationTests`) |
| `TICKET_ASSIGNED` | Assigned Department Specialist | `VERIFIED` (`NotificationTests`, `P9LiveServerE2EIntegrationTests`) |
| `STATUS_CHANGED` / `TICKET_RESOLVED` | Ticket Requester | `VERIFIED` (`NotificationTests`, `P9LiveServerE2EIntegrationTests`) |
| `COMMENT_ADDED` (Public) | Requester / Assignee counterpart | `VERIFIED` (`NotificationTests`) |
| `COMMENT_ADDED` (`isInternal=true`) | **Suppressed for non-specialist requester** (zero leakage) | `VERIFIED` (`NotificationTests.test9_InternalCommentDoesNotNotifyRequester`, `SEC-P9-02`) |

---

## 3. Known Limitations & Out-of-Scope Channels

- **External Email (SMTP) / Push / WebSocket Delivery:** Explicitly `DEFERRED` per approved P8/P9 scope. All notifications are persisted in-app database alerts polled safely on navigation and user interaction.
- **Multi-Node Distributed Delivery:** In a horizontally scaled deployment (P10+), `dedup_key` unique constraints guarantee database-level deduplication across replicas, though WebSocket fan-out (if added later) would require a message broker.
