# EPAMS — P9 Final Database Validation Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Consolidated Database & Migration Validation  
**Date:** 2026-11-18  

---

## 1. Real MySQL 8.0+ Environment Probe Evidence

Executed read-only host connectivity probe on `2026-11-18`:
- **Host MySQL Binary:** `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe Ver 8.0.45 for Win64`
- **TCP Port `127.0.0.1:3306`:** Reachable (`TcpTestSucceeded: True`).
- **Authentication Result:** Host MySQL rejected unauthenticated `root` and default profile password (`ERROR 1045 (28000): Access denied for user 'root'@'localhost'`). No MySQL test credentials are provided in the environment; guessing or bypassing credentials is strictly prohibited.
- **Docker Engine Probe:** `docker info` failed (`open //./pipe/docker_engine: The system cannot find the file specified`) — Docker Desktop daemon is not running, so Testcontainers / ephemeral Docker MySQL cannot be started.
- **Live MySQL 8.0+ Status:** **`BLOCKED` (`BLK-01`)** — Mandatory P10 entry gate.

---

## 2. H2 MySQL Compatibility Mode Verification (Not a Substitute for Live MySQL)

- **Flyway V1–V4:** All four historical migrations (`V1`–`V4`) preserved **100% unmodified** and validated cleanly (`FlywayMigrationTests` 8/8 passing — fresh V1–V4 + brownfield V1 data preservation).
- **Hibernate Validation:** `spring.jpa.hibernate.ddl-auto=validate` verified across all entities (including `ServiceCatalogItem` → `service_catalog`, `ServiceTicket` `@Version`, `InAppAlert` `uk_alert_recipient_dedup`).
- **Concurrency Constraints:** `uk_alert_recipient_dedup` verified under 8 concurrent threads; `audit_chain_head` `FOR UPDATE` verified under 6 concurrent threads; `@Version` optimistic locking verified under conflicting concurrent updates.
