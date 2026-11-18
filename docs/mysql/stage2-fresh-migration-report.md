# EPAMS â€” Stage 2 Fresh MySQL Database Migration Report (Stage C)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Stage:** Stage 2 â€” Real MySQL 8+ Integration (Stage C: Fresh Database Migration)
**Target Database:** `epams_stage2_fresh` on isolated MySQL `8.0.45` (`127.0.0.1:13307`)
**Date:** 2026-11-18
**Verification Status:** **`PASSED (VERIFIED AGAINST REAL MYSQL 8.0.45)`**

---

## 1. Execution Procedure

1. Provisioned empty database `epams_stage2_fresh` (`utf8mb4` / `utf8mb4_unicode_ci`) with schema-scoped migrator user `epams_migrator`@`127.0.0.1`.
2. Booted the actual packaged EPAMS Spring Boot `3.5.16` JAR (`springapp/target/springapp-0.0.1-SNAPSHOT.jar`) with `--spring.profiles.active=mysql` and externalized environment variables (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME=epams_migrator`, `DB_PASSWORD`, `JWT_SECRET`).
3. Probed live health endpoint `GET http://127.0.0.1:18090/api/v1/health`.
4. Applied strict table-level least-privilege grants to runtime user `epams_app`@`127.0.0.1` (`SELECT, INSERT` only on `audit_log`; `SELECT, UPDATE` only on `audit_chain_head`; `SELECT` only on `flyway_schema_history`; zero DDL).
5. Rebooted the actual packaged EPAMS JAR under **`epams_app`** (`--spring.profiles.active=mysql`, `FLYWAY_ENABLED=true`, `ddl-auto=validate`) and probed `GET http://127.0.0.1:18091/api/v1/health`.

---

## 2. Actual Flyway & Hibernate Startup Logs (`VERIFIED`)

### Run 1 â€” Fresh Migration (`epams_migrator`)
```text
INFO org.flywaydb.core.FlywayExecutor         : Database: jdbc:mysql://127.0.0.1:13307/epams_stage2_fresh?... (MySQL 8.0)
INFO o.f.c.i.s.JdbcTableSchemaHistory         : Schema history table `epams_stage2_fresh`.`flyway_schema_history` does not exist yet
INFO o.f.core.internal.command.DbValidate     : Successfully validated 4 migrations (execution time 00:00.036s)
INFO o.f.c.i.s.JdbcTableSchemaHistory         : Creating Schema History table `epams_stage2_fresh`.`flyway_schema_history` ...
INFO o.f.core.internal.command.DbMigrate      : Migrating schema `epams_stage2_fresh` to version "1 - init iam schema"
INFO o.f.core.internal.command.DbMigrate      : Migrating schema `epams_stage2_fresh` to version "2 - service catalog and tickets"
INFO o.f.core.internal.command.DbMigrate      : Migrating schema `epams_stage2_fresh` to version "3 - approvals and audit chain"
INFO o.f.core.internal.command.DbMigrate      : Migrating schema `epams_stage2_fresh` to version "4 - notifications and analytics indexes"
INFO o.f.core.internal.command.DbMigrate      : Successfully applied 4 migrations to schema `epams_stage2_fresh`, now at version v4 (execution time 00:01.809s)
INFO org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.6.53.Final
INFO c.examly.springapp.SpringappApplication  : Started SpringappApplication in 12.61 seconds
HTTP 200: {"status":"UP","database":"UP","timezone":"Asia/Kolkata","timestamp":"2026-11-18T10:07:11.933071700"}
```

### Run 2 â€” Repeated Startup Under Least-Privilege Runtime User (`epams_app`)
```text
INFO o.f.core.internal.command.DbValidate     : Successfully validated 4 migrations (execution time 00:00.042s)
INFO o.f.core.internal.command.DbMigrate      : Schema `epams_stage2_fresh` is up to date. No migration necessary.
INFO c.examly.springapp.SpringappApplication  : Started SpringappApplication in 9.092 seconds
HTTP 200: {"status":"UP","database":"UP","timezone":"Asia/Kolkata","timestamp":"2026-11-18T10:08:20.507987800"}
```
- **Credential Leak Scan on Both Startup Logs:** `SECRET_LEAK_IN_LOG=False` (neither DB passwords nor `JWT_SECRET` appeared in logs).
- **Non-Blocking Hibernate Notice Observed:** `HHH90000025: MySQLDialect does not need to be specified explicitly using 'hibernate.dialect'` (informational deprecation warning in Hibernate 6.6; zero impact on validation or runtime).

---

## 3. Verified `flyway_schema_history` Records

| Rank | Version | Description | Script | Flyway Checksum | Installed By | Success |
| :---: | :---: | :--- | :--- | :---: | :--- | :---: |
| 1 | `1` | `init iam schema` | `V1__init_iam_schema.sql` | `-1144213184` | `epams_migrator` | `1` |
| 2 | `2` | `service catalog and tickets` | `V2__service_catalog_and_tickets.sql` | `1138720496` | `epams_migrator` | `1` |
| 3 | `3` | `approvals and audit chain` | `V3__approvals_and_audit_chain.sql` | `635314025` | `epams_migrator` | `1` |
| 4 | `4` | `notifications and analytics indexes` | `V4__notifications_and_analytics_indexes.sql` | `-687650283` | `epams_migrator` | `1` |

---

## 4. Verified Schema Objects in `epams_stage2_fresh`

- **Tables (`14 / 14` present, all `InnoDB` + `utf8mb4`):** `account_status_log`, `approval_task`, `audit_chain_head`, `audit_log`, `flyway_schema_history`, `in_app_alert`, `profile`, `refresh_token`, `role`, `service_catalog`, `service_ticket`, `ticket_comment`, `user_account`, `user_role_mapping`.
  - *Collation Note:* Tables created with `DEFAULT CHARSET=utf8mb4` (without an explicit `COLLATE` clause) adopt MySQL 8.0's default `utf8mb4` collation (`utf8mb4_0900_ai_ci`), while `flyway_schema_history` inherits the database default (`utf8mb4_unicode_ci`). Both are case-insensitive `utf8mb4` collations and Hibernate `ddl-auto=validate` passed with zero warnings or mismatches.
- **Foreign Keys (`13 / 13` verified):** `fk_approval_approver`, `fk_approval_ticket`, `fk_alert_recipient`, `fk_alert_ticket`, `fk_profile_user`, `fk_refresh_token_user`, `fk_ticket_assignee`, `fk_ticket_catalog`, `fk_ticket_requester`, `fk_comment_author`, `fk_comment_ticket`, `fk_urm_role`, `fk_urm_user`.
- **Unique Indexes (`9 / 9` verified):** `uk_user_account_username`, `uk_role_role_name`, `uk_profile_employee_id`, `uk_refresh_token_hash`, `uk_service_catalog_item_code`, `uk_ticket_number`, `uk_approval_ticket_stage`, `uk_audit_log_sequence`, `uk_alert_recipient_dedup`.
- **Genesis Anchor (`audit_chain_head`):** Verified `id=1`, `last_sequence=0`, `current_hash=0000...0000` (64 zeros).
- **Hibernate `ddl-auto=validate`:** **PASSED** across all 13 JPA entities against real MySQL `8.0.45` without modifying any Flyway `V1`â€“`V4` migration or JPA entity.
