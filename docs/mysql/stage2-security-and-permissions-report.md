# EPAMS â€” Stage 2 Security & Database Permissions Report (Stage F)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration (Stage F: Least-Privilege Accounts, Audit Immutability & Credential Hygiene)
**Target Instance:** Isolated MySQL `8.0.45` (`127.0.0.1:13307`)
**Date:** 2026-11-18
**Verification Status:** **`PASSED (VERIFIED AGAINST REAL MYSQL 8.0.45)`**

---

## 1. Role Separation (`epams_migrator` vs. `epams_app`)

Zero global privileges (`GRANT USAGE ON *.*` only) were granted to either database account.

### 1.1 Migration User (`'epams_migrator'@'127.0.0.1'`)
- **Verified Grants (`SHOW GRANTS`):** `SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER` scoped strictly to `epams_stage2_fresh.*` and `epams_stage2_brownfield.*`.
- **Usage:** Executed Flyway `V1`â€“`V4` fresh migration (Stage C) and brownfield `V1` baseline + `V2`â€“`V4` upgrade (Stage D). Never used for runtime application traffic.

### 1.2 Runtime Application User (`'epams_app'@'127.0.0.1'`)
- **Verified Grants (`SHOW GRANTS`):**
  - `SELECT, INSERT, UPDATE, DELETE` on operational tables only: `user_account`, `role`, `user_role_mapping`, `profile`, `refresh_token`, `account_status_log`, `service_catalog`, `service_ticket`, `ticket_comment`, `approval_task`, `in_app_alert`.
  - **`SELECT, INSERT` ONLY** on `audit_log` (`UPDATE` and `DELETE` explicitly omitted).
  - **`SELECT, UPDATE` ONLY** on `audit_chain_head` (sufficient for `SELECT ... FOR UPDATE` row locking and chain head advancement; `DELETE` and DDL omitted).
  - **`SELECT` ONLY** on `flyway_schema_history` (sufficient for startup `validate-on-migrate=true` schema verification).
- **Live Runtime Verification:** Booted packaged EPAMS Spring Boot `3.5.16` JAR as `epams_app` (`task-4081`) â€” Flyway validated 4 migrations, Hibernate `ddl-auto=validate` passed, `/api/v1/health` returned HTTP `200`, and all 7 Stage 2 reliability/concurrency/analytics tests passed under `epams_app`.

---

## 2. Negative Permission Enforcement Tests (`VERIFIED` on Real MySQL `8.0.45`)

| Attempted Operation as `'epams_app'@'127.0.0.1'` | MySQL Exit Code | Exact MySQL Error Returned | Result |
| :--- | :---: | :--- | :---: |
| `UPDATE audit_log SET details='tamper' WHERE id=1;` | `1` | `ERROR 1142 (42000): UPDATE command denied to user 'epams_app'@'localhost' for table 'audit_log'` | **DENIED (PASS)** |
| `DELETE FROM audit_log WHERE id=1;` | `1` | `ERROR 1142 (42000): DELETE command denied to user 'epams_app'@'localhost' for table 'audit_log'` | **DENIED (PASS)** |
| `DELETE FROM audit_chain_head WHERE id=1;` | `1` | `ERROR 1142 (42000): DELETE command denied to user 'epams_app'@'localhost' for table 'audit_chain_head'` | **DENIED (PASS)** |
| `CREATE TABLE rogue_table (id INT);` | `1` | `ERROR 1142 (42000): CREATE command denied to user 'epams_app'@'localhost' for table 'rogue_table'` | **DENIED (PASS)** |
| `ALTER TABLE service_ticket ADD COLUMN rogue INT;` | `1` | `ERROR 1142 (42000): ALTER command denied to user 'epams_app'@'localhost' for table 'service_ticket'` | **DENIED (PASS)** |
| `DROP TABLE account_status_log;` | `1` | `ERROR 1142 (42000): DROP command denied to user 'epams_app'@'localhost' for table 'account_status_log'` | **DENIED (PASS)** |

*(Additionally verified inside Spring JDBC in `Stage2RealMysqlIntegrationTests.test3` â€” all 3 audit tamper statements threw `DataAccessException`.)*

---

## 3. Credential Externalization, Transport & Log Hygiene (`VERIFIED`)

- **Externalization:** `application-mysql.properties` reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and mandatory `JWT_SECRET` exclusively from environment variables (zero hardcoded production passwords or signing keys).
- **Application Log Leakage Scan:** Verified `SECRET_LEAK_IN_LOG=False` across both `stagec-migrator-boot.log` and `stagec-app-boot.log` â€” neither database passwords nor `JWT_SECRET` appeared anywhere in startup or runtime logs.
- **Transport Security Note (P10 Action):** The local loopback test URL in `application-mysql.properties` currently defaults to `useSSL=false&allowPublicKeyRetrieval=true`. MySQL `8.0.45` confirmed TLS channel support (`mysql_main` encrypted connections enabled). For P10 production deployment, parameterize TLS (`sslMode=VERIFY_IDENTITY` / `useSSL=true`) and remove `allowPublicKeyRetrieval=true` and `createDatabaseIfNotExist=true` from production JDBC URLs.
