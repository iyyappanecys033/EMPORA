# Technical Project Audit: Employee Portal Account Management System (EPAMS)

**Document ID:** EPAMS-AUD-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved  
**Author:** Senior Enterprise Architect & Senior Full-Stack Lead  

---

## 1. Executive Summary

This document presents a comprehensive, evidence-based technical audit of the **Employee Portal Account Management System (EPAMS)** codebase. The inspection was conducted through source code analysis, dependency verification, and runtime validation.

The current system represents a functional foundational prototype centered on identity management, authentication, role-based security, and basic employee profile persistence. To evolve into an enterprise-grade **Employee Lifecycle and Service Management Platform**, significant architectural enhancements, domain expansions, and operational hardening are required.

---

## 2. Repository Layout & Physical Architecture

```
EPAMS/
├── .github/
│   └── modernize/               # Automated upgrade artifacts & migration hooks
├── .vscode/
│   └── settings.json           # Editor configuration
├── docs/                       # Project documentation (created in Phase M1)
│   └── product-discovery/
├── springapp/                  # Main monolithic project folder
│   ├── .mvn/                   # Maven wrapper configuration
│   ├── pom.xml                 # Maven build configuration (Java & Frontend plugin)
│   ├── mvnw / mvnw.cmd         # Maven wrapper scripts
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/examly/springapp/
│   │   │   │   ├── config/      # Spring configuration (CorsConfig, DataInitializer)
│   │   │   │   ├── controller/  # REST controllers (Auth, User, Role, Profile, Dashboard, Audit, Docs)
│   │   │   │   ├── dto/         # Data transfer objects
│   │   │   │   ├── exception/   # Custom exceptions
│   │   │   │   ├── model/       # JPA entities & enums
│   │   │   │   ├── repository/  # Spring Data JPA repositories
│   │   │   │   ├── security/    # Spring Security 6 & JWT filters, services
│   │   │   │   └── service/     # Business logic interfaces & implementations
│   │   │   └── resources/
│   │   │       ├── application.properties        # Primary configuration (H2 default)
│   │   │       ├── application-dev.properties    # H2 in-memory development profile
│   │   │       └── application-mysql.properties  # MySQL production profile
│   │   └── test/
│   │       ├── java/com/examly/springapp/       # Integration & unit test suites
│   │       └── resources/                       # Test resources
│   ├── springbackend/          # React SPA Frontend (nested inside backend repo)
│   │   ├── build/              # Production static bundle
│   │   ├── public/             # Static HTML & assets
│   │   ├── src/
│   │   │   ├── components/     # React functional components
│   │   │   ├── services/       # Axios API client & AuthContext
│   │   │   ├── App.js / App.css
│   │   │   └── index.js / index.css
│   │   └── package.json        # Frontend dependencies & scripts
│   └── target/                 # Maven build output & compiled artifacts
├── cookies.txt                 # Local development cookie export
├── package-lock.json           # Root package lock artifact
└── README.md                   # Repository overview
```

---

## 3. Technology Stack & Runtimes

### 3.1 Backend
* **Runtime:** OpenJDK 25.0.2 LTS (Build 25.0.2+10-LTS)
* **Framework:** Spring Boot 3.4.12
* **Build System:** Apache Maven 3.9.14 (integrated with `frontend-maven-plugin:1.12.1`)
* **Persistence & ORM:** Spring Data JPA with Hibernate ORM 6.6.36.Final
* **Security Engine:** Spring Security 6 with JJWT (`io.jsonwebtoken:jjwt-api:0.11.5`)
* **Databases:**
  * In-Memory (Dev/Test): H2 Database Engine 2.3.232 (`jdbc:h2:mem:springboot_db`)
  * Relational (Prod/Staging): MySQL 8+ supported via `com.mysql:mysql-connector-j`

### 3.2 Frontend
* **Runtime:** Node.js v22.23.2 / npm 10.9.8 (Maven plugin targets Node v20.16.0)
* **Library:** React 18.2.0
* **Routing:** React Router DOM v6.11.1
* **HTTP Client:** Axios v1.4.0 (configured with CSRF header injection and interceptors)
* **State Management:** React Context API (`AuthContext.js`)

---

## 4. Detailed Component & Module Audit

### 4.1 Data Model & JPA Entities (`com.examly.springapp.model`)

