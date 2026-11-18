# EPAMS — P9.6.1 Final QA Closure & Release Readiness Report (Stage G)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6.1 — QA Closure & Independent Evidence Audit  
**Closure Date:** 2026-11-18  
**Git Baseline:** Branch `main`, HEAD commit `a4857679c0bde310e4b705a46d7e7ef701bc3901` *(all P9.6 + P9.6.1 changes remain uncommitted awaiting review)*  
**Flyway `V1`–`V4` Integrity:** **ZERO MODIFICATIONS** (`git diff -- springapp/src/main/resources/db/migration/` = empty)

---

## Final Release Readiness Verdict

### **`CONDITIONALLY APPROVED`**

**Justification:**  
Every defect and actionable verification gap identified in P9.6 and Stage A has been **closed and independently verified with primary-source evidence**:
- **Zero open defects** across Critical, High, Medium, and Low severities (`QA-DEF-001`–`QA-DEF-007` all fixed and re-verified, including `QA-DEF-006` `320×700` target-size and newly discovered `QA-DEF-007` `LoginModal` error-wipe bug).
- **100% automated test pass rate** across Backend (`229` default H2 + `7` opt-in real MySQL `8.0.45` = `236 / 236`), Frontend (`70 / 70` across `6` suites), Live Accessibility (`32 / 32` Light & Dark `axe-core 4.11.4` scans with `0` violations & `0` overflow), Packaged JAR SPA Deep-Links (`11 / 11` HTTP `200`), and **3-Engine Cross-Browser Smoke Suite** (`30 / 30`: Chromium `157.0.8103.79` `10/10`, Firefox `144.0.2` `10/10`, WebKit `26.0` `10/10`).
- **Direct runtime `axios` patched (`1.16.0` → `1.21.1`)** to **zero advisories**, and **`react-router-dom` patched (`6.30.3` → `6.30.6`)** resolving `GHSA-jjmj-jmhj-qwj2`.

An honest verdict of **`CONDITIONALLY APPROVED`** (rather than unconditional `APPROVED`) is maintained strictly because **zero-vulnerability and complete-accessibility certification cannot be claimed** while the following external/structural items remain for P10 / post-P9.6.1:
1. Manual assistive technology testing (**NVDA / JAWS / VoiceOver**) was not executed locally.
2. External SCA/SAST/DAST scanners (**Trivy / Semgrep / CodeQL / OWASP ZAP**) are not installed locally and must execute in CI/staging.
3. `102` residual `npm audit` advisories remain (`100` transitive dev/build-only under archived `react-scripts@5.0.1` + `2` SemVer-major v7-only moderate advisories in `react-router` verified non-applicable/unreachable in EPAMS SPA).
4. Frontend branch coverage (`67.02%` aggregate; `70.40%` modern workspace) and backend branch coverage (`66.62%`) remain below an aspirational `80%` branch stretch goal (while line coverage exceeds `80%` on both backend `86.40%` and modern critical frontend `84.80%`).
5. Production P10 deployment hardening (MySQL TLS `sslMode=VERIFY_IDENTITY`, disabling `createDatabaseIfNotExist=true` & Swagger UI, HSTS at ingress, and multi-hour staging soak test) remains mandatory in P10.

---

## 1. Verified Baseline (Stage A Independent Primary-Source Audit)

Full primary-source audit recorded in [p961-independent-evidence-audit.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/qa/p961-independent-evidence-audit.md):
- **Git State:** Branch `main`, commit `a4857679c0bde310e4b705a46d7e7ef701bc3901`, zero unexpected production-code edits, zero Flyway `V1`–`V4` diffs.
- **Reconciled Stage A Discrepancies:**
  - Confirmed Surefire `skipped=1` on default run represents **`1` opt-in test *class* (`Stage2RealMysqlIntegrationTests`) containing `7` test methods** (attached by Surefire `3.0.0-M5` to `TEST-com.examly.springapp.TicketPersistenceTests.xml`), not 1 skipped method.
  - Corrected JaCoCo XML line count from prose `3,419/3,957` to exact XML **`3,418 / 3,956` (`86.40%`)**.
  - Persisted missing Jest JSON coverage artifacts (`springapp/springbackend/coverage/coverage-final.json`) and exposed severe Stage A gaps in `TicketDetail.js` (`1.1%` L) and `EmployeeDashboard.js` (`8.5%` L), subsequently remediated in Stage D.

---

## 2. Defects Fixed (P9.6 + P9.6.1 — `7 / 7` Closed)

