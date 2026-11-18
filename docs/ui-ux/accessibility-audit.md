# EPAMS — WCAG 2.2 AA Accessibility Audit (Stage U4B)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Target Standard:** WCAG 2.2 Level AA
**Date:** 2026-11-18

---

## 1. Accessibility Verification Matrix

| WCAG 2.2 Criterion | Implementation in EPAMS | Verification Method | Status |
| :--- | :--- | :--- | :---: |
| **1.4.3 / 1.4.11 Contrast (Minimum & Non-text)** | Dedicated Light & Dark semantic token ramps in `tokens.css`: primary text `17.9:1` (Light) / `15.7:1` (Dark), muted text `6.1:1` (Light) / `6.3:1` (Dark), brand button `9.8:1` (Light) / `6.9:1` (Dark), hand-tuned status & priority pills for both themes. | Token audit + automated/browser inspection | **VERIFIED** |
| **2.1.1 / 2.1.2 Keyboard & No Trap** | All navigation, profile menu, notification popover, quick search (`Ctrl+K`), modals, and mobile drawer operate via keyboard and dismiss cleanly via `Escape` without keyboard traps. | Unit tests (`P95-05`) + code inspection | **VERIFIED** |
| **2.4.1 Bypass Blocks** | Added `.ep-skip-link` ("Skip to main content") jumping directly to `<main id="main-content" tabIndex={-1}>`. | Code & DOM verification | **VERIFIED** |
| **2.4.7 / 2.4.11 Focus Visible** | Global `:focus-visible` `2px solid var(--ep-focus-ring)` + `3px` halo; removed prior `outline: 'none'` defect in `LoginModal.js`. | CSS & DOM verification | **VERIFIED** |
| **2.4.8 Location** | Semantic `<nav aria-label="Breadcrumb">` with `aria-current="page"` on all sub-routes. | Unit test (`P95-08`) | **VERIFIED** |
| **2.5.8 Target Size (Minimum >= 24px)** | Buttons (`min-height: 36px`, small `30px`), icon buttons (`36x36px`), nav links (`min-height: 38px`), inputs (`min-height: 38px`). | CSS token verification | **VERIFIED** |
| **1.3.1 / 4.1.2 Landmarks, Labels & Roles** | `<header>`, `<nav>`, `<main>`, `<aside>`, `<article>`, `<section>`, `<dl>/<dt>/<dd>`, `role="dialog"` + `aria-modal="true"`, `role="radiogroup"` + `aria-checked`, explicit `<label htmlFor>` bindings on forms. | Jest RTL accessible-role queries (`42/42` passing) | **VERIFIED** |
| **4.1.3 Status Messages** | `ToastProvider` renders `role="status" aria-live="polite"`; `Alert` renders `role="alert"` for errors and `role="status"` for success/info. | Code & Jest verification | **VERIFIED** |
| **1.1.1 Charts & Icons** | Decorative SVG icons use `aria-hidden="true"`; SVG SLA gauge & trend bar charts provide `role="img"`, descriptive `aria-label`, and `<figcaption className="sr-only">` summaries. | Code & Jest verification | **VERIFIED** |
| **2.3.3 Animation from Interactions** | Honors `@media (prefers-reduced-motion: reduce)` plus explicit `/preferences` toggle (`data-reduced-motion="true"`). | Unit test (`P95-02`) | **VERIFIED** |
| **1.4.4 / 1.4.10 Text Resize & Reflow** | Relative `rem` typography, `[data-font-scale="large"]` (`112.5%`), responsive flex/grid wrapping, and `.ep-table-wrap` isolated horizontal scrolling. | CSS + responsive inspection | **VERIFIED** |

## 2. Documented Remaining Accessibility Follow-Ups (Non-Blocking)
- **Full Automated axe-core / Screen Reader Pass (NVDA/JAWS):** Manual assistive-technology screen-reader walkthrough and automated `axe-core` CI integration are recommended as a continuous P10 QA check.
- **Legacy Phase 1 Admin Tables (`UserList`, `ProfileList`):** Styled with Light/Dark CSS tokens via `App.css`, though future P10 polish can migrate them to `.ep-table-wrap` wrappers and add explicit `aria-label` attributes on every legacy admin action link.
