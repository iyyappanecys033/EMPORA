# EPAMS — Design System Specification (Stage U1)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Identity:** EPAMS — Modern Enterprise Workspace
**Source Files:** `springapp/springbackend/src/styles/design-system/tokens.css`, `components.css`, `src/services/PreferencesContext.js`, `src/services/ToastContext.js`
**Date:** 2026-11-18

---

## 1. Semantic Color Token Architecture (Light & Dark Modes)

All colors are defined exclusively as CSS custom properties in `tokens.css` with separate, hand-crafted Light (`:root` / `[data-theme="light"]`) and Dark (`[data-theme="dark"]`) surface ramps (never inverted Light styles):

| Token Role | Light Mode Value | Dark Mode Value | WCAG 2.2 AA Pairing Check |
| :--- | :--- | :--- | :--- |
| `--ep-primary` / `--ep-text-on-brand` | `#3730a3` / `#ffffff` | `#818cf8` / `#0b1120` | `9.8:1` (Light) / `6.9:1` (Dark) |
| `--ep-secondary` | `#0f766e` | `#2dd4bf` | High-contrast secondary teal accent |
| `--ep-bg-page` | `#f4f6fa` | `#0b1120` | Calm workspace canvas |
| `--ep-bg-surface` / `--ep-bg-elevated` | `#ffffff` / `#ffffff` | `#111827` / `#1e293b` | Layered MD3/Fluent 2 surface hierarchy |
| `--ep-text-primary` on `--ep-bg-surface` | `#0f172a` on `#ffffff` | `#f1f5f9` on `#111827` | `17.9:1` (Light) / `15.7:1` (Dark) |
| `--ep-text-secondary` on `--ep-bg-surface` | `#334155` on `#ffffff` | `#cbd5e1` on `#111827` | `10.1:1` (Light) / `11.2:1` (Dark) |
| `--ep-text-muted` on `--ep-bg-surface` | `#526071` on `#ffffff` | `#94a3b8` on `#111827` | `6.1:1` (Light) / `6.3:1` (Dark) |
| `--ep-border` / `--ep-border-strong` | `#e2e8f0` / `#cbd5e1` | `#263248` / `#3b4b66` | Clear component boundaries |
| `--ep-focus-ring` | `#4f46e5` + `3px` halo | `#a5b4fc` + `3px` halo | WCAG 2.2 `>= 2px` visible focus outline |
| Semantic Statuses (`success`, `warning`, `danger`, `info`) | Deep text on tinted pastel surfaces | Luminous text on alpha-tinted dark surfaces | Dedicated `11` ticket-status & `5` priority token triplets |

---

## 2. Three-Mode Theme Engine & Zero-FOUC Boot

- Supported modes: `light`, `dark`, `system` (synced live via `matchMedia('(prefers-color-scheme: dark)')`).
- Pre-paint bootstrap script in `public/index.html` resolves `epams.preferences` from `localStorage` and stamps `data-theme`, `data-density`, `data-font-scale`, and `data-reduced-motion` on `<html>` before first paint — preventing any white flash in Dark mode.

---

## 3. Typography, Spacing & Density System

- **Font Stack:** `Inter`, system UI sans (`--ep-font-sans`) + `JetBrains Mono` / monospace (`--ep-font-mono`) for ticket references, employee IDs, item codes, and SHA-256 hashes; `font-variant-numeric: tabular-nums` (`.ep-tabular`) for KPIs and timestamps.
- **Scale:** `--ep-text-xs` (`12px`) → `--ep-text-2xl` (`26px`) with `clamp()` responsive page titles and `[data-font-scale="large"]` (`112.5%` root zoom).
- **Spacing & Density:** `4px` base scale (`--ep-space-1` to `--ep-space-8`); `[data-density="compact"]` tightens table cell padding (`8px 12px`), card padding (`14px`), and section gaps (`16px`).

---

## 4. Reusable Component Library (`src/components/common/`)

| Component | Purpose & States |
| :--- | :--- |
| `Icons.js` | Crisp inline stroke SVG icon library (`27` icons, `aria-hidden="true"`). |
| `Avatar.js` | Deterministic monogram avatar fallback with enterprise color palette. |
| `Badge.js` | Tokenized `StatusBadge` (all 11 lifecycle states) and `PriorityBadge` (all 5 tiers). |
| `Alert.js` | Accessible RFC 7807 `role="alert"` / `role="status"` banner with field error list and dismiss. |
| `ToastContext.js` | `aria-live="polite"` toast stack for non-blocking workflow confirmations. |
| `LoadingSkeleton.js` & `LoadingSpinner.js` | Structured shimmer skeletons (`DashboardSkeleton`, `CardGridSkeleton`, `TableSkeleton`) and accessible spinner. |
| `StatusTimeline.js` | 11-state visual progress stepper + contextual next-step guidance. |
| `Breadcrumb.js` | Dynamic `<nav aria-label="Breadcrumb">` trail. |
| `GlobalSearchModal.js` | `Ctrl+K` quick-jump search across authorized routes, catalog offerings, and recent tickets. |
| `Pagination.js` & `EmptyState.js` | Semantic tokenized pagination and actionable empty states. |
| CSS Primitives (`components.css`) | `.ep-btn` variants, `.ep-icon-btn`, `.ep-input`/`.ep-select`/`.ep-textarea`, `.ep-card`, `.ep-table-wrap`/`.ep-table`, `.ep-modal`, `.ep-dropdown`, `.ep-pill-group`, `.ep-filter-bar`. |