| Defect ID | Severity | Phase Fixed | Summary | Primary Verification Evidence |
| :--- | :---: | :---: | :--- | :--- |
| `QA-DEF-001` | **High** | P9.6 | Added 11 modern SPA routes to `SpaController` & `SecurityConfig` | `11/11` packaged JAR routes HTTP `200` (`task-5004`) |
| `QA-DEF-002` | **High** | P9.6 | Added role-aware `sortBy` allowlist in `ProfileController` (blocks `sortBy=salary` oracle & HTTP 500) | `P96ApiAndFieldValidationTests` & `P96RbacFieldSecurityIdorTests` |
| `QA-DEF-003` | **Medium** | P9.6 | Added Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Email`, `@PositiveOrZero`) to auth/profile DTOs & controllers | `P96ApiAndFieldValidationTests` (`229/229` backend pass) |
| `QA-DEF-004` | **Medium** | P9.6 | Added RFC 7807 `415`/`400`/`404` handlers in `GlobalExceptionHandler`; stripped raw `e.getMessage()` from HTTP `500` | `P96ApiAndFieldValidationTests` |
| `QA-DEF-005` | **Medium** | P9.6 | Fixed `@{jacoco.argLine}` & upgraded JaCoCo `0.8.13` → `0.8.14` for Java `25` | `mvn package -o "-Dskip.npm=true" -Pcoverage` (`114` classes) |
| **`QA-DEF-006`** | **Low** | **P9.6.1 (Stage B)** | Fixed flexbox compression of compact `.ep-topbar` controls at `320×700` in [Navbar.js](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/components/layout/Navbar.js) & [components.css](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/styles/design-system/components.css) (`flex-shrink: 0`, `min-width/height: 36px`, visible `:focus-visible`, `@media (max-width: 420px)` compact spacing) | **`32 / 32` Light & Dark `axe-core 4.11.4` scans (`0` violations, `0` overflow, all `320px` targets `34–44 px`)** |
| **`QA-DEF-007`** | **High** | **P9.6.1 (Stage E)** | **Newly discovered during live cross-browser form-validation testing:** `LoginModal.js` included `submitting` in the `useEffect` dependency array that called `setErrorObj(null)`, immediately wiping failed login error alerts when `setSubmitting(false)` ran. Split into open/focus effect `[isOpen]` + Escape keydown effect `[isOpen, submitting, onClose]` in [LoginModal.js](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/components/LoginModal.js) | Jest regression test (`70/70`) + live `01_form_validation` PASS on **Chromium, Firefox `144.0.2`, and WebKit `26.0`** |

---

## 3. Defects Still Open

- **Critical:** `0`
- **High:** `0`
- **Medium:** `0`
- **Low:** `0`
- *(Residual non-defect architectural/external items — CRA `react-scripts@5.0.1` Vite migration, React Router v7 major migration, manual screen readers, external SAST/DAST, P10 deployment hardening — are tracked in [p961-remaining-gap-register.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/qa/p961-remaining-gap-register.md).)*

---

## 4. Final Test-Count Reconciliation

Full baseline table in [p961-test-count-reconciliation.md](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/docs/qa/p961-test-count-reconciliation.md):

| Test Layer | Discovered | Executed | Passed | Failed / Error | Skipped / Opt-In |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Backend Default Suite (`22` Java classes, H2)** | `229` | `229` | `229` | `0` | `0` methods (`1` opt-in MySQL class skipped by default; Surefire reports `230 / 1 skipped`) |
| **Backend Real MySQL `8.0.45` Opt-In Suite (`1` class)** | `7` | `7` | `7` | `0` | `0` (when `EPAMS_MYSQL_INTEGRATION=true`) |
| **Backend Combined Unique Test Methods (`23` classes)** | **`236`** | **`236`** | **`236`** | **`0`** | **`0` unexecuted** |
| **Frontend Jest Suite (`6` suites, incl. `P961CriticalFrontendCoverage.test.js`)** | **`70`** | **`70`** | **`70`** | **`0`** | **`0`** |
| **Live 3-Engine Cross-Browser Smoke Suite (Chrome + Firefox + WebKit)** | **`30`** (`10 × 3`) | **`30`** | **`30`** | **`0`** | **`0`** |
| **Live Accessibility & Responsive Checks (`axe-core 4.11.4`, 4 viewports × 2 themes × 4 routes)** | **`32`** | **`32`** | **`32`** | **`0`** | **`0`** |
| **Packaged JAR SPA Deep-Link Checks** | **`11`** | **`11`** | **`11`** | **`0`** | **`0`** |

---

## 5. Backend & Frontend Code Coverage

### 5.1 Backend Coverage (`springapp/target/site/jacoco/jacoco.xml`, JaCoCo `0.8.14`, `114` classes)
- **Line Coverage:** **`86.40%`** (`3,418 / 3,956`, missed `538`)
- **Branch Coverage:** **`66.62%`** (`890 / 1,336`, missed `446`)
- **Instruction Coverage:** **`86.09%`** (`14,652 / 17,020`)
- **Method Coverage:** **`80.47%`** (`882 / 1,096`)
- **Class Coverage:** **`98.25%`** (`112 / 114`)

### 5.2 Frontend Coverage (`springapp/springbackend/coverage/coverage-final.json`, Jest)
| Scope | Stage A Baseline | Post-Stage D Final | Delta |
| :--- | :---: | :---: | :---: |
| **All Frontend Files (`48` files, incl. `10` legacy CRUD + `index.js`) — Lines** | `56.20%` (`919/1,635`) | **`73.09%`** (`1,198/1,639`) | **`+16.89%`** |
| **All Frontend Files (`48` files) — Branches** | `50.03%` (`791/1,581`) | **`67.02%`** (`1,061/1,583`) | **`+16.99%`** |
| **All Frontend Files (`48` files) — Statements / Functions** | `55.02%` / `46.83%` | **`71.62%`** / **`63.77%`** | **`+16.60%` / `+16.94%`** |
| **Critical Modern Workspace (`37` files, excl. `10` legacy CRUD + `index.js`) — Lines** | ~`65%` | **`84.80%`** (`1,188/1,401`) | Exceeds `80%` target |
| **Critical Modern Workspace (`37` files) — Branches** | ~`52%` | **`70.40%`** (`1,061/1,507`) | Honest gap vs `80%` stretch goal (`-9.60%`) |

Key critical file improvements: `TicketDetail.js` (`1.1%` → **`85.1%` L / `90.9%` B**), `EmployeeDashboard.js` (`8.5%` → **`76.3%` L / `63.3%` B**), `GlobalSearchModal.js` (`34.3%` → **`92.5%` L / `76.6%` B**), `Navbar.js` (`36.4%` → **`78.8%` L / `69.2%` B**), `AppLayout.js` (`47.1%` → **`76.5%` L / `88.9%` B**), `api.js` (`48.9%` → **`76.6%` L / `89.4%` B**), `ServiceCatalog.js` (`57.6%` → **`75.8%` L / `74.6%` B**), `NotificationsPage.js` (`64.4%` → **`79.7%` L / `64.2%` B**), `analyticsService.js` (`26.3%` → **`100.0%` L**).

---

## 6. Dependency Security Findings (Stage C)

| Package | Before P9.6.1 | After P9.6.1 | Advisories Resolved | Remaining Risk & Disposition |
| :--- | :---: | :---: | :--- | :--- |
| **`axios`** *(Direct runtime)* | `1.16.0` (`^1.4.0`) | **`1.21.1`** | **ALL (`18+` High/Mod → `0` advisories)** | **CLEAN (`0` vulnerabilities in `npm audit`)** |
| **`react-router-dom`** *(Direct runtime)* | `6.30.3` (`^6.11.1`) | **`6.30.6`** (`@remix-run/router` `1.23.4`) | **`GHSA-jjmj-jmhj-qwj2`** (CVSS `6.9`) + `@remix-run/router` clean | `2` Moderate `react-router` advisories remain requiring SemVer-major `v7.18.5` (`GHSA-wrjc-x8rr-h8h6` open redirect via backslash — unreachable since EPAMS uses static route paths; `GHSA-337j-9hxr-rhxg` SSR hydration — non-applicable to client-only SPA). Deferred to Vite + React Router v7 sprint. |
| **`react-scripts`** *(Dev/build toolchain)* | `5.0.1` | `5.0.1` (unchanged) | None (upstream archived; `npm audit fix --force` downgrades to `0.0.0`) | `100` transitive dev-only advisories (`3` Critical `proxy-addr`/`shell-quote`/`websocket-driver`, `85` High, `8` Moderate, `4` Low). Not bundled into static production JS/CSS served by Spring Boot. Planned Vite migration. |
| **Total `npm audit` Count** | `106` (`3C / 88H / 11M / 4L`) | **`102`** (`3C / 85H / 10M / 4L`) | **`-4` advisories (`0` direct High remaining)** | Transparently documented; zero claim of "zero vulnerabilities". |

---

## 7. Cross-Browser Verification Results (Stage E — All 3 Engines Passed)

Executed live via Playwright `1.57.0` against the freshly repackaged standalone JAR (`http://127.0.0.1:18097`, isolated H2 with synthetic catalog seed, `task-5004`):