| Entity Name | Table Name | Key Attributes | Relationships | Status |
| :--- | :--- | :--- | :--- | :--- |
| **`UserAccount`** | `user_account` | `id`, `username`, `passwordHash`, `email`, `phone`, `status`, `failedLoginAttempts`, `lockedUntil` | One-to-Many with `UserRoleMapping`, One-to-One with `Profile` | **Implemented & Verified** |
| **`Role`** | `role` | `id`, `roleName` (`ADMIN`, `HR_MANAGER`, `EMPLOYEE`, `USER`) | One-to-Many with `UserRoleMapping` | **Implemented & Verified** |
| **`UserRoleMapping`** | `user_role_mapping` | `id`, `user` (FK), `role` (FK) | Many-to-One with `UserAccount`, Many-to-One with `Role` | **Implemented & Verified** |
| **`Profile`** | `profile` | `id`, `employeeId`, `firstName`, `lastName`, `department`, `designation`, `salary`, `dateOfJoining`, `reportingManager`, `emergencyContact`, `address`, `dob`, `gender` | Many-to-One with `UserAccount` | **Implemented & Verified** |
| **`AuditLog`** | `audit_log` | `id`, `actorUsername`, `action`, `targetUsername`, `details`, `timestamp` | Standalone audit trail | **Implemented & Verified** |
| **`AccountStatusLog`** | `account_status_log` | `id`, `status` | Simple status history | **Implemented & Verified** |
| **`RefreshToken`** | `refresh_token` | `id`, `userId`, `tokenHash`, `revoked`, `createdAt`, `expiresAt` | Foreign key to `UserAccount` | **Implemented & Verified** |
| **`AccountStatus`** | *(Enum)* | `ACTIVE`, `SUSPENDED`, `DISABLED` | Used by `UserAccount` | **Implemented & Verified** |

### 4.2 Repositories & Data Access (`com.examly.springapp.repository`)

| Repository Interface | Custom Query Capabilities | Verification Notes |
| :--- | :--- | :--- |
| `UserAccountRepo` | `findByUsername(String)`, `findByEmail(String)` | Standard Spring Data JPA queries; works cleanly with H2. |
| `RoleRepo` | `findByRoleName(String)` | Basic role retrieval. |
| `UserRoleMappingRepo` | `findByUserId(Long)`, `findByUserIdAndRoleId(Long, Long)` | Used by `CustomUserDetailsService` to assemble Spring Security authorities. |
| `ProfileRepo` | `findByName`, `findByNameAndAddress`, `findByEmployeeId`, `findByUserId`, `findByDepartment`, `searchProfiles(query, dept, Pageable)` | Supports full-text dynamic parameter search across name, department, employee ID, and linked user fields. |
| `AuditLogRepo` | `findAllByOrderByTimestampDesc()`, `findByAction...`, `findByActor...`, `findByTarget...` | Chronological audit querying. |
| `RefreshTokenRepo` | `findByTokenHash(String)`, `deleteByUserId(Long)` | Validates active refresh token validity and supports revocation. |

### 4.3 Service Layer (`com.examly.springapp.service`)

* **`UserAccountServiceImpl`:** Manages user lifecycle operations (`save`, `findById`, `suspendUser`, `disableUser`, `reactivateUser`, `unlockUser`). Implements account lock logic and resets failed attempts.
* **`ProfileServiceImpl`:** Handles employee profile creation, updates, and directory search. Interacts with `AuditLogService` to log profile mutation events.
* **`AuditLogServiceImpl`:** Records system events using current `SecurityContextHolder` authentication or marks unauthenticated attempts as `anonymous`.
* **`RiskAssessmentService`:** Heuristic risk scoring engine evaluating failed attempts, account lock status, and profile completion.
* **`AuthService`:** Manages authentication, token generation, user registration, token refreshing, and refresh token hashing (`SHA-256`).

### 4.4 Security & Authentication Architecture (`com.examly.springapp.security`)

* **Stateless JWT Design:** 
  * Tokens are generated using `io.jsonwebtoken:jjwt-api:0.11.5` signed via HMAC-SHA256 (`SignatureAlgorithm.HS256`) with a 256-bit Base64-decoded secret key.
  * Access tokens have a default lifetime of 15 minutes (`jwt.expiration=900000ms`), and refresh tokens have a 7-day lifetime (`jwt.refresh-expiration=604800000ms`).
  * Tokens are primarily transmitted as HTTP-only cookies (`accessToken` with `Path=/`, `refreshToken` with `Path=/api/auth`), preventing JavaScript XSS access.
  * **Dual Extraction Support:** `JwtFilter` inspects the `accessToken` HTTP-only cookie first; if absent, it gracefully falls back to inspecting the `Authorization: Bearer <token>` HTTP header, enabling both browser and programmatic API clients.
* **Cross-Site Request Forgery (CSRF):** 
  * Configured via `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `setCookiePath("/")`.
  * The server issues an `XSRF-TOKEN` cookie, which is read by the frontend Axios interceptor (`api.js`) and returned in the `X-XSRF-TOKEN` header on state-changing HTTP requests (`POST`, `PUT`, `DELETE`).
  * Explicitly ignores public authentication endpoints (`/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`) where pre-authentication sessions do not yet possess CSRF tokens.
* **Content Security Policy (CSP) & Frame Protection:** 
  * Configured in `SecurityConfig.java`: `default-src 'self' 'unsafe-inline' 'unsafe-eval' http://localhost:3000; connect-src 'self' http://localhost:3000 http://localhost:8080`.
  * Enforces `frameOptions(frame -> frame.deny())` against clickjacking and `contentTypeOptions` (`nosniff`).
* **Brute-Force Lockout Defense:** 
  * Tracks `failedLoginAttempts` in `UserAccount`.
  * After 5 consecutive failed login attempts, `AuthService` computes `lockedUntil = LocalDateTime.now().plusMinutes(15)`.
  * `CustomUserDetailsService` enforces `accountNonLocked = (lockedUntil == null || !lockedUntil.isAfter(now))`.
  * Successful authentication resets `failedLoginAttempts` to 0. An administrator can also manually unlock via `PUT /api/users/{id}/unlock`.

