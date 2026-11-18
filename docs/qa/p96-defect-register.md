# EPAMS — P9.6 Defect Register & Remediation Log (Stage N)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9.6 — Enterprise Full-Stack QA & Pre-Deployment Verification (Stage N)  
**Audit Date:** 2026-11-18  
**Baseline Commit:** `a4857679c0bde310e4b705a46d7e7ef701bc3901` (`main`)

---

## 1. Defect Summary by Severity & Disposition

| Severity | Found | Fixed & Re-Verified in P9.6 / P9.6.1 | Open / Deferred (Documented) | Release Impact |
| :--- | :---: | :---: | :---: | :--- |
| **Critical** | `0` | `0` | `0` | None |
| **High** | `3` | `3` (`QA-DEF-001`, `QA-DEF-002`, `QA-DEF-007`) | `0` | Zero open High defects |
| **Medium** | `3` | `3` (`QA-DEF-003`, `QA-DEF-004`, `QA-DEF-005`) | `0` | Zero open Medium defects |
| **Low** | `1` | `1` (`QA-DEF-006`) | `0` | Zero open Low defects |
| **Total** | **`7`** | **`7`** | **`0`** | **Zero open defects across all severities** |

---

## 2. Detailed Defect Register

### `QA-DEF-001` — Standalone Packaged JAR Missing SPA Fallback Routes (`High` — **FIXED & VERIFIED**)
- **Discovery Phase:** Stage A / Stage H (Live standalone executable JAR route audit).
- **Root Cause:** `SpaController` forwarded legacy routes (`/dashboard`, `/employees`, etc.) to `index.html`, omitting the 11 modern enterprise routes introduced in P9.5 (`/catalog`, `/tickets/**`, `/approvals`, `/queue/**`, `/audit-chain`, `/notifications/**`, `/analytics/**`, `/profile`, `/preferences`, `/admin/metrics`, `/audit/**`), and `SecurityConfig` omitted `/profile`, `/preferences`, `/admin/metrics` from static SPA `permitAll()`. Direct browser deep-link or page refresh on those URLs against the standalone Spring Boot JAR returned HTTP `404`/`401` instead of serving the React SPA shell.
- **Remediation:**
  - Updated [SpaController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/SpaController.java) to forward all modern SPA routes to `forward:/index.html`.
  - Updated [SecurityConfig.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/security/SecurityConfig.java) to permit SPA route shell delivery for `/profile`, `/preferences`, and `/admin/metrics` (all backing `/api/**` endpoints remain strictly authenticated & RBAC-protected).
- **Verification Evidence:**
  - Automated MockMvc verification in `P96ApiAndFieldValidationTests` (`12` SPA routes → HTTP `200`).
  - Live standalone packaged JAR (`springapp-0.0.1-SNAPSHOT.jar`) verification across all `11` routes (`scratch/p96-live-results.json`: all `11/11` returned HTTP `200` with `servesReactIndex: true`).

---

### `QA-DEF-002` — Employee Directory `sortBy` Parameter Lacked Allowlist (Salary Ordering Oracle & HTTP 500) (`High` — **FIXED & VERIFIED**)
- **Discovery Phase:** Stage F / Stage G (API negative validation & field-level security audit).
- **Root Cause:** `GET /api/employees/directory?sortBy=...` passed user-supplied `sortBy` directly into Spring Data `Sort.by(sortBy)`. Supplying `sortBy=salary` as a non-HR employee allowed inferring relative salary ranking (field-level PII oracle), and supplying an unknown property name triggered an unhandled `PropertyReferenceException` (HTTP `500`).
- **Remediation:**
  - Added strict allowlist validation in [ProfileController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/ProfileController.java): non-HR/Admin callers are restricted strictly to `{id, firstName, lastName, department, designation, employeeId, dateOfJoining}`; HR/Admin callers may additionally sort by `{salary, dateOfBirth}`. Invalid or unauthorized `sortBy` values return clean RFC 7807 HTTP `400 Bad Request`.
- **Verification Evidence:**
  - `P96ApiAndFieldValidationTests` & `P96RbacFieldSecurityIdorTests`: `sortBy=salary` as `john_doe` → HTTP `400`; `sortBy=nonExistentField` → HTTP `400`; valid `sortBy=firstName` → HTTP `200`; `size=500` clamped cleanly.

