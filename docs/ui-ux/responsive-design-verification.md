# EPAMS — Responsive Design Verification (Stage U4B)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Target Breakpoints:** Desktop `1440px`, Laptop `1366px`, Tablet `768px`, Mobile `390px`, Narrow Mobile `320px`
**Date:** 2026-11-18

---

## 1. Breakpoint Architecture (`components.css`)

| Breakpoint | Layout Behaviour | Overflow Protection |
| :--- | :--- | :--- |
| **Desktop (`1440px`)** | Full expanded `248px` sidebar (or `72px` compact rail), centered `max-width: 1440px` workspace canvas, multi-column KPI/service/analytics grids, full top-bar quick search (`Ctrl K`) and user identity block. | Zero page overflow (`overflow-x: clip`). |
| **Laptop (`1366px`)** | Same spacious desktop layout; responsive `repeat(auto-fit, minmax(...))` grids adapt seamlessly without cramped columns. | Controlled table wrappers. |
| **Tablet (`<= 1024px`, e.g., `768px`)** | Desktop sidebar hides; hamburger toggle activates slide-over `268px` drawer (`max-width: 85vw`) with backdrop & `Escape` dismiss; brand subtitle collapses to maximize search & actions space. | Zero horizontal page scroll; data tables scroll internally inside `.ep-table-wrap`. |
| **Mobile (`<= 768px`, e.g., `390px`)** | Top bar condenses padding (`0 12px`), Quick Search compacts to icon trigger, page padding tightens (`14px 12px`), `MyTickets` offers 1-click Card View for comfortable vertical reading, modals fit viewport (`max-height: 90vh; overflow-y: auto`). | Zero horizontal page scroll; all touch targets `>= 30–36px`. |
| **Narrow Mobile (`<= 420px`, e.g., `320px`)** | User role subtitle hides in top-bar pill (`.ep-user-meta-hide-xs`) keeping avatar, notifications, theme toggle, search, and menu cleanly on a single `60px` header row; modal padding tightens (`16px`). | Zero overlapping header controls; `StatusTimeline` scrolls smoothly inside its own horizontal container. |

## 2. Live Verification Record
- Verified CSS responsive rules, grid min-widths, and `.ep-table-wrap` isolation across Light and Dark themes.
- Live Chrome viewport checks (Desktop `1440px` Light/Dark + Mobile `390px`) are recorded in `docs/ui-ux/ui-ux-test-report.md` after browser verification executes.
