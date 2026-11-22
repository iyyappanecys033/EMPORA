# EMPORA Developer Testing & Quality Gate Runbook

> **Application:** EMPORA — Employee Services & Workflow Platform  
> **Tagline:** One platform. Connected operations.  
> **Stack:** Java 25 (OpenJDK) + Spring Boot 3.5.16 + React 18 + MySQL 8.0+  
> **Repository Structure:**  
> - `springapp/backend`: Maven Spring Boot application  
> - `springapp/frontend`: React 18 Single Page Application  

---

## 1. Quick-Start Test Execution

### 1.1 Backend Unit & Integration Tests (OpenJDK 25)
All backend test commands should be executed from the `springapp/backend` directory.

#### Run Entire Backend Test Suite
```powershell
cd springapp/backend
mvn test -B -Dfrontend.skip=true -XX:+EnableDynamicAgentLoading
```

#### Run OWASP Top 10:2025 Security Unit Tests
Validates AES-256-GCM encryption, LogSanitizer CRLF/PII masking, Token Bucket Rate Limiting, and CSP headers:
```powershell
cd springapp/backend
mvn test -Dtest=OwaspSecurityUnitTests -Dfrontend.skip=true
```

#### Run Module 1 Auth & Platform Suite (CSRF, JWT, Health, Docs)
```powershell
cd springapp/backend
mvn test -Dtest=Module1AuthPlatformIntegrationTests -Dfrontend.skip=true
```

#### Run Core Ticket & Approval Workflow Engine (11-State FSM, SLA, Approvals)
```powershell
cd springapp/backend
mvn test -Dtest=TicketWorkflowTests -Dfrontend.skip=true
```

#### Run Account Lifecycle & IAM Administration Suite (User, Role, Mapping, Status)
```powershell
cd springapp/backend
mvn test -Dtest=AccountLifecycleTests -Dfrontend.skip=true
```

#### Run Analytics, Audit Ledger, & Notification Suites
```powershell
cd springapp/backend
mvn test -Dtest=AnalyticsTests,AuditLogTests,NotificationTests,FlywayMigrationTests -Dfrontend.skip=true
```

#### Run Real MySQL 8 Testcontainers Integration Suite
Requires Docker running locally:
```powershell
cd springapp/backend
mvn test -Dtest=TestHarnessSmokeTest,Stage2RealMysqlIntegrationTests -Dfrontend.skip=true
```

---

### 1.2 Frontend Unit & Component Tests (React 18 + Jest)
All frontend test commands should be executed from the `springapp/frontend` directory.

#### Run Frontend Test Suite
```powershell
cd springapp/frontend
npm test -- --watchAll=false
```

#### Run Frontend Tests with Coverage & HTML Report
```powershell
cd springapp/frontend
npm test -- --watchAll=false --coverage
```

#### Run Security & Sanitization Unit Tests
```powershell
cd springapp/frontend
npm test -- src/utils/securityUtils.test.js --watchAll=false
```

#### Build Production React Bundle
```powershell
cd springapp/frontend
npm run build
```

---

### 1.3 Playwright End-to-End (E2E) Flow Tests

#### Prerequisites
1. Start backend server:
   ```powershell
   cd springapp/backend
   mvn spring-boot:run
   ```
2. Start frontend development server or serve build:
   ```powershell
   cd springapp/frontend
   npm start
   ```

#### Run Playwright Tests (Headless)
```powershell
cd springapp/frontend
npx playwright test
```

#### Run Playwright Tests (Interactive UI Mode)
```powershell
cd springapp/frontend
npx playwright test --ui
```

---

## 2. 5-Minute Production Smoke Test Checklist

Before promoting any build of **EMPORA** to staging or production, perform this deterministic 5-minute verification:

| Step | Verification Target | Action / Command | Expected Result | Pass/Fail |
| :---: | :--- | :--- | :--- | :---: |
| **1** | **Platform Health** | `curl -i http://localhost:8080/api/v1/health` | HTTP `200 OK`, JSON `{"status":"UP"}` | [ ] |
| **2** | **Strictly Gated Portal** | `curl -i http://localhost:8080/api/v1/catalog` | HTTP `401 Unauthorized` (zero unauthenticated data leak) | [ ] |
| **3** | **Double-Submit CSRF** | `curl -i http://localhost:8080/api/auth/csrf` | HTTP `200 OK`, sets `XSRF-TOKEN` cookie | [ ] |
| **4** | **Rate Limiting Guard** | Send 20 rapid `POST /api/auth/login` requests | Requests 1–15 return `401`/`200`, requests 16+ return `429 Too Many Requests` with `Retry-After: 60` | [ ] |
| **5** | **Secure Cookie Issuance** | Authenticate via `POST /api/auth/login` | Returns `Set-Cookie: accessToken=...; HttpOnly; Secure; SameSite=Strict` and `refreshToken=...; HttpOnly; Secure; SameSite=Strict` | [ ] |
| **6** | **State Machine Flow** | Submit ticket -> Assign -> In Progress -> Resolve -> Close | Valid transitions succeed (`200`); illegal jumps (e.g. Assigned -> Closed) return `400 Bad Request` | [ ] |
| **7** | **Anti-Self Approval** | Manager attempts to approve their own ticket | Returns `400 Bad Request` (`MANAGER_CANNOT_APPROVE_OWN_TICKET`) | [ ] |
| **8** | **PII Field Encryption** | Inspect MySQL `profile` table directly (`SELECT emergency_contact, address FROM profile`) | Values stored as Base64 AES-256-GCM ciphertexts with IV prefix, never raw plaintext | [ ] |
| **9** | **Cryptographic Audit** | `GET /api/v1/audit/verify` with `ROLE_AUDITOR` | HTTP `200 OK`, JSON `{"chainValid": true, "totalRecords": N, "tamperedRecords": 0}` | [ ] |
| **10** | **Error Sanitization** | Request non-existent or invalid payload | Standardized RFC 7807 `ProblemDetail` response with `correlationId`; zero stack traces or SQL errors leaked | [ ] |