| Browser Engine | Exact Version | Journeys Passed | JS `pageerror` | Result | Journeys Verified (`10 / 10` per engine) |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Chromium (Google Chrome)** | `157.0.8103.79` | **`10 / 10`** | `0` | **PASS** | Form validation (`QA-DEF-007`), Login + `HttpOnly` cookie, Session refresh reload, Catalog navigation, Ticket creation, Ticket detail (`TKT-`), Notifications, Theme switching (`light`/`dark`), Mobile `390×844` drawer, Logout + Admin Approvals |
| **Firefox (Playwright v1497)** | `144.0.2` | **`10 / 10`** | `0` | **PASS** | Same `10 / 10` end-to-end journeys |
| **WebKit (Playwright v2227)** | `26.0` | **`10 / 10`** | `0` | **PASS** | Same `10 / 10` end-to-end journeys |
| **Total Across 3 Engines** | **3 Engines** | **`30 / 30`** | **`0`** | **PASS** | *Note: `consoleErr` log lines correspond solely to expected `401` unauthenticated/bad-password probes and missing `favicon.ico` `404`.* |

---

## 8. Accessibility Evidence (Stage B — `QA-DEF-006` Closed)

- **Compact `320×700` `.ep-topbar` Measured Interactive Bounding Boxes (Chrome `157.0.8103.79`):**
  - `Open navigation menu` (`BUTTON`): **`36 × 36 px`** (`>= 24px`: `true`)
  - `EPAMS Enterprise Workspace Home` (`A`): **`44 × 34 px`** (`>= 24px`: `true`)
  - `Quick search workspace (Ctrl+K)` (`BUTTON`): **`36 × 36 px`** (`>= 24px`: `true`)
  - `Switch theme` (`BUTTON`): **`36 × 36 px`** (`>= 24px`: `true`)
  - `Notifications` (`BUTTON`): **`36 × 36 px`** (`>= 24px`: `true`)
  - `Account and profile menu` (`BUTTON`): **`38 × 38 px`** (`>= 24px`: `true`)
