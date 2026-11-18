# EPAMS — P7 Workflow & Authorization Acceptance Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P7 — Ticket Lifecycle, Approval Workflow & Role-Based UX  
**Date:** 2026-11-18  

---

## 1. 11-State Ticket Machine Verification (`TicketStateMachine`)

All 11 approved states (`DRAFT`, `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`) are preserved and validated:

- **Illegal state skips & terminal state immutability:** Rejected with `IllegalStateTransitionException` (`400 Bad Request`, `TicketWorkflowTests.test1`, `test2`).
- **Mandatory reason boundary ($\ge 15$ trimmed characters):** Verified at 14 chars (`400`), whitespace-only (`400`), exact 15 chars (`200`), and >15 chars (`200`) (`P9SecurityAndWorkflowQaTests.test5`, `TicketWorkflowTests.test3`).
- **Requester cancellation boundary (`ADR-007`):** Permitted prior to active work (`SUBMITTED`/`PENDING_APPROVAL`/`ASSIGNED`), blocked once `IN_PROGRESS` (`TicketWorkflowTests.test4`).
- **Optimistic concurrency control (`@Version`):** Conflicting concurrent updates fail with RFC 7807 `409 Conflict` (`TicketPersistenceTests.test4`, `ApiSecurityTests.test17`, `P9SecurityAndWorkflowQaTests.test6`).
- **48-Business-Hour Reopen Window (`BR-003`):** Corrected in P9.1 to exact `BusinessCalendar.calculateReopenDeadline(resolvedAt)` (`Asia/Kolkata`, Mon–Fri `09:00–18:00`, weekends & configured holidays excluded, inclusive exact deadline; `BusinessCalendarReopenWindowTests` 10/10 + `TicketWorkflowTests.test5`).

---

## 2. Approval Workflow & Multi-Department Queue Verification

- **Single-Level & Two-Stage Approvals:** Verified in `TicketWorkflowTests.test6`–`test9` and `P7P9ConsolidatedAcceptanceTests.scenario2`.
- **Anti-Self-Approval:** Enforced server-side via `AntiSelfApprovalException` (`403 Forbidden`), even if manager submits against their own department (`TicketWorkflowTests.test7`, `P7P9ConsolidatedAcceptanceTests.scenario2`).
- **Approval History BOLA Protection (`SEC-P9-04`):** `GET /api/v1/approvals/tickets/{ticketId}` strictly enforces `canViewTicket || isAssignedApprover` (`P9SecurityAndWorkflowQaTests.test4`).
- **Department Queue Isolation (`ADR-013`):** IT, HR, and Facilities specialists are strictly isolated to their own department queue (`GET /api/v1/departments/{department}/queue`, cross-department `403`, verified in `ApiSecurityTests.test11`–`12`, `P9LiveServerE2EIntegrationTests.test2`, and `P7P9ConsolidatedAcceptanceTests.scenario3`).

---

## 3. Comments Privacy & XSS Safety

- **Internal Specialist Notes (`SEC-P9-02`):** Non-specialists blocked from setting `isInternal=true` (`403`), and internal notes are stripped from requester `GET /comments` responses (`P9SecurityAndWorkflowQaTests.test2`, `P9LiveServerE2EIntegrationTests.test2`).
- **XSS Safety:** React JSX auto-escaping + `Content-Security-Policy` (`frame-ancestors 'none'; object-src 'none'`) + `X-Content-Type-Options: nosniff`.
