# EPAMS — Employee, Manager, Resolver & Admin Experience Redesign (Stages U3 & U3B)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Date:** 2026-11-18

---

## 1. Employee Home Dashboard (`EmployeeDashboard.js`)
- **Personalized Hero:** Time-aware greeting (`Good morning/afternoon/evening`), generated `Avatar`, employee identity metadata (`designation`, `department`, `employeeId`), primary `New Service Request` CTA, and quick-action shortcuts (`Service Catalog`, `My Tickets` with backend `totalElements`, `Pending Approvals` if approver, `Department Queue` if resolver, `Notifications` with unread count, `My Profile`).
- **KPI Summary Strip:** Active Requests (Recent), Total Tickets (backend `totalElements`), Unread Notifications (real `/api/v1/notifications/my/unread-count`), and contextual Manager/Specialist cards.
- **Three-Panel Workspace:** My Recent Tickets, Featured Services (1-click Request), and Notifications Preview (real `/api/v1/notifications/my`).
- **Guest Mode:** Refined enterprise hero with direct Sign In and Browse Catalog actions.

## 2. Service Catalog (`ServiceCatalog.js`) & Ticket Creation (`CreateTicketModal.js`)
- **Catalog Discovery:** Keyword search, department selector, dynamic category filter pills derived strictly from actual catalog data, SLA badge, approval requirement tags, and `CardGridSkeleton` loading.
- **Request Creation:** Accessible `role="dialog"` modal with Escape dismiss, selected-service SLA/approval preview card, required field indicators, live character counter (`2000` max), double-submit prevention (`submitting` guard), exact backend DTO payload (`catalogItemId`, `department`, `priority`, `description`), and toast confirmation with ticket reference.

## 3. My Tickets (`MyTickets.js`) & Ticket Workspace (`TicketDetail.js` + `TicketComments.js`)
- **My Tickets:** Search, all-11-state status filter, 5-tier priority filter, sort selector (`Newest`, `Oldest`, `Highest Priority`), Table/Card view toggle, responsive `.ep-table-wrap`, and backend pagination metadata (`totalElements`).
- **Ticket Detail:** Visual **`StatusTimeline`** representing all 11 backend lifecycle states + contextual next-step guidance, requester/assignee `Avatar` badges, SLA breach highlighting, tokenized action modals, and visually distinct `Internal Note` styling (strictly role-gated for Resolvers/Admins).

## 4. Manager, Resolver, Analytics & Audit Workspaces
- **Department Queue (`DepartmentQueue.js`):** Added live search, priority filter, calculated ticket age (`Xh` / `Xd Yh`), SLA overdue indicator, toast feedback on self-assign, and controlled horizontal scroll wrapper.
- **Approval Center (`ApprovalQueue.js`):** Stage badges, left status border, prominent Anti-Self-Approval guardrail, accessible decision modal with 15-character minimum rejection validator, and toast confirmation.
- **Operational Analytics (`AnalyticsDashboard.js`):** Migrated to full Light/Dark design-system tokens, added `<figure>`/`<figcaption>` screen-reader summaries and `tabular-nums`, preserved department scope locking and lazy route splitting.
- **Cryptographic Audit Ledger (`AuditLedger.js`):** Added actor/action/target search filter, responsive table wrapper, and Light/Dark adaptive verification banner.