- **Keyboard Focus Check:** `Tab` traversal landed cleanly on `BUTTON:Open navigation menu` with explicit `:focus-visible` outline (`2px solid`).
- **Automated `axe-core 4.11.4` Matrix (`wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa`):**
  - **`32 / 32` scans PASSED with `0` violations and `0` horizontal overflow** across `320×700`, `390×844`, `768×1024`, `1440×900` × `Light` & `Dark` themes × `/`, `/tickets`, `/catalog`, `/preferences`.
- **Honesty Disclosure:** Automated WCAG 2.2 AA scans cannot verify screen-reader announcement ergonomics; manual **NVDA / JAWS / VoiceOver** verification remains `NOT TESTED`.

---

## 9. Real MySQL `8.0.45` Status (Stage F)

- **Default Opt-In Behavior Preserved:** `Stage2RealMysqlIntegrationTests` remained skipped by default during `mvn package -o "-Dskip.npm=true" -Pcoverage` (`Tests run: 230, Failures: 0, Errors: 0, Skipped: 1`).
- **Opt-In Real MySQL `8.0.45` Execution (`task-4955`, isolated `127.0.0.1:33308`):**
  - Packaged JAR booted as `epams_migrator` (`migratorBootHealthy = true`), applied Flyway `V1`–`V4` untouched.
  - Least-privilege runtime role `epams_app` (`SELECT, INSERT` only on `audit_log`) passed **`7 / 7`** `Stage2RealMysqlIntegrationTests` (`62.318 s`, `BUILD SUCCESS`).
  - SQL invariants: `0` orphan tickets/comments/approvals, `0` duplicate dedup keys, `4` Flyway migrations applied.
  - `mysqldump --no-tablespaces` backup (`301,216` bytes) restored **`14 / 14` tables** into `epams_p96_restore` (`restoreVerified = true`).

---

## 10. Full Regression Commands & Exact Exit Codes (Stage F)

