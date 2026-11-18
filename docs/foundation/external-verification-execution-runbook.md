# EPAMS — External Verification Execution Runbook (Stage D)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Task:** Final Foundation Readiness — Reproducible Runbook for Online / External Verification Gates  
**Date:** 2026-11-18  

> **Honesty Rule:** This runbook documents exact, reproducible execution procedures for environment-blocked external checks. Having this runbook prepared does **NOT** mark any check below as passed until executed in an eligible online / Docker / MySQL environment.

---

## D.1 Browser E2E Automation (Playwright + Chromium)

- **Required Tooling:** Node.js `>= 20.x` (verified local `v22.23.2`), npm `>= 10.x` (verified `10.9.8`), outbound internet to install `@playwright/test` and Chromium browser binary.
- **Configuration & Spec Locations:**
  - `springapp/springbackend/playwright.config.js`
  - `springapp/springbackend/e2e/epams.spec.js`
- **Test Identities:** Seeded accounts via `DataInitializer` (`admin`/`admin123`, `hr_manager`/`hr123`, `john_doe`/`user123`) plus configurable env overrides (`E2E_MANAGER_USER`, `E2E_SPECIALIST_USER`, `E2E_AUDITOR_USER`).
- **Execution Steps:**
  ```powershell
  # 1. Start backend on :8080 (isolated test DB)
  mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:epams_e2e;MODE=MySQL;DATABASE_TO_LOWER=TRUE --spring.jpa.hibernate.ddl-auto=validate" -f springapp/pom.xml

  # 2. Start React dev server on :3000 (or point EPAMS_BASE_URL=http://localhost:8080 for packaged JAR)
  cd springapp/springbackend; npm start

  # 3. Install Playwright + Chromium once (online runner) and run suite
  cd springapp/springbackend
  npm install -D @playwright/test
  npx playwright install chromium
  npx playwright test --reporter=html,junit
  ```
- **Expected Artifacts:** `springapp/springbackend/playwright-report/index.html`, JUnit XML, failure traces/screenshots.

---

## D.2 Dependency Vulnerability Scanning

- **Frontend (`npm audit` — online runner):**
  ```powershell
  cd springapp/springbackend
  npm audit --omit=dev --audit-level=high
  npm audit --json > npm-audit-report.json
  ```
- **Backend (`Trivy` or `OWASP Dependency-Check` — online runner):**
  ```powershell
  # Option A: Trivy filesystem scan
  trivy fs --scanners vuln --severity HIGH,CRITICAL --format table springapp/pom.xml

  # Option B: OWASP Dependency-Check Maven plugin (requires NVD API key)
  mvn org.owasp:dependency-check-maven:check -DnvdApiKey=$env:NVD_API_KEY -f springapp/pom.xml
  ```
- **Container Image Scan (P10 once image exists):** `trivy image --severity HIGH,CRITICAL epams:candidate`.
- **Governance:** Never run `npm audit fix --force` blindly; document CVE ID, affected version, reachability, patch version, and full regression proof.

---

## D.3 JaCoCo Code Coverage (`-Pcoverage` Maven Profile)

- **Profile Configured in `springapp/pom.xml`:** `<profile><id>coverage</id>` using `org.jacoco:jacoco-maven-plugin:0.8.13` with `<propertyName>jacoco.argLine</propertyName>` safely concatenated into Surefire `<argLine>${jacoco.argLine} -XX:+DisableAttachMechanism</argLine>`.
- **Execution Command (online runner with Maven Central access):**
  ```powershell
  mvn clean test jacoco:report -Pcoverage "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml
  ```
- **Generated Output Locations:**
  - XML: `springapp/target/site/jacoco/jacoco.xml`
  - HTML: `springapp/target/site/jacoco/index.html`
- **Critical Services to Review:** `TicketStateMachine`, `TicketCommandService`, `ApprovalService`, `BusinessCalendar`, `AuditChainService`, `NotificationService`, `NotificationEventListener`, `SlaScheduledProcessor`, `AnalyticsService`, `TicketSecurityEvaluator`, `AuthService`, `RefreshTokenService`.
- **Recommended Exclusions (if thresholds enforced in CI):** DTOs (`com.examly.springapp.dto.**`), Spring Boot entrypoint (`SpringappApplication`), pure JPA entity boilerplate getters/setters.

---

## D.4 External SMTP / Mailpit Verification

- **Important Distinction:** In-process localhost TCP SMTP tests (`P7P9ConsolidatedAcceptanceTests.scenario5`, `Stage1RemediationVerificationTests.test3`) verify RFC 5321/4616/2047 protocol behavior but do **NOT** replace verification against a real Mailpit daemon.
- **Startup & Verification Commands (Docker / Mailpit host):**
  ```powershell
  docker run -d --name epams-mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
  $env:EPAMS_EMAIL_ENABLED="true"
  $env:EPAMS_EMAIL_HOST="127.0.0.1"
  $env:EPAMS_EMAIL_PORT="1025"
  $env:EPAMS_EMAIL_FROM="no-reply@epams.local"
  # Start EPAMS, trigger ticket approval/resolution, and query Mailpit REST API:
  Invoke-RestMethod http://127.0.0.1:8025/api/v1/messages | ConvertTo-Json -Depth 4
  ```
- **Verify:** Real message delivery in Mailpit UI (`http://127.0.0.1:8025`), UTF-8 subject/body rendering, zero duplicate emails on retry (`idempotencyKey`), and in-app alert survival when Mailpit container is stopped (`docker stop epams-mailpit`).

---

## D.5 Stage 2 MySQL 8+ Preparation Checklist (NOT Executed in This Task)

- **Required Engine:** MySQL `8.0.x` (e.g., `8.0.45`) or `8.4 LTS`, InnoDB, `utf8mb4` / `utf8mb4_unicode_ci`.
- **Disposable Container Provisioning (never touch user/production data):**
  ```powershell
  docker run -d --name epams-mysql-stage2 -e MYSQL_ROOT_PASSWORD=epams_stage2_pw -e MYSQL_DATABASE=epams_fresh -p 3307:3306 mysql:8.0.45
  ```
- **Database Privilege Separation (`ADR-011`):**
  - Migration user (`epams_migrator`): DDL privileges on `epams_fresh` for Flyway `V1`–`V4`.
  - Application user (`epams_app`): CRUD on operational tables, **`SELECT, INSERT` ONLY on `audit_log`** (`UPDATE`/`DELETE` explicitly revoked), and `SELECT, UPDATE` on `audit_chain_head` (for `SELECT ... FOR UPDATE` row locking).
- **Required Stage 2 Test Procedures:**
  1. **Fresh V1–V4 Migration:** Empty DB → Flyway `V1`–`V4` → Hibernate `ddl-auto=validate` → verify checksums, InnoDB, `utf8mb4`, `uk_alert_recipient_dedup`.
  2. **Brownfield Migration:** Seed pre-existing V1 IAM schema + legacy users/password hashes/roles/profiles/tokens/audit rows → backup dump → Flyway baseline v1 + migrate `V2`–`V4` → verify zero data loss & unchanged BCrypt hashes.
  3. **Concurrency & Query Plans:** Verify `@Version` optimistic lock, 8-thread `uk_alert_recipient_dedup` uniqueness, 6-thread `audit_chain_head` `FOR UPDATE`, and `EXPLAIN` plans for analytics & notification queries.
