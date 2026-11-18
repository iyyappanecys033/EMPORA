# EPAMS â€” Stage 2 MySQL 8+ Environment Report (Stage B)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration (Stage B: Isolated Disposable MySQL Environment)
**Date:** 2026-11-18
**Environment Status:** **`READY & VERIFIED (REAL MYSQL 8.0.45 COMMUNITY SERVER)`**

---

## 1. Provisioning Method & Engine Metadata (`VERIFIED`)

- **Docker Status:** Docker CLI `29.7.2` is installed, but the Docker Desktop Linux engine daemon (`//./pipe/dockerDesktopLinuxEngine`) was not running on the host.
- **Selected Isolation Method:** Provisioned a **100% isolated, disposable real MySQL Community Server `8.0.45`** instance directly from the installed official MySQL Server binary (`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe`) using a brand-new disposable `--datadir` stored outside the workspace (`...\scratch\mysql-stage2-8045\data`), bound exclusively to `127.0.0.1:13307` (`--skip-mysqlx`). Existing host MySQL services (if any on `:3306`) and user data directories were never touched.
- **No H2 Substitution:** Zero H2 databases are used for any Stage Câ€“F MySQL verification.

| Parameter | Verified Live MySQL Value (`SELECT @@...`) |
| :--- | :--- |
| **MySQL Version** | `8.0.45` |
| **Edition** | `MySQL Community Server - GPL` (Win64 `x86_64`) |
| **Container Image / Digest** | N/A â€” native isolated disposable `mysqld.exe` `8.0.45` instance (Docker daemon inactive) |
| **Default Storage Engine** | `InnoDB` |
| **Server Character Set** | `utf8mb4` |
| **Server Collation** | `utf8mb4_unicode_ci` |
| **Default Transaction Isolation** | `REPEATABLE-READ` *(overridden to `READ_COMMITTED` by `AuditChainService`)* |
| **TLS Channel** | Enabled on `mysql_main` (auto-generated self-signed `ca.pem`) |
| **Bind Address & Port** | `127.0.0.1:13307` (loopback-only) |

---

## 2. Disposable Databases & Credential Governance

- **Databases Created (`utf8mb4` / `utf8mb4_unicode_ci`):**
  1. `epams_stage2_fresh` â€” Fresh `V1`â€“`V4` migration, Hibernate validation, Stage E reliability/concurrency/analytics, Stage F permissions, Stage G live startup.
  2. `epams_stage2_brownfield` â€” Pre-existing legacy IAM schema + realistic seeded records â†’ explicit Flyway baseline v1 + `V2`â€“`V4` upgrade preservation test.
- **Credential Externalization:** Ephemeral per-run credentials for `epams_migrator`, `epams_app`, and `JWT_SECRET` were randomly generated at runtime via GUID entropy, stored exclusively in scratch storage outside the Git repository, injected via environment variables (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`), and never hardcoded or printed in logs/reports.
- **Initial Migrator Grants (`SHOW GRANTS FOR 'epams_migrator'@'127.0.0.1'`):**
  - `GRANT USAGE ON *.*` (zero global privileges)
  - `GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER` scoped strictly to `epams_stage2_fresh.*` and `epams_stage2_brownfield.*`.
- **Runtime Application User (`'epams_app'@'127.0.0.1'`):** Strict table-level least-privilege grants applied post-migration (documented & verified in Stage F).