| # | Gate | Exact Command Executed | Exit Code | Key Result |
| :---: | :--- | :--- | :---: | :--- |
| `1` | Frontend Jest + Coverage | `$env:CI="true"; npm --prefix springapp/springbackend test -- --watchAll=false --coverage --coverageReporters=json-summary --coverageReporters=json --coverageReporters=text-summary` | **`0`** | `6/6` suites, **`70/70`** passed; `73.09%` L / `67.02%` B (`84.80%` L modern workspace) |
| `2` | Frontend Production Build | `$env:CI="true"; npm --prefix springapp/springbackend run build` | **`0`** | `Compiled successfully` (`0` warnings; `112.46 kB` JS / `5.99 kB` CSS gzipped) |
| `3` | Backend Regression + JaCoCo + JAR | `mvn -f springapp/pom.xml package -o "-Dskip.npm=true" -Pcoverage` | **`0`** | `230` (`229` passed, `1` opt-in MySQL class skipped), JaCoCo `86.40%` L / `66.62%` B, JAR built |
| `4` | Post-Patch JAR Repackage | `mvn -f springapp/pom.xml package -o "-DskipTests" "-Dskip.npm=true"` | **`0`** | Fresh `springapp-0.0.1-SNAPSHOT.jar` with `QA-DEF-006` + `QA-DEF-007` fixes |
| `5` | Post-Patch Dependency Audit | `npm --prefix springapp/springbackend audit --json` | **`0`**\* | `102` total (`axios` = `0`; `react-router-dom` `GHSA-jjmj-jmhj-qwj2` resolved) |
| `6` | Real MySQL `8.0.45` Opt-In Suite | `powershell -ExecutionPolicy Bypass -File ".../scratch/p96-mysql-stage-k.ps1"` | **`0`** | **`7/7`** `Stage2RealMysqlIntegrationTests` PASS, `14/14` restore verified |
| `7` | Packaged JAR Deep-Links + Stage B `axe-core` + Stage E 3-Engine Cross-Browser | `node ".../scratch/p961-cross-browser-and-a11y.js"` | **`0`** | `11/11` SPA routes `200`; `32/32` `axe-core` `0` violations; **`30/30`** (Chrome `10/10`, Firefox `10/10`, WebKit `10/10`) |

*\*`npm audit` itself exits non-zero when transitive advisories exist; redirected capture script returned `0` and full JSON was parsed directly.*

---

## 11. Environment Limitations (Disclosed Honestly)

- **Windows Sandbox (`AppContainer`) vs. Firefox/WebKit:** Inside the restricted Windows sandbox, Firefox exited with `0xC0000022` (`STATUS_ACCESS_DENIED`) and WebKit loopback requests hit the sandbox proxy (`400`). Executing the cross-browser harness with host browser access (`BypassSandbox: true`) ran Chromium `157.0.8103.79`, Firefox `144.0.2`, and WebKit `26.0` cleanly (`30/30`).
- **Unavailable Local Security/AT Tools:** `trivy`, `semgrep`, OWASP ZAP, NVDA, JAWS, and VoiceOver are not installed on this workstation (`NOT TESTED`).
- **GitHub Actions Workflow:** `.github/workflows/ci-quality-gates.yml` was statically verified locally; remote GitHub runner execution occurs only after commit/push.

---

## 12. Remaining P10 Production Deployment Requirements

1. **Review & Commit P9.6 + P9.6.1 Changes:** Inspect complete `git diff` and commit/push separately after explicit approval.
2. **Database & Transport Hardening:** Enforce MySQL TLS (`sslMode=VERIFY_IDENTITY`), remove `allowPublicKeyRetrieval=true` and `createDatabaseIfNotExist=true`, provision separate production `epams_migrator` and least-privilege `epams_app` credentials via secrets manager, and disable dev demo user seeding.
3. **Edge & API Security:** Enable HSTS (`Strict-Transport-Security`), gate `/swagger-ui/**` and `/api/docs` for production, and add `trivy fs` + CodeQL/Semgrep SAST to GitHub Actions.
4. **Operational Readiness:** Configure graceful shutdown (`server.shutdown=graceful`), ShedLock if scaling `>= 2` backend replicas, scheduled `mysqldump`/snapshot backups with restore drills, and a 2–4 hour staging soak test.
5. **Post-GA Engineering Backlog:** Migrate frontend from archived `react-scripts@5.0.1` to Vite + Vitest and upgrade `react-router-dom` `6.30.6` → `7.x` to eliminate remaining transitive dev-toolchain and v7-only advisories; complete manual screen-reader (NVDA/VoiceOver) verification.
