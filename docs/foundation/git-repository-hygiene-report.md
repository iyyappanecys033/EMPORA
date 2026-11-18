# EPAMS — Git Initialization & Repository Hygiene Report (Stage C)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Task:** Final Foundation Readiness — Stage C (Git Inspection, `.gitignore`, Secret Hygiene & Baseline Commit Readiness)  
**Date:** 2026-11-18  
**Git Baseline Commit Status:** **`BLOCKED — NESTED LEGACY REPOSITORY REQUIRES OWNER DECISION (`GIT-01`)`**  

---

## 1. Pre-Remediation Inspection Findings (`VERIFIED`)

| Check | Verified Finding |
| :--- | :--- |
| **Root `.git` Directory (`EPAMS/.git`)** | Absent prior to Stage C (`Test-Path .git` = `False`). |
| **Nested `.git` Directory (`springapp/.git`)** | **PRESENT (pre-existing legacy repository discovered during staging audit).** Branch `main` tracking `origin/main` (`https://github.com/iyyappanecys033/Employee-Account-Portal-Management-System-.git`) with 2 historical starter CRUD commits from `2026-05-16`: `7d082276` (*first commit*) and `b76d88f4` (*feat: complete full stack crud application*). |
| **Legacy `springapp/.gitignore`** | Contains only `node_modules/` (1 line); does not exclude `target/`, `springbackend/node_modules/`, or `springbackend/build/`, yielding `44,074` dirty status entries inside `springapp/`. |
| **Root `.gitignore`** | Absent prior to Stage C; created during Stage C. |
| **Git Binary & Identity** | `git version 2.53.0.windows.2`; user `user.name` and `user.email` already configured globally. |
| **Untracked / Generated / Legacy Artifacts Found** | `cookies.txt` (root session cookie file), `.vscode/`, `.github/modernize/java-upgrade/` (legacy IDE upgrade scratch logs), `springapp/target/`, `springapp/springbackend/node_modules/`, `springapp/springbackend/build/`, empty 6-line legacy root `package-lock.json` (`"name": "Employee", "packages": {}`). |

---

## 2. Root `.gitignore` & `.env.example` Remediation (`VERIFIED`)

- Created [`.gitignore`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/.gitignore) excluding:
  - Maven/Java build outputs (`target/`, `**/target/`, `*.jar`, `*.war`, `*.class`, `*.original`, `hs_err_pid*`)
  - Node.js / React dependencies and outputs (`node_modules/`, `**/node_modules/`, `springapp/springbackend/build/`)
  - Playwright / coverage outputs (`playwright-report/`, `test-results/`, `coverage/`)
  - IDE & legacy scratch metadata (`.vscode/`, `.idea/`, `.github/modernize/`, `.DS_Store`, `Thumbs.db`)
  - Runtime/local artifacts (`*.log`, `*.tmp`, `cookies.txt`, `uploads/`, `*.h2.db`, `*.mv.db`, `*.sqlite`)
  - Secret files and key material (`.env`, `.env.local`, `.env.*.local`, `*.pem`, `*.key`, `*.p12`, `*.pfx`, `*.jks`, `*.crt`, `*.cer`), while explicitly preserving `!.env.example`, `pom.xml`, `springapp/springbackend/package-lock.json`, Flyway `V1`–`V4` SQL migrations, source code, tests, and `docs/`.
- Created [`.env.example`](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/.env.example) containing safe non-secret placeholders (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `COOKIE_SECURE`, `COOKIE_SAME_SITE`, `EPAMS_EMAIL_*`, `EPAMS_HOLIDAYS`, `EPAMS_SCHEDULING_ENABLED`).

---

## 3. Secret Hygiene Audit (`VERIFIED` — Zero Secret Values Printed)

Scanned **194** backend, frontend, config, migration, and E2E files for credential patterns (`jwt.secret`, `datasource.password`, `mail.password`, API keys, PEM private keys, GitHub PATs, AWS keys). **Zero** production credentials, API keys, PATs, or private keys were found.

