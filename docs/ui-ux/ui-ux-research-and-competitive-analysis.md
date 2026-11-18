# EPAMS — UI/UX Research & Competitive Analysis (Stage U0)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.5 — Enterprise UI/UX Transformation
**Date:** 2026-11-18

---

## 1. Reference Benchmarks & Core Principles Studied

Rather than imitating any single vendor's visual skin, EPAMS synthesizes proven enterprise usability patterns from five authoritative references into an original **EPAMS — Modern Enterprise Workspace** identity:

| Reference | Primary Strengths Studied | How Adapted to EPAMS |
| :--- | :--- | :--- |
| **ServiceNow Employee Center** | Unified "single front door" portal; task-first employee home ("My Active Items"); intent-to-action service discovery; contextual breadcrumbs; clear approval cards | Personalized greeting + identity summary + "Needs Attention" action strip + quick service shortcuts + recent tickets + notifications preview; category/department service discovery |
| **Zoho People 5.0** | Calm left-rail navigation with collapsible icon-rail mode; clean employee self-service profile cards; fast inline filtering and density controls; accessible self-service preferences | Collapsible sidebar (expanded `248px` / compact `72px` with tooltips / mobile drawer); dedicated `/profile` (`GET /api/profiles/me`) and `/preferences` workspaces |
| **Microsoft Fluent 2** | Crisp token architecture (`surface`, `surface-elevated`, `border`, `brand`, `neutral`); subtle elevation without visual clutter; responsive modular card grids; smooth restrained transitions (`150ms–220ms`) | Full CSS custom-property token architecture in `src/styles/design-system/` supporting Light, Dark, and System themes with zero flash of wrong theme |
| **Google Material Design 3** | Tonal surface hierarchy (separate Dark surfaces `#0b1120` → `#111827` → `#1e293b` rather than inverted light colors); explicit state layers (`hover`, `focus`, `active`, `disabled`, `selected`); clear status semantics | Hand-crafted Light and Dark surface ramps; semantic badge tokens for all 11 ticket states and 5 priorities; skeleton loading states |
| **WCAG 2.2 AA** | Minimum `4.5:1` text contrast & `3:1` UI contrast; visible `>= 2px` focus rings (`:focus-visible`); `>= 24x24px` touch targets; keyboard modal/drawer Escape & focus management; `aria-live` status announcements; `prefers-reduced-motion`; responsive reflow down to `320px` | Global `:focus-visible` ring token; skip-to-main-content link; semantic landmarks (`header`, `nav`, `main`); `aria-live="polite"` toast/status region; reduced-motion media query + explicit user preference toggle |

---

## 2. Twelve-Dimension Enterprise UX Analysis

1. **Navigation Architecture:** Left navigation rail grouped logically (`Workspace`, `Service Desk & Ops`, `Insights & Governance`, `Administration`) with role-gated visibility so users never see forbidden dead ends, paired with a sticky top bar containing mobile toggle, breadcrumbs, global quick-jump search (`Ctrl+K` / `/`), notification bell, theme/profile dropdown.
2. **Employee Home Experience:** Shifts from a generic admin panel to an action-oriented daily hub: time-aware greeting, employee identity chip (`designation • department • employeeId`), quick action shortcuts (New Request, My Tickets, Approvals if approver, Notifications, My Profile), live active-ticket list, featured services, and recent unread notifications preview.
3. **Personalisation:** Dedicated `/preferences` workspace persisting Theme (`Light` / `Dark` / `System`), Density (`Comfortable` / `Compact`), Reduced Motion (`System` / `On` / `Off`), and Font Scale (`Standard` / `Large`) in browser `localStorage` (explicitly labeled as device/browser-local since no backend preference API exists).
4. **Search & Service Discovery:** Top-bar quick-jump palette searching supported EPAMS workspaces, loaded catalog services, and recent tickets (honestly documented as client-assisted search over supported APIs, not a fake enterprise search index) plus category chips + department filter + keyword search in Service Catalog.
5. **Employee Profiles:** Dedicated `/profile` screen backed by real `GET /api/profiles/me` displaying generated monogram avatar, identity header, work & organizational metadata, contact details, and role summary — strictly respecting backend RBAC (employees view read-only self-profile with a "Request Profile Update" shortcut to Service Catalog; Admin/HR retain full edit flows).
6. **Notifications:** Accessible bell popover (`Escape` dismiss, unread count badge, quick mark-all-read) + upgraded Notification Center with filter tabs, unread indicator dots, deep links to tickets/approvals, and live announcements.
7. **Task & Approval Experiences:** Approval Center shows clear stage badges (`STAGE 1 MANAGER`, `STAGE 2 SECONDARY`), requester identity, creation age, anti-self-approval guardrail callout, confirmation modal with character-counter validation, and immediate feedback.
8. **Data Tables & Filtering:** Controlled horizontal overflow wrappers (`overflow-x: auto`) so complex tables never break page reflow at `390px`/`320px`; density-aware row padding; card/table view toggle on `MyTickets`; status/priority/search/sort controls; backend pagination totals (`totalElements`).
9. **Dashboard Information Hierarchy:** Clear typographic hierarchy (`clamp()` fluid headings, `tabular-nums` for KPI figures), subtle left-accent KPI cards, accessible SVG charts with screen-reader summaries and dark-mode adaptive tracks.
10. **Responsive Behaviour:** Breakpoints at `1440px`, `1366px`, `1024px`, `768px`, `390px`, `320px`; sidebar becomes a slide-over accessible drawer below `1024px` with backdrop click and Escape dismiss.
11. **Accessibility:** Full keyboard operability, skip link, semantic `<dialog>`/`role="dialog"` modals, `aria-label` on icon buttons, high-contrast status pills in both Light and Dark themes.
12. **Interaction & Motion Design:** Restrained cubic-bezier easing (`150ms` micro, `220ms` surfaces), subtle card hover elevation, skeleton shimmer disabled automatically when `prefers-reduced-motion: reduce` or user Reduced Motion is active.