---

### `QA-DEF-003` — Missing Jakarta Bean Validation Annotations on Auth & Employee Request DTOs (`Medium` — **FIXED & VERIFIED**)
- **Discovery Phase:** Stage F (Field validation inventory across all 10 request DTOs / 44 fields).
- **Root Cause:** `RegisterRequest`, `LoginRequest`, and `EmployeeRequestDTO` lacked Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@PositiveOrZero`) and `AuthController` / `ProfileController` omitted `@Valid`, allowing empty usernames, malformed emails, oversized fields, or negative salaries to reach service/persistence layers before failing inconsistently.
- **Remediation:**
  - Added Jakarta validation constraints to [RegisterRequest.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/dto/RegisterRequest.java) (`password` min `4` preserving backward compatibility with existing `RbacAuthenticationTests.test14_FakeRoleInPayload_Rejected`), [LoginRequest.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/dto/LoginRequest.java), and [EmployeeRequestDTO.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/dto/EmployeeRequestDTO.java).
  - Enabled `@Valid` on [AuthController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/AuthController.java) and [ProfileController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/ProfileController.java).
- **Verification Evidence:**
  - `P96ApiAndFieldValidationTests` verifies blank login, invalid email, oversized username, negative salary → structured RFC 7807 HTTP `400` with field error map. Full `229/229` regression passed.

---

### `QA-DEF-004` — Inconsistent HTTP 415/400/404 Handling & Raw `e.getMessage()` Leakage in HTTP 500 Handler (`Medium` — **FIXED & VERIFIED**)
- **Discovery Phase:** Stage E / Stage J (API contract & error information-disclosure audit).
- **Root Cause:** `GlobalExceptionHandler` lacked explicit RFC 7807 handlers for `HttpMediaTypeNotSupportedException`, `MissingServletRequestParameterException`, `PropertyReferenceException`, and `NoResourceFoundException` (allowing them to fall through to generic `500`), and the fallback `Exception` handler serialized raw `e.getMessage()`, risking internal class/SQL detail exposure.
- **Remediation:**
  - Added explicit RFC 7807 handlers (`415`, `400`, `400`, `404`) in [GlobalExceptionHandler.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/exception/GlobalExceptionHandler.java) and replaced raw `e.getMessage()` in HTTP `500` responses with `"An unexpected internal error occurred."`.
- **Verification Evidence:**
  - `P96ApiAndFieldValidationTests` verifies `text/plain` POST → HTTP `415`, missing required query params → HTTP `400`, invalid sort field → HTTP `400`, malformed JSON → HTTP `400`.

---

### `QA-DEF-005` — JaCoCo Coverage Instrumentation Broken on Java 25 (`Medium` — **FIXED & VERIFIED**)
- **Discovery Phase:** Stage A / Stage P (Coverage measurement baseline).
- **Root Cause:** `springapp/pom.xml` configured `maven-surefire-plugin` with eager `${jacoco.argLine}` (not late-bound `@{jacoco.argLine}`) and `jacoco-maven-plugin` `0.8.13` (which rejects Java 25 class file major version 69 with `NoClassDefFoundError` during bytecode instrumentation).
- **Remediation:**
  - Updated [springapp/pom.xml](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/pom.xml) to `<argLine>@{jacoco.argLine} -XX:+DisableAttachMechanism</argLine>` and upgraded `jacoco-maven-plugin` `0.8.13` → `0.8.14`.
- **Verification Evidence:**
  - `mvn test -o -Dskip.npm=true -Pcoverage` succeeded (`229/229` passed, `114` classes analyzed, **`86.40%` line / `66.62%` branch coverage**).

---

### `QA-DEF-006` — Compact Mobile (`320×700`) Topbar Icon Target-Size Warning (`Low` — **FIXED & VERIFIED IN P9.6.1**)
- **Discovery Phase:** Stage I (P9.6 live Chrome `157.0.8103.79` + `axe-core 4.11.4` 6-viewport audit).
- **Root Cause:** `.ep-icon-btn` and `.ep-search-trigger` lacked `flex-shrink: 0` and minimum dimensions, causing flexbox to compress topbar controls below `24×24px` at `320px` width.
- **Remediation (P9.6.1 Stage B):** Added `flex-shrink: 0`, `min-width/height: 36px`, visible `:focus-visible` outlines, `.ep-profile-trigger` styling, and `@media (max-width: 420px)` compact brand/spacing rules in [Navbar.js](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/components/layout/Navbar.js) and [components.css](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/styles/design-system/components.css).
- **Verification Evidence:** Live Chrome `157.0.8103.79` + `axe-core 4.11.4` (`task-5004`): all `6` interactive `.ep-topbar` targets at `320×700` measured `34–44 px`; **`32 / 32`** Light & Dark checks across `320×700`, `390×844`, `768×1024`, `1440×900` achieved **`0` violations** and **`0` horizontal overflow**.

---

### `QA-DEF-007` — `LoginModal` Error Alert Immediately Cleared After Failed Authentication (`High` — **FIXED & VERIFIED IN P9.6.1**)
- **Discovery Phase:** P9.6.1 Stage E (Live cross-browser invalid-login form-validation test).
- **Root Cause:** [LoginModal.js](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/components/LoginModal.js) included `submitting` in the `useEffect` dependency array that called `setErrorObj(null)`. When a failed login caught an error, called `setErrorObj(...)`, and then executed `finally { setSubmitting(false); }`, React re-ran the effect and immediately cleared `errorObj` before users could read the authentication error.
- **Remediation (P9.6.1 Stage E):** Split the open/focus reset effect (`[isOpen]`) from the `Escape` keydown listener effect (`[isOpen, submitting, onClose]`).
- **Verification Evidence:** Added dedicated regression test in [P961CriticalFrontendCoverage.test.js](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/springbackend/src/P961CriticalFrontendCoverage.test.js) (`70/70` Jest passed) and verified live `01_form_validation` persisted `[role="alert"]` across **Chromium `157.0.8103.79`, Firefox `144.0.2`, and WebKit `26.0`**.

---

## 3. Production Source Files Modified During P9.6 (Audit Trail)

| File | Defect ID | Change Summary | Flyway Modified? |
| :--- | :---: | :--- | :---: |
| [SpaController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/SpaController.java) | `QA-DEF-001` | Added 11 modern SPA routes to `forward:/index.html` | No |
| [SecurityConfig.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/security/SecurityConfig.java) | `QA-DEF-001` | Added `/profile`, `/preferences`, `/admin/metrics` to SPA shell `permitAll()` | No |
| [ProfileController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/ProfileController.java) | `QA-DEF-002`, `QA-DEF-003` | Added role-aware `sortBy` allowlist & `@Valid` on employee create/update | No |
| [AuthController.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/controller/AuthController.java) | `QA-DEF-003` | Added `@Valid` on `login` and `register` endpoints | No |
| [RegisterRequest.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/dto/RegisterRequest.java) | `QA-DEF-003` | Added `@NotBlank`, `@Size`, `@Email` validation annotations | No |
| [LoginRequest.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/dto/LoginRequest.java) | `QA-DEF-003` | Added `@NotBlank`, `@Size` validation annotations | No |
| [EmployeeRequestDTO.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/dto/EmployeeRequestDTO.java) | `QA-DEF-003` | Added `@NotBlank`, `@Size`, `@PositiveOrZero` validation annotations | No |
| [GlobalExceptionHandler.java](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/src/main/java/com/examly/springapp/exception/GlobalExceptionHandler.java) | `QA-DEF-004` | Added RFC 7807 `415`/`400`/`404` handlers; sanitized `500` error message | No |
| [springapp/pom.xml](file:///c:/Users/Iyyappan%20E/Folders/SDE/Round%203-HR%20Interview/PROJECTS/Projects/EPAMS/springapp/pom.xml) | `QA-DEF-005` | Fixed `@{jacoco.argLine}` & upgraded JaCoCo `0.8.13` → `0.8.14` | No |
| **Flyway `V1`–`V4` (`db/migration/`)** | — | **ZERO MODIFICATIONS** (`git diff -- springapp/src/main/resources/db/migration/` = empty) | **Untouched** |

