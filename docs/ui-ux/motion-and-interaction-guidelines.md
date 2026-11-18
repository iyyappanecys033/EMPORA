# EPAMS — Motion & Interaction Guidelines (Stage U4)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Source Files:** `src/styles/design-system/tokens.css`, `components.css`, `src/services/PreferencesContext.js`, `src/services/ToastContext.js`
**Date:** 2026-11-18

---

## 1. Motion Philosophy
Motion in EPAMS is calm, brief, and strictly functional—reinforcing spatial hierarchy and feedback without slowing down high-frequency employee or specialist workflows:
- **Standard Easing:** `cubic-bezier(0.2, 0, 0, 1)` (`--ep-ease`).
- **Durations:** `--ep-duration-fast` (`150ms`) for hover, focus, dropdowns, button press, and backdrops; `--ep-duration-normal` (`220ms`) for sidebar collapse/expand, modals, and toast slide-in.
- **Explicit Prohibitions:** Zero bouncy springs, zero parallax, zero flashing effects, zero autoplaying decorative animations.

## 2. Implemented Micro-Interactions
- **Sidebar Collapse/Expand:** Smooth `220ms` width transition (`248px` ↔ `72px`) with instant tooltip exposure in compact mode.
- **Modals & Popovers (`ep-fade-in` / `ep-scale-in`):** Subtle `4px` entry scale on `LoginModal`, `CreateTicketModal`, `GlobalSearchModal`, `NotificationBell` popover, and Profile dropdown.
- **Toast Announcements (`ep-slide-up`):** Non-blocking bottom-right `aria-live="polite"` toast notifications on ticket submission, assignment, approval decision, and preference updates.
- **Loading Skeletons (`ep-shimmer`):** Layout-preserving shimmer skeletons (`DashboardSkeleton`, `CardGridSkeleton`, `TableSkeleton`) replacing layout-shifting spinners.
- **Interactive Cards & Buttons:** Subtle `1px` active press feedback and calm border/shadow elevation on hover.

## 3. Reduced-Motion Accessibility (`WCAG 2.3.3`)
- Automatically respects `@media (prefers-reduced-motion: reduce)`.
- Users can explicitly toggle **Follow OS / Reduce Motion / Standard Motion** in `/preferences`, setting `data-reduced-motion="true"` on `<html>` which clamps all animation and transition durations to `0.01ms !important` across the entire application.
- No workflow state or critical feedback depends on animation to be understood.