### 4.5 Controller Layer (`com.examly.springapp.controller`)

* **`AuthController`:** Endpoints for `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/auth/logout`.
* **`UserAccountController`:** Endpoints for `/api/users`, `/api/users/me`, `/api/users/{id}/suspend`, `/api/users/{id}/disable`, `/api/users/{id}/reactivate`, `/api/users/{id}/unlock`, `/api/users/{id}/risk`.
* **`ProfileController`:** Endpoints for `/api/profiles`, `/api/profiles/directory` (with pagination/sorting), `/api/profiles/me`, `/api/profiles/{id}/details`, `/api/profiles/employee`.
* **`DashboardController`:** Aggregated counts (`/api/dashboard/metrics`) for user statuses, locked accounts, and risk indicators.
* **`AuditLogController`:** Administrative audit query endpoints (`/api/audit-logs`).
* **`ApiDocsController`:** In-app API endpoint catalog (`/api/docs`).
* **`SpaController`:** Single-page app forwarding handler routing browser hits to `forward:/index.html`.

---

## 5. Implementation Status Categorization

Applying the strict 4-tier assessment criteria:

### Category 1: Implemented and Verified Through Source Inspection
1. **User Identity & Lifecycle:** Entity definition, password hashing via `BCryptPasswordEncoder`, account status transitions (`ACTIVE`, `SUSPENDED`, `DISABLED`), and manual administrative unlock.
2. **Stateless JWT Flow:** Access token generation, verification via `JwtFilter`, and cookie management.
3. **Database-Backed Refresh Token Mechanism:** Token hashing in database, revocation on logout, automatic session re-issuance.
4. **Audit Logging Framework:** Synchronous persistence of user mutation events and login attempts.
5. **Basic RBAC Authority Mapping:** `CustomUserDetailsService` mapping database roles to `ROLE_<NAME>` Spring Security authorities.
6. **Frontend Authentication Context:** Axios request/response interceptors with transparent 401 retry queue.

### Category 2: Partially Implemented
1. **Employee Profile Management:** Basic personal and employment attributes exist, but there is no document attachment, hierarchy visualization, or skill/qualification tracking.
2. **Dashboard Analytics:** Calculates total and status-based user counts, but lacks temporal trend analysis, SLA tracking, or service metrics.
3. **Risk Engine:** Computes static heuristic scores based on failed logins, but lacks contextual signals (IP anomalies, geographic velocity, device fingerprints).
4. **Role Management:** Roles exist as database records and controller annotations, but there is no UI for dynamic role creation or granular permission assignment.

### Category 3: Dependency Present but Implementation Unverified / Incomplete
1. **MySQL Database Connector (`com.mysql:mysql-connector-j`):** Dependency is resolved in Maven, but the application runtime defaults to in-memory H2. Production database initialization, connection pooling benchmarks, and failover behavior are unverified.
2. **Frontend-Maven Integration (`com.github.eirslett:frontend-maven-plugin`):** Present and builds the UI, but couples backend compilation directly to Node/npm execution, slowing down development cycles.
3. **Spring Boot DevTools:** Included as runtime optional dependency, but hot-reloading across the multi-module Maven structure requires manual IDE sync.

### Category 4: Missing (Required for Target Platform)
1. **Employee Service Request Engine:** No data structures, APIs, or UI for tickets, service categories, or SLAs.
2. **Approval Workflow Engine:** No multi-level approval matrix, delegation, or conditional routing.
3. **Document Management & Storage:** No file upload handling, metadata storage, access authorization, or cloud storage integration (S3/MinIO).
4. **Notifications & Escalations:** No email delivery (SMTP/SendGrid), in-app notification center, or automated background escalation schedulers.
5. **Centralized Frontend State Management:** Redux Toolkit is absent; complex workflow states currently rely on fragmented local component state.
6. **DevOps & Containerization:** Missing `Dockerfile`, `docker-compose.yml`, and continuous integration pipelines.

---

## 6. Technical Debt & Security Observations

1. **Synchronous Audit Logging:** Audit logs are written synchronously in the request execution thread. A database bottleneck during audit writing can degrade API response times.
2. **Database Migration Tooling Absent:** Schema evolution relies entirely on `spring.jpa.hibernate.ddl-auto=update`. In production, this can lead to unversioned schema drifts and data loss. Tools like Liquibase or Flyway must be introduced.
3. **In-Memory JWT Secret Fallback:** While configurable via environment variables (`JWT_SECRET`), a static default string exists in `application.properties`. Production deployments must enforce externalized secrets management.
4. **Lack of Rate Limiting / Gateway Protection:** Public endpoints (`/api/auth/login`, `/api/auth/register`) lack IP-based rate limiting, leaving them susceptible to denial-of-service or distributed brute-force attacks.
5. **Coupled Client-Server Build:** Packaging the React SPA into Spring Boot's `/static` directory simplifies single-container deployment but hinders independent frontend scalability and CDN caching.

