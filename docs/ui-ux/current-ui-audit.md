# EPAMS — Current UI/UX Baseline Audit (Stage U0)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Baseline Commit:** `b2650baa` (`main`)
**Frontend Stack Verified:** React `18.2.0`, React Router `6.11.1`, Axios `1.4.0`, `react-scripts` `5.0.1`, Jest + React Testing Library (`34/34` passing)
**Backend Baseline Verified:** Java `25.0.2`, Spring Boot `3.5.16`, MySQL `8.0.45` verified (`208` H2 + `7` MySQL tests)
**Date:** 2026-11-18

---

## 1. Frontend Architecture Inventory (`VERIFIED` from Source)

| Layer | Existing Files Inspected | Baseline Observation |
| :--- | :--- | :--- |
| **Entry & Shell** | `public/index.html`, `src/index.js`, `src/index.css`, `src/App.js`, `src/App.css` | `<title>SpringApp</title>` still present; no pre-paint theme script; legacy Phase 1 `.navbar`/`.card` CSS coexists with inline-styled P7/P8 components; zero design tokens. |
| **Auth & Services** | `src/services/api.js`, `AuthContext.js`, `ticketService.js`, `notificationService.js`, `analyticsService.js` | Solid cookie JWT + `X-XSRF-TOKEN` + silent 401 refresh + RFC 7807 `formatApiError`; **missing** profile self-service helper (`GET /api/profiles/me` exists on backend `ProfileController.java:116` but unused in employee UI!). |
| **Layout** | `components/layout/AppLayout.js`, `Navbar.js`, `Sidebar.js` | Fixed `230px` sidebar with raw emojis, no collapse mode, no mobile drawer; top bar lacks breadcrumbs, global search, theme switcher, and profile menu; 100% hardcoded light hex values. |
| **Common Primitives** | `Alert.js`, `Badge.js`, `EmptyState.js`, `LoadingSpinner.js`, `Pagination.js` | Duplicate `EmptyState` exported inside `LoadingSpinner.js`; inline `<style>` tag injected per spinner; no `Skeleton`, `Toast`, `Modal`, `Drawer`, `Avatar`, `StatusTimeline`, `Breadcrumb`, or `Tabs`. |
| **Employee Workspace** | `EmployeeDashboard.js`, `ServiceCatalog.js`, `CreateTicketModal.js`, `MyTickets.js`, `TicketDetail.js`, `TicketComments.js`, `LoginModal.js` | Functional API wiring; **defects found:** hardcoded demo credentials (`admin/admin123`, `john_doe/user123`) in `LoginModal.js`, `outline: 'none'` breaking keyboard focus, no 11-state lifecycle timeline on `TicketDetail`, no category filter chips on `ServiceCatalog`, no priority/sort/card-view on `MyTickets`, no `/profile` or `/preferences` views, no notification preview on `EmployeeDashboard`. |
| **Manager / Ops / Admin** | `DepartmentQueue.js`, `ApprovalQueue.js`, `NotificationBell.js`, `NotificationsPage.js`, `AnalyticsDashboard.js` (lazy), `AuditLedger.js` (lazy), Phase 1 IAM views (`Dashboard.js`, `UserList`, `RoleList`, `ProfileList`, `UserRoleMapping`, `AuditLogs`) | Functional RBAC & department scope; tables lack responsive scroll containers (`overflow-x: auto`), `DepartmentQueue` lacks search/priority/age/SLA indicators, `AuditLedger` lacks client-side filtering & structured hash copy/details, `AnalyticsDashboard` lacks dark-mode tokens and accessible chart summaries. |

---

## 2. Pre-Transformation Route & Screenshot Inventory

| Route | Accessible Roles | Pre-Transformation Baseline Status |
| :--- | :--- | :--- |
| `/` (Guest & Employee Dashboard) | Public / All Auth | Hardcoded light cards; guest screen has basic blue box; lacks notifications preview, quick profile/preferences actions, and identity avatar. |
| `/catalog` | Public / All Auth | Department dropdown + search only; no category pills; hardcoded light cards. |
| `/tickets` | Authenticated | Fixed table without overflow wrapper; no priority filter, sorting, or card/list toggle. |
| `/tickets/:id` | Requester / Resolver / Admin | Flat metadata block; no 11-state visual progress timeline; raw emoji metadata icons. |
| `/notifications` | Authenticated | Functional filter & pagination; hardcoded light borders/backgrounds. |
| `/approvals` | Manager / HR Manager / Dept Head / Admin | Functional anti-self-approval guard; plain white cards with emoji icons; modal lacks `role="dialog"` and Escape dismiss. |
| `/queue/:department` | Resolver / Specialists / Dept Head / Admin | Missing search, priority filter, ticket age, SLA status indicators, and horizontal scroll wrapper. |
| `/analytics` (lazy) | Admin / Auditor / Manager / Resolver | Strong KPIs & SVG charts, but hardcoded light colors and missing `<figure>` / SR summary alternatives. |
| `/audit-chain` (lazy) | Auditor / Admin | Functional verify button & table; lacks search/action filter and responsive scroll container. |
| `/profile` | *(Missing in baseline)* | **Not implemented** despite `GET /api/profiles/me` existing on backend. |
| `/preferences` | *(Missing in baseline)* | **Not implemented** (no theme, density, reduced-motion, or font-scale controls). |
| `/admin/metrics`, `/users`, `/roles`, `/profiles`, `/user-role-mappings`, `/audit` | Admin | Legacy Phase 1 CSS (`.card.blue`, unstyled raw tables) clashes with P7/P8 shell and breaks dark mode. |

---

## 3. Baseline Production Build & Test Snapshot

- **Jest Suite:** `3` suites, `34 / 34` passing (`App.test.js`, `P8Features.test.js`, `P9FrontendQa.test.js`).
- **Production Bundle (gzip):** `main.4914abd4.js` (`93.97 kB`), `555.chunk.js` (`4.78 kB` Analytics), `467.chunk.js` (`2.40 kB` Audit), `main.d3e18450.css` (`1.31 kB`).
- **Backend Regression Baseline:** `208 / 208` H2 passing + `7 / 7` real MySQL `8.0.45` passing.
