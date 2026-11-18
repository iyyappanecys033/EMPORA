# EPAMS P9 — End-to-End Integration & Browser Automation Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P9 — Quality Assurance, Security, Reliability & Release Verification  
**Date:** 2026-11-17  

---

## 1. Dual-Layer E2E Verification Summary (Honest Separation)

| E2E Verification Layer | Harness & Tooling | Execution Status | Journeys Covered |
| :--- | :--- | :---: | :--- |
| **Live-Server Full-Stack HTTP E2E** | `P9LiveServerE2EIntegrationTests.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, real Apache Tomcat 10.1.49 TCP socket, `java.net.http.HttpClient` + `CookieManager`) | **`VERIFIED`** (`2/2` passing in `task-3610`, `5.093 s`) | `E2E-01` through `E2E-08` (all 8 workflows, real cookie jars, real CSRF headers, 6 distinct logged-in persona sessions) |
| **Headless Chromium Browser UI E2E** | `springapp/springbackend/playwright.config.js` + `springapp/springbackend/e2e/epams.spec.js` | **`BLOCKED`** (Playwright npm package & browser binary installation blocked by offline sandbox; suite authored and ready for P10/CI) | `E2E-01` through `E2E-08` (DOM rendering, SPA routing, `localStorage` non-leakage) |
| **Frontend Component & Route Guard E2E Simulation** | Jest + React Testing Library (`App.test.js` 21, `P8Features.test.js` 7, `P9FrontendQa.test.js` 5) | **`VERIFIED`** (`33/33` passing in `task-3478`, `27.01 s`) | Protected routes (403), session-expired clearing, duplicate submit protection, NotificationBell/Page, AnalyticsDashboard |

---

## 2. Critical Real-World Defect Uncovered by Live-Server HTTP E2E (`SEC-P9-06`)

Executing `P9LiveServerE2EIntegrationTests` over a real TCP Tomcat instance with `HttpClient` + `CookieManager` exposed a **CRITICAL production-blocking Spring Security 6 stateless SPA CSRF bug** that all prior MockMvc `.with(csrf())` tests completely missed:

1. `AuthController.getCsrfToken()` did not materialize Spring Security 6's deferred `CsrfToken`, so `XSRF-TOKEN` was never written on `/api/auth/csrf`.
2. Spring Security 6's default `XorCsrfTokenRequestAttributeHandler` rejected raw `X-XSRF-TOKEN` headers echoed by React/Axios (`api.js`) from `CookieCsrfTokenRepository`.
3. `SessionManagementFilter`'s default `CsrfAuthenticationStrategy` detected every stateless JWT authentication as a "new login" and emitted `Set-Cookie: XSRF-TOKEN=; Max-Age=0`, **wiping the client's CSRF cookie on every authenticated `GET` or `POST` request**.
4. Missing `AccessDeniedHandler` caused `CsrfFilter` rejections to dispatch to `/error` and return misleading `401 Unauthorized` instead of `403 Forbidden`.

**Remediation Applied & Verified:** Configured `SpaCsrfTokenRequestHandler`, `CsrfCookieFilter`, `NullAuthenticatedSessionStrategy`, materialized `GET /api/auth/csrf`, and explicit RFC 7807 `403` `AccessDeniedHandler` in `SecurityConfig.java` and `AuthController.java`. Re-running `P9LiveServerE2EIntegrationTests` and the full 190-test Maven suite verified **100% pass** with zero regressions.

---

## 3. Journey-by-Journey Verification Matrix (`E2E-01` to `E2E-08`)

| Journey ID | End-to-End Scenario | Live HTTP E2E (`P9LiveServerE2EIntegrationTests`) | Browser UI E2E (Playwright) |
| :---: | :--- | :---: | :---: |
| **E2E-01** | Login cookie attributes (`HttpOnly`, `SameSite=Lax`, `Path=/api/auth` for refresh), `GET /api/users/me`, CSRF 403 block without `X-XSRF-TOKEN`, silent refresh, CSRF-protected logout revoking session (`401` afterward) | `VERIFIED` (`test1`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-02** | Employee browses Service Catalog (`GET /api/v1/catalog?department=IT`) and creates `URGENT` approval-required ticket (`201 Created`, `PENDING_APPROVAL`) | `VERIFIED` (`test2`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-03** | Department Specialist views IT queue, assigns ticket (transitions to `IN_PROGRESS`), posts internal + public comments (requester sees public only), resolves ticket (`RESOLVED`) | `VERIFIED` (`test2`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-04** | Manager checks pending approvals (`GET /api/v1/approvals/pending`), approves ticket (`POST /api/v1/approvals/{id}/decide`), transitions ticket to `ASSIGNED` | `VERIFIED` (`test2`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-05** | Live persisted notifications triggered for Manager (`APPROVAL_REQUIRED`) and Employee (`TICKET_SUBMITTED`, `TICKET_APPROVED`, `TICKET_RESOLVED`); cross-user mark-read blocked (`403`); owner mark-read & mark-all-read reset unread count to `0` | `VERIFIED` (`test2`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-06** | Operational Analytics dashboard returns exact period count (`1`) and resolved count (`1`) scoped to `IT`; Employee blocked (`403`); IT Specialist blocked from `?department=HR` (`403`) | `VERIFIED` (`test2`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-07** | Auditor runs `GET /api/v1/audit/verify` returning `valid: true` across full lifecycle audit trail; Employee blocked (`403`) | `VERIFIED` (`test2`) | `BLOCKED` (Authored in `epams.spec.js`) |
| **E2E-08** | Cross-user & cross-department security boundaries: unrelated employee blocked from ticket (`403`) and approval history (`403`), Facilities specialist blocked from IT queue (`403`), cross-user notification mutation blocked (`403`) | `VERIFIED` (`test1`, `test2`) | `BLOCKED` (Authored in `epams.spec.js`) |

---

## 4. Reproducible P10 Playwright Execution Command

Once deployed or running in a networked P10/CI runner:
```powershell
cd springapp/springbackend
npm install -D @playwright/test
npx playwright install chromium
npx playwright test
```

