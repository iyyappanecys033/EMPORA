# EPAMS — MySQL 8+ Operational & Integration Verification Runbook (Stage 2)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Stage:** Stage 2 — Real MySQL 8+ Integration (Gated Behind Stage 1 Acceptance)  
**Date:** 2026-11-18  
**Current Execution Status:** **`BLOCKED` (`STAGE 1 GATE NOT YET PASSED` & `BLK-01`)**  

---

## 1. Why Stage 2 Has Not Started

Per Section 5.9 (**STAGE 1 ACCEPTANCE GATE**):
> *"Do not begin MySQL integration until: Browser E2E has actually run, SMTP/Mailpit integration has actually run, Dependency scanning has been performed, JaCoCo reports have been generated... If a task remains blocked, do not pretend the gate passed. Record the exact limitation and stop before Stage 2 unless the environment becomes available."*

Because `BLK-02` (Playwright Chromium), `BLK-03` (Online CVE scan), `BLK-04` (JaCoCo plugin), and `BLK-SMTP-EXT` (external Mailpit daemon) remain `BLOCKED` by the offline sandbox — and Docker Desktop daemon is stopped — Stage 2 was intentionally **NOT** started and no H2 results are substituted for MySQL.

---

## 2. Reproducible Stage 2 MySQL 8+ Execution Commands (Disposable DB Only)

```powershell
# 1. Start isolated disposable MySQL 8.0 container (never touch user/prod data)
docker run -d --name epams-mysql-stage2 -e MYSQL_ROOT_PASSWORD=epams_test_pw -e MYSQL_DATABASE=epams_fresh -p 3307:3306 mysql:8.0.45

# 2. Fresh V1-V4 Flyway migration + Hibernate ddl-auto=validate
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_HOST="127.0.0.1"; $env:DB_PORT="3307"; $env:DB_NAME="epams_fresh"
$env:DB_USERNAME="root"; $env:DB_PASSWORD="epams_test_pw"
$env:COOKIE_SECURE="false"
mvn spring-boot:run -f springapp/pom.xml

# 3. Verify Flyway checksums, InnoDB, utf8mb4, unique constraint & EXPLAIN plans
mysql -h 127.0.0.1 -P 3307 -u root -pepams_test_pw epams_fresh -e "
  SELECT version, description, checksum, success FROM flyway_schema_history ORDER BY installed_rank;
  SHOW TABLE STATUS WHERE Engine = 'InnoDB';
  SHOW INDEX FROM in_app_alert WHERE Key_name = 'uk_alert_recipient_dedup';
  EXPLAIN SELECT * FROM in_app_alert WHERE recipient_id = 1 AND is_read = 0 ORDER BY created_at DESC LIMIT 20;
  EXPLAIN SELECT status, COUNT(*) FROM service_ticket WHERE created_at BETWEEN '2026-10-01' AND '2026-11-30' GROUP BY status;
"

# 4. Least-privilege audit_log DB user verification (ADR-011)
# Grant SELECT, INSERT on epams_fresh.audit_log; SELECT, UPDATE on epams_fresh.audit_chain_head; revoke UPDATE, DELETE on audit_log.
```
