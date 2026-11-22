# EPAMS — Stage 1 Authorized Git Release Execution Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Operation:** Authorized Option 2 — Three-Commit Split & Normal Fast-Forward Push  
**Execution Date:** 2026-11-18  
**Target Branch / Remote:** `main` → `origin` (`https://github.com/iyyappanecys033/Employee-Account-Portal-Management-System-.git`)  
**Previous Baseline Commit:** `a4857679c0bde310e4b705a46d7e7ef701bc3901`  
**Final Synchronized `HEAD` / `origin/main` Commit:** `6205bd5a9bc06a6dafbb259f73e8215325537f2e`  
**Execution Status:** **`SUCCESS — VERIFIED REMOTE PARITY`**

---

## 1. Created Commits & Exact Commit Hashes

| Order | Full Commit Hash | Short Hash | Commit Message | Files | Diff Summary |
| :---: | :--- | :---: | :--- | :---: | :--- |
| **1** | `3b0e60264c51912917128a7d73e709b059557d26` | `3b0e6026` | `fix: resolve enterprise QA defects and dependency vulnerabilities` | `14` | `+264 / -65` |
| **2** | `34b8b169a2c2e049eb9f3c5afcac0f83b5b4bea1` | `34b8b169` | `test: expand EPAMS enterprise regression coverage` | `7` | `+2,323 / -0` |
| **3** | `6205bd5a9bc06a6dafbb259f73e8215325537f2e` | `6205bd5a` | `docs: record P9.6.1 enterprise QA closure evidence` | `33` | `+1,735 / -0` |
| **Total** | **`a4857679..6205bd5a`** (`3` commits) | — | Option 2 Three-Commit Split | **`54`** | **`+4,322 / -65`** |

---

## 2. Complete Per-Commit File Inventory

### Commit 1 (`3b0e60264c51912917128a7d73e709b059557d26`) — Production Fixes & Dependency Patches (`14` files)
1. `springapp/pom.xml` (`QA-DEF-005`: `@{jacoco.argLine}`, JaCoCo `0.8.14`)
2. `springapp/src/main/java/com/examly/springapp/controller/SpaController.java` (`QA-DEF-001`)
3. `springapp/src/main/java/com/examly/springapp/security/SecurityConfig.java` (`QA-DEF-001`)
4. `springapp/src/main/java/com/examly/springapp/controller/ProfileController.java` (`QA-DEF-002`, `QA-DEF-003`)
5. `springapp/src/main/java/com/examly/springapp/security/AuthController.java` (`QA-DEF-003`)
6. `springapp/src/main/java/com/examly/springapp/dto/RegisterRequest.java` (`QA-DEF-003`)
7. `springapp/src/main/java/com/examly/springapp/dto/LoginRequest.java` (`QA-DEF-003`)
8. `springapp/src/main/java/com/examly/springapp/dto/EmployeeRequestDTO.java` (`QA-DEF-003`)
9. `springapp/src/main/java/com/examly/springapp/exception/GlobalExceptionHandler.java` (`QA-DEF-004`)
10. `springapp/springbackend/src/components/layout/Navbar.js` (`QA-DEF-006`)
11. `springapp/springbackend/src/styles/design-system/components.css` (`QA-DEF-006`)
12. `springapp/springbackend/src/components/LoginModal.js` (`QA-DEF-007`)
13. `springapp/springbackend/package.json` (`axios@^1.21.1`, `react-router-dom@^6.30.6`)
14. `springapp/springbackend/package-lock.json` (`axios@1.21.1`, `react-router-dom@6.30.6`, `@remix-run/router@1.23.4`)

### Commit 2 (`34b8b169a2c2e049eb9f3c5afcac0f83b5b4bea1`) — Automated Tests & CI Quality Gates (`7` files)
1. `springapp/src/test/java/com/examly/springapp/P96UnitBusinessLogicTests.java`
2. `springapp/src/test/java/com/examly/springapp/P96ApiAndFieldValidationTests.java`
3. `springapp/src/test/java/com/examly/springapp/P96RbacFieldSecurityIdorTests.java`
4. `springapp/src/test/java/com/examly/springapp/P96TransactionReliabilityTests.java`
5. `springapp/springbackend/src/P96FrontendComprehensive.test.js`
6. `springapp/springbackend/src/P961CriticalFrontendCoverage.test.js`
7. `.github/workflows/ci-quality-gates.yml`

