# EPAMS — P9.6.1 Test-Count Reconciliation (Stage A Baseline)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6.1 — QA Closure & Independent Evidence Audit  
**Baseline Date:** 2026-11-18 (Pre-Stage D test additions)

---

## 1. Backend Java Test Reconciliation (Exact Primary-Source Counts)

| Test Class (`springapp/src/test/java/com/examly/springapp/`) | `@Test` Annotations Discovered | Execution Mode | Surefire XML Executed | Passed | Failed / Error | Skipped | Notes |
| :--- | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `AccountLifecycleTests` | `36` | Default (H2) | `36` | `36` | `0` | `0` | Verified in XML |
| `AnalyticsTests` | `8` | Default (H2) | `8` | `8` | `0` | `0` | Verified in XML |
| `ApiSecurityTests` | `19` | Default (H2) | `19` | `19` | `0` | `0` | Verified in XML |
| `AuditLogTests` | `24` | Default (H2) | `24` | `24` | `0` | `0` | Verified in XML |
| `BusinessCalendarReopenWindowTests` | `10` | Default (H2) | `10` | `10` | `0` | `0` | Verified in XML |
| `FlywayMigrationTests` | `8` | Default (H2) | `8` | `8` | `0` | `0` | Verified in XML |
| `JwtAuthenticationTests` | `11` | Default (H2) | `11` | `11` | `0` | `0` | Verified in XML |
| `NotificationTests` | `12` | Default (H2) | `12` | `12` | `0` | `0` | Verified in XML |
| `P7P9ConsolidatedAcceptanceTests` | `5` | Default (H2) | `5` | `5` | `0` | `0` | Verified in XML |
| `P96ApiAndFieldValidationTests` | `5` | Default (H2) | `5` | `5` | `0` | `0` | Added in P9.6 |
| `P96RbacFieldSecurityIdorTests` | `5` | Default (H2) | `5` | `5` | `0` | `0` | Added in P9.6 |
| `P96TransactionReliabilityTests` | `5` | Default (H2) | `5` | `5` | `0` | `0` | Added in P9.6 |
| `P96UnitBusinessLogicTests` | `6` | Default (H2) | `6` | `6` | `0` | `0` | Added in P9.6 |
| `P9LiveServerE2EIntegrationTests` | `2` | Default (H2) | `2` | `2` | `0` | `0` | Verified in XML |
| `P9ScalePerformanceTests` | `1` | Default (H2) | `1` | `1` | `0` | `0` | Verified in XML |
| `P9SecurityAndWorkflowQaTests` | `10` | Default (H2) | `10` | `10` | `0` | `0` | Verified in XML |
| `PerformanceBenchmarkTests` | `4` | Default (H2) | `4` | `4` | `0` | `0` | Verified in XML |
| `RbacAuthenticationTests` | `15` | Default (H2) | `15` | `15` | `0` | `0` | Verified in XML |
| `RiskEngineTests` | `13` | Default (H2) | `13` | `13` | `0` | `0` | Verified in XML |
| `Stage1RemediationVerificationTests` | `3` | Default (H2) | `3` | `3` | `0` | `0` | Verified in XML |
| `TicketPersistenceTests` | `10` | Default (H2) | `10` | `10` | `0` | `0`\* | \*XML includes `1` class-level skip entry from `Stage2RealMysqlIntegrationTests` (Surefire `3.0.0-M5` quirk) |
| `TicketWorkflowTests` | `17` | Default (H2) | `17` | `17` | `0` | `0` | Verified in XML |
| **Subtotal — Default Backend Suite (`22` classes)** | **`229`** | **Default (H2)** | **`229`** | **`229`** | **`0`** | **`0` methods (`1` opt-in class skipped)** | Default `mvn test` reports `Tests run: 230, Skipped: 1` (`229` methods + `1` skipped class) |
| `Stage2RealMysqlIntegrationTests` | `7` | **Opt-In (`EPAMS_MYSQL_INTEGRATION=true`)** | `7` | `7` | `0` | `0` (when enabled) | Skipped by default; `7/7` executed & passed against real MySQL `8.0.45` |
| **Grand Total — Unique Backend Test Methods (`23` classes)** | **`236`** | **Default + Opt-In MySQL** | **`236`** | **`236`** | **`0`** | **`0` unexecuted** | **Exact 100% reconciliation (`229 + 7 = 236`)** |

---

## 2. Frontend Jest Test Reconciliation (Stage A Baseline)

| Test Suite (`springapp/springbackend/src/`) | Discovered `test()`/`it()` | Executed | Passed | Failed | Skipped |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `App.test.js` | `21` | `21` | `21` | `0` | `0` |
| `P8Features.test.js` | `7` | `7` | `7` | `0` | `0` |
| `P95UiUxTransformation.test.js` | `8` | `8` | `8` | `0` | `0` |
| `P96FrontendComprehensive.test.js` | `7` | `7` | `7` | `0` | `0` |
| `P9FrontendQa.test.js` | `6` | `6` | `6` | `0` | `0` |
| **Total Frontend Jest Baseline (`5` suites)** | **`49`** | **`49`** | **`49`** | **`0`** | **`0`** |

---

## 3. Browser / E2E / External Verification Reconciliation (Stage A Baseline)

| Layer | Discovered Specs / Checks | Executed in P9.6 | Passed | Failed | Not Tested |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Packaged JAR SPA Deep-Link Checks | `11` routes | `11` | `11` | `0` | `0` |
| Live Chrome 6-Viewport Layout & `axe-core` | `19` route/viewport checks | `19` | `15` clean / `4` (`320px`) flagged `QA-DEF-006` | `0` fatal (`4` Low a11y target-size flags) | `0` |
| HTTP Load / Stress / Spike Benchmarks | `8` profiles (`780` reqs) | `780` | `780` | `0` | `0` |
| `springapp/springbackend/e2e/epams.spec.js` (Playwright) | `1` spec file | `0` in P9.6 (P9.6 used custom CDP/Playwright script) | — | — | Scheduled for Stage E cross-browser execution |
| Firefox & WebKit Browser Suites | — | `0` | `0` | `0` | `NOT TESTED` (Stage E target) |
