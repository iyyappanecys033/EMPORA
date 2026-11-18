# EPAMS — P9.6.1 Remaining Gap Register (Stage A Input → Stage B–F Closure Tracker)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6.1 — QA Closure & Independent Evidence Audit  
**Created:** 2026-11-18 (Stage A Baseline)

---

## 1. P9.6.1 Closure Gaps — Final Disposition (Post-Stage F)

| Gap ID | Severity | Category | Baseline Status (Stage A) | Final P9.6.1 Outcome | Final Status |
| :--- | :---: | :--- | :--- | :--- | :---: |
| `GAP-961-01` (`QA-DEF-006`) | **Low** | Accessibility (`WCAG 2.2 AA target-size`) | Open — `axe-core` flagged `320×700` topbar | Fixed in `Navbar.js` & `components.css`; **`32 / 32` Light/Dark `axe-core 4.11.4` scans (`0` violations, `0` overflow, `34–44 px` targets)** | **CLOSED (VERIFIED)** |
| `GAP-961-02` | **High / Mod** | Direct Frontend Runtime Dependencies | Open — `axios@1.16.0` & `react-router-dom@6.30.3` | Patched `axios` → **`1.21.1` (`0` advisories)** and `react-router-dom` → **`6.30.6` (`GHSA-jjmj-jmhj-qwj2` resolved)**; `2` non-applicable v7-only moderate `react-router` advisories documented | **PARTIALLY CLOSED (DIRECT HIGH = 0)** |
| `GAP-961-03` | **Medium** | Critical Frontend Component Test Coverage | Open — `56.20%` L / `50.03%` B (`TicketDetail` `1.1%`, `EmployeeDashboard` `8.5%`) | Added `P961CriticalFrontendCoverage.test.js` (`70/70` Jest); aggregate **`73.09%` L / `67.02%` B**; critical modern workspace (`37` files) **`84.80%` L / `70.40%` B** | **SUBSTANTIALLY CLOSED (LINE > 80%; BRANCH GAP DOCUMENTED)** |
| `GAP-961-04` | **Medium** | Cross-Browser Verification | `NOT TESTED` — Firefox & WebKit missing | Executed Playwright `1.57.0` 10-journey smoke suite across **Chromium `157.0.8103.79` (`10/10`), Firefox `144.0.2` (`10/10`), WebKit `26.0` (`10/10`) = `30 / 30` PASS**; caught & fixed `QA-DEF-007` | **CLOSED (VERIFIED)** |
| `GAP-961-05` | **Low** | Evidence & Test-Count Reconciliation Precision | Minor count/artifact nuances | Reconciled primary Surefire XMLs (`229 + 7 = 236`), JaCoCo XML (`3,418/3,956`), and persisted Jest JSON coverage | **CLOSED (VERIFIED)** |

---

## 2. Structural / External Environment Gaps (Tracked for P10 / Post-P9.6.1)

| Gap ID | Severity | Category | Classification | Recommended Action Window |
| :--- | :---: | :--- | :---: | :--- |
| `GAP-P10-01` | **Medium** | Transitive `react-scripts@5.0.1` CRA build-toolchain advisories (`103` dev-only advisories; `0` emitted to production browser bundle) | **DOCUMENTED / DEFERRED** | Planned Vite + Vitest migration sprint (cannot be fixed via `npm audit fix` without breaking CRA) |
| `GAP-P10-02` | **Medium** | Manual Assistive Technology Screen-Reader Audit (NVDA / JAWS / VoiceOver) | **NOT TESTED** | Pre-GA UAT with real screen readers |
| `GAP-P10-03` | **Medium** | External SCA / SAST / DAST (`trivy`, `semgrep`, CodeQL, OWASP ZAP) | **NOT TESTED** | Enable in GitHub Actions CI & staging environment |
| `GAP-P10-04` | **Medium** | Production MySQL TLS (`sslMode=VERIFY_IDENTITY`), disable `createDatabaseIfNotExist=true`, gate `/swagger-ui/**`, HSTS header at edge, multi-hour staging soak test | **P10 DEPLOYMENT SCOPE** | Mandatory P10 production configuration & staging readiness checklist |
| `GAP-P10-05` | **Low** | Legacy pre-P9.5 CRUD admin components (`UserList.js`, `ProfileForm.js`, `RoleForm.js`, `UserForm.js`, etc. at `2%`–`7%` Jest coverage) | **DOCUMENTED** | Covered by backend MockMvc integration tests; low frontend priority compared to modern employee/resolver/manager workspace |