### Commit 3 (`6205bd5a9bc06a6dafbb259f73e8215325537f2e`) — QA Documentation & Traceability (`33` files)
1. `docs/product-discovery/requirements-traceability-matrix.md` (Section 5 P9.6 Addendum)
2. `docs/qa/p96-*.md` (`27` P9.6 verification reports, including updated `p96-defect-register.md`)
3. `docs/qa/p961-*.md` (`4` P9.6.1 closure reports: `p961-final-qa-closure-report.md`, `p961-independent-evidence-audit.md`, `p961-test-count-reconciliation.md`, `p961-remaining-gap-register.md`)
4. `docs/qa/stage1-git-release-audit.md`

---

## 3. Pre-Release Test & Integrity Evidence

| Verification Gate | Result | Measured Evidence |
| :--- | :---: | :--- |
| **Backend Default Regression (`mvn test -o "-Dskip.npm=true"`)** | **PASS** (exit `0`) | `Tests run: 230, Failures: 0, Errors: 0, Skipped: 1` (`229` default tests + `1` opt-in MySQL class skipped), JaCoCo `86.40%` L / `66.62%` B |
| **Real MySQL `8.0.45` Opt-In Suite** | **PASS** (exit `0`) | `7 / 7` `Stage2RealMysqlIntegrationTests` under least-privilege `epams_app`; `14 / 14` `mysqldump` restore verified |
| **Frontend Jest & Production Build (`CI=true`)** | **PASS** (exit `0`) | `6 / 6` suites, `70 / 70` tests (`73.09%` aggregate L / `67.02%` B; `84.80%` modern workspace L); `Compiled successfully` (`0` warnings) |
| **Live Accessibility (`axe-core 4.11.4`)** | **PASS** (exit `0`) | `32 / 32` Light & Dark scans (`320×700`, `390×844`, `768×1024`, `1440×900` × `4` routes): `0` violations, `0` overflow |
| **3-Engine Cross-Browser Smoke Suite** | **PASS** (exit `0`) | `30 / 30`: Chromium `157.0.8103.79` (`10/10`), Firefox `144.0.2` (`10/10`), WebKit `26.0` (`10/10`); `11 / 11` JAR SPA deep-links HTTP `200` |
| **Flyway `V1`–`V4` Range Diff (`a4857679..6205bd5a`)** | **VERIFIED** | `FLYWAY_RANGE_DIFF_LEN=0` (zero modifications to `V1`–`V4`) |
| **Secret & Unwanted Artifact Scan** | **VERIFIED** | `0` secrets, `0` credentials, `0` DB files, `0` browser profiles, `0` build outputs committed |

---

## 4. Push Execution & Post-Push Remote Parity Verification

### Push Command Output (`git push origin main` — Normal Fast-Forward, Exit Code `0`)
```text
To https://github.com/iyyappanecys033/Employee-Account-Portal-Management-System-.git
   a4857679..6205bd5a  main -> main
```

### Post-Push Verification (`git fetch origin` + Hash Parity Check, Exit Code `0`)
```text
LOCAL_HEAD=6205bd5a9bc06a6dafbb259f73e8215325537f2e
ORIGIN_MAIN=6205bd5a9bc06a6dafbb259f73e8215325537f2e
LS_REMOTE=6205bd5a9bc06a6dafbb259f73e8215325537f2e	refs/heads/main
AHEAD_BEHIND=0	0
STATUS_LINES=0
```

- **Local vs. Remote Hash Equality:** **EXACT MATCH** (`6205bd5a9bc06a6dafbb259f73e8215325537f2e`).
- **Working Tree Status Immediately Post-Push:** **CLEAN (`STATUS_LINES=0`)**.
- *(Note: This execution report `docs/qa/stage1-git-release-execution-report.md` was generated after the verified push as required by Step 7 and intentionally left uncommitted so that exactly the three authorized commits were created and pushed.)*

---

## 5. Remaining Limitations & Scope Confirmation

- **Remaining Documented Limitations (Carried Forward from P9.6.1):**
  - `100` transitive dev/build-only advisories under archived `react-scripts@5.0.1` + `2` non-applicable/unreachable v7-only `react-router` moderate advisories (`0` direct High/Critical runtime advisories).
  - Manual screen-reader evaluation (NVDA/JAWS/VoiceOver) and external SAST/DAST (`trivy`/`semgrep`/ZAP) not executed locally.
  - GitHub Actions workflow `.github/workflows/ci-quality-gates.yml` is now pushed to `origin/main` and ready to execute on GitHub runners.
- **Strict Scope Honored:** R1 market research was NOT started, P10 deployment was NOT started, Flyway `V1`–`V4` were NOT modified, and zero history rewriting or force-pushing occurred.
