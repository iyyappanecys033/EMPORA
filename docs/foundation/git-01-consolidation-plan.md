# EPAMS — GIT-01 Legacy Repository Consolidation Plan (Stages A & C)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Task:** GIT-01 Legacy Repository Consolidation (`springapp/.git` → Unified Root Repository)  
**Date:** 2026-11-18  

---

## 1. Stage A — Read-Only Inspection & File-Level Inventory (`VERIFIED`)

### 1.1 Root Repository (`EPAMS/.git`)
- **State:** Initialized (`HEAD` unborn on branch `main`, `0` commits, `0` remotes).
- **Index Contents (`git ls-files --stage`):** `119` entries total:
  - `118` regular files (`docs/**`, `.gitignore`, `.env.example`, `README.md`, `package-lock.json`).
  - `1` unintended embedded gitlink: `160000 b76d88f41284ec8bfe86a711852c99aba3905c42 0 springapp`.
- **Impact of Embedded Gitlink:** None of the `214` backend/frontend/Flyway/test source files inside `springapp/` are tracked in `EPAMS/.git`, and `git check-ignore` fails on `springapp/*` with `fatal: Pathspec 'springapp/pom.xml' is in submodule 'springapp'`.

### 1.2 Nested Legacy Repository (`springapp/.git`)
- **Branch & Remote:** `* main` at `b76d88f41284ec8bfe86a711852c99aba3905c42` tracking `origin/main` (`https://github.com/iyyappanecys033/Employee-Account-Portal-Management-System-.git`).
- **Commit History (2 Legacy Commits):**
  1. `7d082276aae080bcd56bfcd82d2797b6e4f56429` — `2026-05-16 18:18:45 +0530` — `iyyappanecys033 <727724eucy033@skcet.ac.in>` — `first commit`
  2. `b76d88f41284ec8bfe86a711852c99aba3905c42` — `2026-05-16 18:27:23 +0530` — `iyyappanecys033 <727724eucy033@skcet.ac.in>` — `feat: complete full stack crud application` (parent `7d082276`)
- **Legacy Tree Breakdown in `b76d88f4` (`43,881` tracked files; `springapp/.git` size = `234.11 MB`):**
  - `frontend/` — `43,793` files (legacy starter React app including committed `node_modules/`, `node.exe`, `build/`).
  - `target/` — `50` files (legacy Maven compiled outputs).
  - `src/` — `32` files (original starter CRUD Spring Boot entities/controllers/services).
  - `pom.xml`, `.gitignore`, `mvnw`, `mvnw.cmd`, `HELP.md`, `README.md` — `6` files.
- **Uncommitted Working-Tree State in `springapp/` (excluding `target/`, `springbackend/node_modules/`, `springbackend/build/`):**
  - **21 modified legacy files** that evolved directly into EPAMS IAM (`pom.xml`, `SpringappApplication.java`, `CorsConfig.java`, `AccountStatusLogController.java`, `ProfileController.java`, `RoleController.java`, `UserAccountController.java`, `UserRoleMappingController.java`, `GlobalExceptionHandler.java`, `Profile.java`, `UserAccount.java`, `ProfileRepo.java`, `RoleRepo.java`, `UserRoleMappingRepo.java`, `ProfileService.java`, `ProfileServiceImpl.java`, `RoleServiceImpl.java`, `UserAccountService.java`, `UserAccountServiceImpl.java`, `UserRoleMappingServiceImpl.java`, `application.properties`).
  - **43,794 deleted legacy files** (replaced legacy `frontend/` & old `target/`).
  - **99 untracked directories/files** encompassing the full EPAMS P0–P9 backend, Flyway `V1`–`V4` migrations, 18 test suites, and React 18 `springbackend/` frontend.
- **Current Trackable EPAMS File Count:**
  - `springapp/` (excluding `.git`, `target`, `node_modules`, `build`): **`214` files**.
  - Full workspace (`docs/`, root files, `springapp/`, excluding `.git`, `.vscode`, `.github/modernize`, `target`, `node_modules`, `build`, `cookies.txt`): **`334+` files** (plus GIT-01 docs).
