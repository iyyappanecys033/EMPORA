# EPAMS — P9.6 CI Quality Gates Architecture (Stage O)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification (Stage O)  
**Workflow File:** [.github/workflows/ci-quality-gates.yml](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/.github/workflows/ci-quality-gates.yml)  
**Scope Boundary:** Continuous Integration **quality verification gates only** (compile, unit/integration tests, JaCoCo coverage, Jest/build/audit, real MySQL 8 integration, and Playwright E2E). **Zero production deployment or release steps** (reserved strictly for P10).

---

## 1. CI Quality Gate Pipeline Overview

Triggered on `push` to `main`, `pull_request` targeting `main`, and manual `workflow_dispatch` with least-privilege token scope (`permissions: contents: read`):

| Job ID | Gate Objective | Runtime | Pass/Fail Policy | Artifacts Uploaded |
| :--- | :--- | :--- | :--- | :--- |
| `backend-unit-integration-jacoco` | Compiles Java `25` backend, runs all `229` unit/integration/security/API/reliability tests, generates JaCoCo `0.8.14` coverage report | `ubuntu-latest`, Temurin JDK `25` | **Hard Blocking** (zero test failures allowed) | `jacoco-coverage-report` (`springapp/target/site/jacoco/`) |
| `frontend-test-build-audit` | Runs `npm ci`, executes Jest (`49` tests) with lcov coverage, validates zero-warning React production build (`CI=true`), runs `npm audit --audit-level=high` | `ubuntu-latest`, Node.js `22` | Jest & Build: **Hard Blocking**; `npm audit`: **Advisory Report** (`continue-on-error: true` until CRA→Vite migration) | `frontend-jest-coverage` (`springapp/springbackend/coverage/`) |
| `mysql8-integration-gate` | Spins up ephemeral `mysql:8.0` service, provisions separate `epams_migrator` & least-privilege `epams_app` accounts + `epams_stage2_brownfield`, applies Flyway `V1`–`V4` via migrator, and executes `Stage2RealMysqlIntegrationTests` (`7/7`) under `epams_app` | `ubuntu-latest`, JDK `25` + `mysql:8.0` container | **Hard Blocking** (enforces real MySQL 8 migration, brownfield preservation, audit immutability & concurrency) | Surefire test logs |
| `playwright-e2e-accessibility-gate` | Builds standalone packaged JAR, boots live app, installs Playwright Chromium, and executes browser E2E suite against live UI | `ubuntu-latest`, JDK `25` + Node `22` (`needs: [backend, frontend]`) | **Hard Blocking** | Playwright report on failure |

---

## 2. Recommended Quality Thresholds & Ratchet Policy

| Metric | P9.6 Measured Baseline | Current CI Gate Policy | Recommended P10 Ratchet Target |
| :--- | :---: | :--- | :---: |
| **Backend Tests Passing** | `229 / 229` (`100%`) | `100%` pass required | `100%` pass required |
| **Real MySQL 8 Suite Passing** | `7 / 7` (`100%`) | `100%` pass required | `100%` pass required |
| **Frontend Jest Tests Passing** | `49 / 49` (`100%`) | `100%` pass required | `100%` pass required |
| **Frontend Production Build (`CI=true`)** | `0` warnings / `0` errors | Zero warnings/errors | Zero warnings/errors |
| **Backend JaCoCo Line Coverage** | **`86.40%`** | Report & trend artifact | Enforce `>= 80%` line / `>= 65%` branch via `jacoco:check` |
| **Frontend Jest Line Coverage** | **`56.20%`** | Report & trend artifact | Ratchet to `>= 65%` line / `>= 55%` branch as page specs expand |
| **Flyway `V1`–`V4` Immutability** | Verified checksum match | Enforced by `validate-on-migrate=true` | Maintain immutable applied migrations |

---

## 3. Additional Security Gates Recommended for P10

- **GitHub CodeQL / Semgrep SAST:** Automated static analysis on Java + JavaScript PRs.
- **Trivy Filesystem & Container Scan:** Automated CVE scanning across Maven JARs, npm runtime dependencies, and P10 Docker images.
- **Dependabot Security Updates:** Automated PRs for direct runtime dependencies (`axios`, `react-router-dom`, Spring Boot patch releases).
- **Note on Local Workflow Execution:** `.github/workflows/ci-quality-gates.yml` was authored and syntax-checked locally; actual GitHub Actions runner execution occurs once the repository changes are reviewed and pushed to GitHub.
