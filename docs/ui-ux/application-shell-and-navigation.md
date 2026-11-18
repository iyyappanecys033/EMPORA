# EPAMS — Application Shell & Navigation Architecture (Stage U2)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Components:** `AppLayout.js`, `Navbar.js`, `Sidebar.js`, `Breadcrumb.js`, `GlobalSearchModal.js`
**Date:** 2026-11-18

---

## 1. Navigation Architecture & Role-Based Grouping

The collapsible sidebar strictly mirrors `AuthContext` authorization flags so users never encounter unauthorized dead-end menu links:

| Group | Routes | Visibility |
| :--- | :--- | :--- |
| **WORKSPACE** | Dashboard (`/`), Service Catalog (`/catalog`), My Tickets (`/tickets`), Notifications (`/notifications`) | Public (`/` & `/catalog`); Authenticated (`/tickets`, `/notifications`) |
| **INSIGHTS** | Operational Analytics (`/analytics`) | `canViewAnalytics` (Admin, Auditor, Approver, Resolver) |
| **APPROVALS** | Pending Approvals (`/approvals`) | `isApprover` (Manager, HR Manager, Dept Head, Admin) |
| **SPECIALIST QUEUE** | Department Queue (`/queue/:department`) | `isResolver` (IT Support, Facilities Crew, HR Specialist, Dept Head, Admin) |
| **AUDIT & SECURITY** | Cryptographic Ledger (`/audit-chain`) | `isAuditor` (Auditor, Admin) |
| **ACCOUNT** | My Profile (`/profile`), Preferences (`/preferences`) | Authenticated |
| **ADMINISTRATION** | System Metrics, Users, Roles, Profiles, Role Mappings, Legacy Audit | `isAdmin` |

## 2. Collapsible Desktop Rail & Mobile Drawer

- **Desktop Expanded (`248px`):** Full stroke SVG icons + labels + section headers + collapse button.
- **Desktop Compact (`72px`):** Centered icons with native `title` tooltips and `aria-label` attributes; state persisted via `PreferencesContext` (`sidebarCollapsed`).
- **Tablet & Mobile (`<= 1024px`):** Desktop rail hides cleanly; top bar exposes an accessible hamburger button toggling a slide-over `role="dialog"` drawer with backdrop click and `Escape` dismiss.

## 3. Sticky Top Bar, Breadcrumbs & Global Quick Search

- **Top Bar:** Mobile drawer toggle, EPAMS brand mark, Quick Search button (`Ctrl+K`), one-click Theme Cycle button, `NotificationBell` (with unread badge & popover), and Profile Dropdown Menu (`My Profile`, `Preferences & Accessibility`, `Service Help & Catalog`, inline `Light / Dark / System` theme switcher, `Sign Out`).
- **Breadcrumbs:** Automatically renders `Home / Section / Detail` above every non-root route with `aria-current="page"`.
- **Quick Search (`GlobalSearchModal.js`) — Honest Scope Disclosure:** Because no backend enterprise full-text search endpoint exists, Quick Search transparently searches role-authorized navigation routes, loaded Service Catalog items (`GET /api/v1/catalog`), and the signed-in user's recent tickets (`GET /api/v1/tickets/my`). No fake search results are fabricated.
