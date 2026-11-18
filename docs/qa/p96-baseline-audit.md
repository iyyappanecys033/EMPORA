# EPAMS — P9.6 Independent Baseline Audit Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage A: Independent Baseline Audit
**Baseline Commit:** `a4857679c0bde310e4b705a46d7e7ef701bc3901` (`main`)
**Date:** November 2026

---

## 1. Executive Audit Summary

An independent, source-level baseline audit of the complete EPAMS repository was performed prior to designing or implementing P9.6 test suites. All counts below were extracted directly from the filesystem and source code (`springapp/` and `springapp/springbackend/`).

| Dimension | Audited Count / Version | Verification Source |
| :--- | :--- | :--- |
| **Git Branch & Baseline** | `main` @ `a4857679c0bde310e4b705a46d7e7ef701bc3901` (clean working tree at audit start) | `git rev-parse HEAD`, `git status` |
| **Runtime & Framework Stack** | Java `25.0.2` LTS, Spring Boot `3.5.16`, Spring Security `6.5.x`, Hibernate `6.6.x`, React `18.2.0` | `springapp/pom.xml`, `package.json`, `java -version` |
| **Database & Migrations** | MySQL `8.0` (`mysqld`/`mysql`/`mysqldump` present locally) + H2 test mode; Flyway `V1`–`V4` intact | `springapp/src/main/resources/db/migration/` |
| **Backend Java Source Files** | `126` classes under `com.examly.springapp` | Direct directory walk |
| **Backend Controllers** | `18` (`17` in `controller/` + `AuthController` in `security/`) | Source extraction |
| **Backend HTTP Mappings** | `95` total (`86` REST/API endpoints + `9` SPA forward routes in `SpaController`) | Controller annotation extraction |
| **DTOs & Input Fields** | `32` DTOs (`10` Request/Input DTOs containing `44` fields; `22` Response/Analytics DTOs) | DTO source extraction |
| **JPA Entities & Domain Enums** | `13` `@Entity` classes + `4` enums (`AccountStatus`, `ApprovalStatus`, `TicketPriority`, `TicketStatus`) | `model/` source audit |
| **Repositories / Services / Security / Workflow / Notification** | `13` repositories, `19` service interfaces/impls, `8` security classes, `3` workflow classes, `4` notification classes | Package audit |
| **Actual Security Roles (`11`)** | `ADMIN`, `AUDITOR`, `DEPT_HEAD`, `EMPLOYEE`, `FACILITIES_CREW`, `HR_MANAGER`, `HR_SPECIALIST`, `IT_SUPPORT`, `MANAGER`, `RESOLVER`, `USER` | `SecurityConfig`, `@PreAuthorize`, `TicketSecurityEvaluator`, `DataInitializer`, `AuthContext` |
| **Frontend Routes** | `26` declared routes (`25` workspace/admin routes + `*` fallback) | `springapp/springbackend/src/App.js` |
| **Existing Backend Tests** | `19` test classes (`215` declared `@Test` methods: `208` executed in default H2 run, `7` gated by `EPAMS_MYSQL_INTEGRATION=true`) | `springapp/src/test/java/` + Surefire execution |
| **Existing Frontend Tests** | `4` Jest/RTL test suites (`42` tests passing); baseline coverage `51.00%` lines / `43.89%` branches | `npm test -- --coverage` |
| **CI Automation Baseline** | No `.github/workflows/` CI pipeline present (only `.github/modernize/java-upgrade` migration logs) | Repository inspection |
| **Local Security/Browser Tools** | Chrome `157.0.8103.79`, `axe-core 4.11.4`, `npm audit`, MySQL `8.0`; `trivy` & `semgrep` not installed locally | Host binary check |

---

## 2. Early Static & Configuration Defects Identified in Stage A

Independent inspection immediately uncovered five genuine, reproducible defects prior to running P9.6 test suites (tracked formally in `p96-defect-register.md`):

1. **`QA-DEF-001` (Medium — SPA Deep-Link Routing):** `SpaController.java` only forwards legacy Phase 1 paths (`/users`, `/roles`, `/profiles`, `/user-role-mappings`), omitting modern P7/P8/P9.5 routes (`/catalog`, `/tickets/**`, `/approvals`, `/queue/**`, `/audit-chain`, `/notifications/**`, `/analytics/**`, `/profile`, `/preferences`, `/admin/metrics`, `/audit/**`), and `SecurityConfig.java` omits `/profile`, `/preferences`, and `/admin/metrics` from `permitAll()`. Direct browser refresh on those routes fails on the standalone packaged JAR.
2. **`QA-DEF-002` (High — Directory `sortBy` Salary Oracle & 500 Crash):** `ProfileController.getEmployeeDirectory` passes raw user-supplied `sortBy` directly into `Sort.by(sortBy)` without an allowlist. Non-HR/Admin users can sort by `salary` (inferring relative colleague salaries despite response redaction), and an invalid field name throws `PropertyReferenceException` (HTTP `500`).
3. **`QA-DEF-003` (High — Missing Server-Side Validation on Registration & Employee DTOs):** `RegisterRequest`, `LoginRequest`, and `EmployeeRequestDTO` lack Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@PositiveOrZero`) and `AuthController.register` / `ProfileController.saveEmployee` lack `@Valid`, allowing blank/whitespace usernames, 1-character passwords, or negative salaries to reach persistence or return raw non-RFC-7807 errors.
4. **`QA-DEF-004` (High — `GlobalExceptionHandler` Internal Message Exposure & Missing 415/404/400 Handlers):** `GlobalExceptionHandler.handleException(Exception)` leaks raw `e.getMessage()` in HTTP `500` responses whenever the text does not contain `"Exception"`, and lacks explicit handlers for `HttpMediaTypeNotSupportedException` (`415`), `MissingServletRequestParameterException` (`400`), `PropertyReferenceException` (`400`), and `NoResourceFoundException` (`404`).
5. **`QA-DEF-005` (Medium — JaCoCo Coverage Broken on Java 25):** `springapp/pom.xml` used `${jacoco.argLine}` (eager resolution preventing Surefire agent attachment) and JaCoCo `0.8.13` (incompatible with Java 25 class file major version 69; requires `0.8.14` + `@{jacoco.argLine}`).

---

## 3. Baseline Verification Status

- All baseline stack claims (Java 25, Spring Boot 3.5.16, Flyway `V1`–`V4`, 208 H2 tests + 7 opt-in MySQL tests, 42 frontend Jest tests) are **independently verified against source and runtime**.
- Significant testing gaps exist across: pure unit tests for IAM/Approval/Audit/Analytics/Notification edge cases, exhaustive field-by-field validation (`44` DTO fields + query/path params), full 11-role RBAC & field-level access matrix, IDOR/BOLA negative tests, transaction rollback/concurrency/recovery tests, multi-viewport (`6` viewports) browser E2E journeys, and CI quality gates.
