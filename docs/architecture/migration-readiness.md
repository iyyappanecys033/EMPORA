# Framework Migration Readiness & Schema Evolution Plan
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-ARCH-MIG-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved Technical Migration Plan  
**Author:** Principal Software Architect & Senior Java Engineer  

---

## 1. Java & Spring Boot Runtime Migration Strategy

### 1.1 Current Baseline vs. Target Runtime

| Runtime Dimension | Current Development Environment | Target Enterprise Runtime | Migration Delta & Policy |
| :--- | :--- | :--- | :--- |
| **Java Runtime** | OpenJDK 25.0.2 LTS (Eclipse Adoptium) | Java 25 LTS (`eclipse-temurin:25-jre-alpine`) | **Zero Container Delta.** The application compiles and packages on Java 25 in both development and production container stages. |
| **Spring Boot Framework** | Spring Boot 3.4.12 | Spring Boot 4.1.x (GA Target) | **Controlled Phase 2 Roadmap.** Spring Boot 3.4.12 serves as the active working baseline; a planned migration spike occurs upon Spring Boot 4.1 GA. |
| **Spring Security** | Spring Security 6.4 | Spring Security 7.x | Filter chain DSL refactoring; removal of deprecated authentication managers. |
| **JPA / ORM** | Hibernate 6.6.36 | Hibernate 7.x (Jakarta Persistence 3.2) | Namespace and type mapping stability. |
| **JWT Library** | JJWT 0.11.5 | JJWT 0.12.5+ | Migration to immutable parser and builder APIs. |
| **Database Migrations** | Hibernate `ddl-auto=update` | Flyway 10+ with `ddl-auto=validate` | Enforces deterministic SQL migrations with zero runtime auto-DDL. |

---

### 1.2 Official Compatibility Clarification
* **Finding:** While Spring Boot 3.4.12 compiles and executes with Java 25 in local testing, Spring Boot 3.4.x was officially validated by VMware/Broadcom against Java 17, 21, 22, and 23.
* **Policy:** We do not claim Spring Boot 3.4.12 is "certified" for Java 25 in enterprise sales documentation. We classify our current setup as **"Target Runtime: Java 25 LTS with Spring Boot 3.4.12 as verified experimental dev baseline, transitioning to Spring Boot 4.1.x upon certified GA"**.

---

### 1.3 Step-by-Step Migration Roadmap to Spring Boot 4.1.x

When Spring Boot 4.1 reaches General Availability:
1. **Step 1: Dependency Upgrade Spike in Feature Branch:**
   Update `parent` in `pom.xml` to `4.1.0` and run `mvn clean compile`.
2. **Step 2: JJWT 0.12+ API Migration:**
   * Replace deprecated `Jwts.parser().setSigningKey(key).parseClaimsJws(token)`:
   * Migrate to:
     ```java
     SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
     Claims claims = Jwts.parser()
         .verifyWith(key)
         .build()
         .parseSignedClaims(token)
         .getPayload();
     ```
3. **Step 3: Spring Security 7 Filter Chain Alignment:**
   * Refactor security configuration to eliminate deprecated lambda matchers.
   * Verify that `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` and CSRF repositories function identically.
4. **Step 4: Hibernate 7 Dialect & Type Validation:**
   * Verify MySQL 8 dialect mappings and `@Version` optimistic locking behavior.
5. **Step 5: Full Regression Testing:**
   * Execute all 5 automated test suites (`JwtAuthenticationTests`, `AccountLifecycleTests`, `TicketStateMachineTests`, `ApprovalWorkflowTests`, `OptimisticLockingTests`).

---

## 2. Relational Schema Migration & Data Preservation Plan

### 2.1 Existing Database Entities vs. Target Flyway Scripts

```
EXISTING SCHEMA (H2 / MySQL Dev)          TARGET SCHEMA (Flyway V1, V2, V3)
--------------------------------          ----------------------------------
user_account ---------------------------> user_account (+version, +created_at)
role -----------------------------------> role (preserved)
user_role_mapping ----------------------> user_role_mapping (preserved)
profile --------------------------------> profile (+version, +constraints)
refresh_token --------------------------> refresh_token (preserved)
account_status_log ---------------------> account_status_log (preserved)
audit_log ------------------------------> audit_log (+seq, +prev_hash, +curr_hash)
[MISSING] ------------------------------> audit_chain_head (New Table V3)
[MISSING] ------------------------------> service_catalog (New Table V2)
[MISSING] ------------------------------> service_ticket (New Table V2)
[MISSING] ------------------------------> ticket_comment (New Table V2)
[MISSING] ------------------------------> ticket_state_log (New Table V2)
[MISSING] ------------------------------> approval_task (New Table V3)
[MISSING] ------------------------------> notification (New Table V3)
```

---

### 2.2 Zero-Data-Loss Migration Governance

To guarantee that existing user accounts, passwords, and profiles are never dropped or corrupted during Flyway introduction:

1. **Flyway Baseline Configuration:**
   In `application-mysql.properties`:
   ```properties
   # Enable Flyway
   spring.flyway.enabled=true
   # Prevent migration failure when tables already exist in legacy databases
   spring.flyway.baseline-on-migrate=true
   spring.flyway.baseline-version=0
   spring.flyway.baseline-description=Initial Legacy Schema Baseline
   # Validate Hibernate mappings against physical schema
   spring.jpa.hibernate.ddl-auto=validate
   ```
2. **Defensive, Idempotent DDL Syntax:**
   All migration scripts use `CREATE TABLE IF NOT EXISTS` for table generation.
3. **Safe Additive Alterations:**
   For existing tables that require new columns (such as `version` for optimistic locking), migration scripts employ defensive alters:
   ```sql
   -- Safely add version column to user_account if missing
   SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
                      WHERE TABLE_SCHEMA = DATABASE() 
                      AND TABLE_NAME = 'user_account' 
                      AND COLUMN_NAME = 'version');
   SET @stmt = IF(@col_exists = 0, 
                  'ALTER TABLE user_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0;', 
                  'SELECT "Column version already exists in user_account";');
   PREPARE stmt FROM @stmt;
   EXECUTE stmt;
   DEALLOCATE PREPARE stmt;
   ```
4. **Historical Audit Log Backfill Strategy:**
   If legacy unchained audit rows exist in `audit_log`, a one-time migration procedure:
   * Sets sequence numbers $1 \dots K$ chronologically based on `timestamp`.
   * Calculates seed hash chaining from $H_0$ up to $H_K$.
   * Seeds `audit_chain_head` with row `id = 1, current_hash = H_K, last_sequence = K`.
   * Future entries chain seamlessly from $H_K$.

---

## 3. Migration Verification Checklist

- [ ] All table creation scripts tested on clean MySQL instance.
- [ ] Baseline-on-migrate tested on database populated with legacy sample data.
- [ ] User login and password verification confirmed functional after migration.
- [ ] Profile retrieval and salary masking verified after migration.
- [ ] Optimistic locking verified via concurrent thread test asserting `version = 1`.
- [ ] Hash chain verified via `AuditVerificationService.verifyChainIntegrity()`.