| File | Finding & Classification | Remediation / Disposition |
| :--- | :--- | :--- |
| `springapp/src/main/resources/application-mysql.properties` | Previously contained a hardcoded fallback value on `spring.datasource.password` and relied on default profile `jwt.secret`. | **REMEDIATED (`VERIFIED`):** Replaced with `${DB_USERNAME:epams_app}`, `${DB_PASSWORD:}` (empty fallback), `jwt.secret=${JWT_SECRET}` (mandatory env variable, no fallback), and `${EPAMS_EMAIL_PASSWORD:}`. |
| `springapp/src/main/resources/application.properties` | Local ephemeral H2 development defaults (blank H2 password + local dev-only sample JWT secret). | **RETAINED FOR LOCAL H2 DEV:** Required for zero-config local H2 startup; explicitly overridden by mandatory `${JWT_SECRET}` in `mysql` profile. |
| `springapp/src/main/resources/application-dev.properties` | Legacy ephemeral in-memory H2 dev profile (`jdbc:h2:mem:springboot_db`) with trivial local H2 placeholders. | **RETAINED (NON-PRODUCTION):** Never active in `mysql` profile; recommended for removal/consolidation during P10 hardening. |
| `springapp/src/test/resources/application.properties` | Ephemeral in-memory H2 test configuration used by automated JUnit/SpringBootTest suites. | **RETAINED FOR AUTOMATED TESTS:** Non-sensitive test fixture only. |
| `SmtpEmailDeliveryAdapter.java`, `JwtService.java` | Property binding annotations (`@Value`) only — no hardcoded literals. | **CLEAN (`VERIFIED`).** |
| `cookies.txt` (workspace root) | Untracked local session cookie artifact from earlier manual testing. | **EXCLUDED (`VERIFIED`):** Ignored by root `.gitignore`; verified `0` forbidden files staged. Preserved on disk per Rule 6 (safe for owner to delete locally). |

---

## 4. Git Initialization, Staging Audit & Blocker (`GIT-01`)

### What Was Executed (`VERIFIED`)
1. Ran `git init -b main` and `git add .` at the EPAMS project root (`EPAMS/.git` created).
2. Verified root staging audit:
   - `STAGED_COUNT=119` (root files + `docs/**` + embedded gitlink `springapp`).
   - `FORBIDDEN_STAGED_COUNT=0` (`cookies.txt`, `.vscode/`, `.github/modernize/` confirmed ignored via `git status --ignored --short`).
3. **Critical Discovery During Staging Review:** Git emitted:
   `warning: adding embedded git repository: springapp` (`Am springapp`, mode `160000`).
   Because `springapp/.git` already exists, **none of the EPAMS Java backend, Flyway V1–V4 migrations, or React frontend source files inside `springapp/` are tracked as files in `EPAMS/.git`**.

### Why No Baseline Commit Was Created (`HONEST BLOCKER REPORT`)
- Per **Section C.1** (*"Never overwrite an existing Git repository"*) and **Rule 6** (*"Do not delete existing user data"*), `springapp/.git` was **NOT deleted or altered**.
- Creating a root commit while `springapp/.git` remains nested would commit a broken `160000` gitlink without any application source code.
- Additionally, subsequent sandboxed Git index writes inside the newly created `.git/` failed with `fatal: Unable to create '.git/index.lock': Permission denied` (sandbox `.git` write guard).
- **Therefore, NO Git commit was created.**

---

## 5. Exact Owner Commands to Finalize Unified Git Baseline

Choose **Option 1 (Recommended — Single Unified EPAMS Repository)** after confirming `springapp/.git`'s 2 legacy commits (`7d082276`, `b76d88f4`) are already safely backed up on `origin/main`:

```powershell
# Run from: c:\Users\Iyyappan E\Folders\SDE\Round 3-HR Interview\PROJECTS\Projects\EPAMS

# 1. (Optional safety backup) Archive legacy springapp/.git history outside the repo before removing it
git -C springapp bundle create ..\springapp-legacy-history.bundle --all

# 2. Remove the broken gitlink from the root index and remove the nested legacy springapp/.git
git rm --cached springapp
Remove-Item -Recurse -Force springapp\.git

# 3. Stage the complete unified EPAMS repository (docs + backend + frontend)
git add .

# 4. Verify zero forbidden files (target/, node_modules/, build/, cookies.txt, .env, .vscode/) are staged
git status --short
git diff --cached --name-only | Select-String -Pattern '(^|/)target/|(^|/)node_modules/|springbackend/build/|cookies\.txt|\.env($|\.(?!example))'

# 5. Create the clean unified baseline commit
git commit -m "chore: establish EPAMS project baseline"

# 6. Verify clean working tree and commit metadata
git status --short
git log -1 --stat
```