- **Secrets / Generated Artifacts Check:** Zero secrets, `cookies.txt`, `target/`, `node_modules/`, or `build/` files are staged in root `.git` (`FORBIDDEN_STAGED_COUNT=0`).

---

## 2. Stage C — Consolidation Strategy Evaluation

| Strategy | Description | Assessment | Verdict |
| :--- | :--- | :--- | :---: |
| **1. `git subtree add --prefix=springapp`** | Imports `b76d88f4` into `springapp/` via subtree checkout + merge. | **Unsafe on live working tree:** `b76d88f4` contains `43,793` legacy `frontend/node_modules` + `50` `target/` files at `springapp/` root; `git subtree add` requires a clean working tree and would collide with or overwrite the `214` uncommitted EPAMS source files in `springapp/`. | **REJECTED** |
| **2. History-Preserving Unrelated-Histories Merge (`fetch` from verified bundle + `merge --allow-unrelated-histories -s ours --no-commit` + stage unified EPAMS working tree)** | Creates root documentation/config initial commit on `main`, fetches legacy `springapp` refs (`7d082276` → `b76d88f4`) verbatim into `refs/heads/legacy-springapp`, retires nested `springapp/.git` *only after* verified bundle + filesystem backups exist, then performs a two-parent merge commit on `main` (`-s ours --no-commit`) and stages the intact current working tree under root `.gitignore`. | **Optimal & Safe:** (a) Preserves exact unmodified legacy commit SHA-1s (`7d082276`, `b76d88f4`) as directly reachable ancestors of `HEAD`; (b) zero working-tree modification or collision with uncommitted files; (c) keeps all `43,881` legacy `frontend/node_modules` and `target/` files out of `HEAD` while still inspectable in `b76d88f4`; (d) produces a single unified repository with zero `160000` gitlinks; (e) leaves remote `origin` completely untouched. | **SELECTED** |
| **3. Formal Git Submodule (`git submodule`)** | Keeps `springapp/` as a separate independent repository referenced by `.gitmodules`. | **Misaligned with EPAMS ownership:** EPAMS is a single tightly coupled monorepo (`docs/`, backend `springapp/`, frontend `springapp/springbackend/`). Keeping `springapp` as a submodule forces dual commits for every PR, perpetuates the bloated 234 MB legacy `frontend/node_modules` working-tree diff, and breaks unified CI/CD and Stage 2/P10 release tagging. | **REJECTED** |

---

## 3. Selected Execution Sequence (Stage D)

1. **Gate:** Confirm Stage B Git bundle (`--all`) verification and separate filesystem backup (working tree + `springapp/.git`) both passed with exit `0`.
2. Remove the unintended `160000` gitlink from root index (`git rm --cached springapp`) without touching any files in `springapp/`.
3. Commit the clean root project scaffolding & documentation as the initial root commit on `main` (`chore: initialize EPAMS root repository structure and documentation`).
4. Fetch the exact legacy commits (`7d082276`, `b76d88f4`) from the verified bundle into `refs/heads/legacy-springapp` inside root `EPAMS/.git` (`git fetch "<bundle>" refs/heads/main:refs/heads/legacy-springapp`).
5. Retire nested `springapp/.git` (already preserved in both the verified Git bundle, the standalone filesystem backup, AND fetched into root `EPAMS/.git` as `legacy-springapp`).
6. Execute `git merge --allow-unrelated-histories -s ours --no-commit legacy-springapp` on `main` (attaches `b76d88f4` as second parent without touching any working-tree files).
7. Stage all trackable EPAMS files (`git add -A`) respecting root `.gitignore`, verify zero forbidden files/secrets and zero `160000` gitlinks, and commit (`chore: consolidate legacy springapp history into unified EPAMS baseline`).
8. Do **NOT** modify, add, or push to any remote (`origin` remains completely untouched).
