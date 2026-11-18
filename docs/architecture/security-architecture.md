# Enterprise Security Architecture & Threat Mitigation
## Employee Lifecycle and Service Management Platform (EPAMS)

**Document ID:** EPAMS-HLD-SEC-001  
**Version:** 1.0.0  
**Date:** November 2026  
**Status:** Approved Architecture Baseline  
**Author:** Principal Security Architect & Senior Java Engineer  

---

## 1. Security Architecture Principles

EPAMS follows the **Defense-in-Depth** and **Zero Trust Architecture** principles:
1. **Stateless Authentication with Dual Token Lifecycle:** Stateless JWT access tokens for API requests combined with rotating, revocable database refresh tokens stored in secure cookies (`FR-001`, `FR-003`, `FR-004`).
2. **Explicit Least-Privilege RBAC:** Access to resources is granted strictly based on authenticated roles (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_RESOLVER`, `ROLE_DEPT_HEAD`, `ROLE_ADMIN`, `ROLE_AUDITOR`).
3. **Department-Level Data Isolation:** Resolvers can only view and mutate service tickets belonging to their own department queue (`FR-013`).
4. **Data Privacy & Field Masking:** Confidential employee compensation (salary) is masked at the service boundary to comply with India's DPDP Act 2023 (`FR-009`).
5. **Non-Repudiation through Cryptographic Chaining:** All administrative, authorization, and lifecycle mutations are sealed in an append-only, SHA-256 hash-chained audit journal (`ADR-011`).

---

## 2. Authentication & Token Lifecycle Architecture

```mermaid
graph TD
    subgraph Client Browser
        AxiosReq["HTTP Request (Axios)"]
        AccessTokenMemory["In-Memory Access Token (15 min)"]
        RefreshTokenCookie["HttpOnly Secure Cookie (7 days)"]
    end

    subgraph EPAMS Ingress Security Filter
        JwtFilter["JwtFilter (OncePerRequestFilter)"]
        SecurityContext["Spring SecurityContextHolder"]
    end

    subgraph Authentication Subsystem
        JwtSvc["JwtService (HMAC-SHA256)"]
        CustomUserDetail["CustomUserDetailsService"]
        RefreshSvc["RefreshTokenService"]
        UserRepo["UserAccountRepo (BCrypt Passwords)"]
        RefreshRepo["RefreshTokenRepo (MySQL)"]
    end

    AxiosReq -->|Authorization: Bearer <jwt>| JwtFilter
    JwtFilter -->|Extract & Validate Token| JwtSvc
    JwtSvc -->|Load User & Roles| CustomUserDetail
    CustomUserDetail --> UserRepo
    JwtFilter -->|Populate Authentication Token| SecurityContext

    AxiosReq -.->|POST /api/v1/auth/refresh (Cookie)| RefreshSvc
    RefreshSvc -->|Verify & Revoke Old Token| RefreshRepo
    RefreshSvc -->|Generate New Rotated Token| RefreshRepo
    RefreshSvc -->|Issue New Access Token| JwtSvc
```

### 2.1 Dual Token Lifecycle Parameters
* **Access Token:**
  * Format: Signed JWT (HMAC-SHA256 with 256-bit secret `jwt.secret`).
  * Claims: `sub` (username), `roles` (list of authorities), `iat`, `exp`.
  * Lifetime: **15 minutes** (900 seconds) to minimize exposure if stolen.
  * Transmission: `Authorization: Bearer <token>` HTTP header.
* **Refresh Token:**
  * Format: Cryptographically secure UUID string (`UUID.randomUUID()`).
  * Storage: Persisted in `refresh_token` table with expiration timestamp, revocation boolean flag, and user linkage.
  * Lifetime: **7 days** (604,800 seconds).
  * Transmission: Transmitted solely via HTTP `Set-Cookie` with flags:
    * `HttpOnly = true` (Inaccessible to browser JavaScript, mitigating XSS exfiltration).
    * `Secure = true` (Transmitted strictly over HTTPS in production).
    * `SameSite = Lax` (Prevents cross-site request forgery on token refresh).
  * **Rotation Policy:** On every refresh call, the presented refresh token is immediately marked `revoked = true`, and an entirely new refresh token is generated and returned. If an already revoked token is presented, all refresh tokens for that user are immediately revoked (compromise detection).

---

## 3. Spring Security 7.x Target Configuration & Migration

While Spring Boot 3.4.12 runs Spring Security 6.4, our target architecture aligns with **Spring Security 7.x** (Spring Boot 4.1.x generation):

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtFilter jwtFilter, CustomUserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepo)
                .ignoringRequestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh",
                    "/actuator/health"
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; object-src 'none'")
                )
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentTypeOptions(Customizer.withDefaults())
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public Ingress Endpoints
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/actuator/health",
                    "/",
                    "/index.html",
                    "/static/**",
                    "/*.ico",
                    "/*.png",
                    "/*.json"
                ).permitAll()

                // Role-Restricted Endpoints
                .requestMatchers("/api/v1/admin/**", "/api/v1/users/*/unlock").hasRole("ADMIN")
                .requestMatchers("/api/v1/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                .requestMatchers("/api/v1/approvals/**").hasAnyRole("MANAGER", "DEPT_HEAD", "ADMIN")
                .requestMatchers("/api/v1/departments/**").hasAnyRole("RESOLVER", "ADMIN")

                // All other application endpoints mandate authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 4. Role-Based Access Control (RBAC) & Permission Matrix

| Capability / API Scope | `ROLE_EMPLOYEE` | `ROLE_MANAGER` | `ROLE_RESOLVER` | `ROLE_DEPT_HEAD` | `ROLE_ADMIN` | `ROLE_AUDITOR` |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| Browse Catalog (`GET /catalog`) | Yes | Yes | Yes | Yes | Yes | Yes |
| Submit Ticket (`POST /tickets`) | Yes | Yes | Yes | Yes | Yes | No |
| View Own Tickets & Profile | Yes | Yes | Yes | Yes | Yes | No |
| Add Public Comments | Yes | Yes | Yes | Yes | Yes | No |
| Add Internal Resolver Comments | No | No | Yes | No | Yes | No |
| Primary Approval (`POST /approvals/*/decide`) | No | **Yes** | No | Yes | Yes | No |
| Secondary Approval (High-Impact) | No | No | No | **Yes** | Yes | No |
| Claim Department Ticket (`POST /assign`)| No | No | **Yes (Own Dept)** | No | Yes | No |
| Mutate Ticket State (`ON_HOLD`, `RESOLVED`)| No | No | **Yes (Own Dept)** | No | Yes | No |
| Cancel Ticket (Pre-Work / Hold) | **Yes (Owner)** | No | No | No | Yes | No |
| Reopen Ticket (Within 48 Business Hrs) | **Yes (Owner)** | No | No | No | No | No |
| Unlock User Accounts | No | No | No | No | **Yes** | No |
| Configure Service Catalog | No | No | No | No | **Yes** | No |
| View Audit Logs & Verify Hash Chain | No | No | No | No | **Yes** | **Yes** |

---

## 5. Department-Level Data Isolation & Data Ownership

To prevent resolvers from viewing or tampering with confidential service requests belonging to other operational units (e.g. an IT resolver reading sensitive HR harassment grievance tickets):

1. **Resolver Department Binding:** Each resolver profile contains a designated department (`profile.department = 'IT'`).
2. **Queue Isolation Enforcement:** `DepartmentQueueService` validates that the caller's assigned department matches the requested queue:
   ```java
   @PreAuthorize("hasRole('ADMIN') or (hasRole('RESOLVER') and #dept.equalsIgnoreCase(principal.department))")
   public Page<ServiceTicketDTO> getQueue(String dept, Pageable pageable) { ... }
   ```
3. **Direct Ticket Access Guard:** When retrieving or modifying a ticket via `/api/v1/tickets/{id}`, an authorization check verifies:
   * Caller is the ticket creator (`requesterUsername == principal.username`), OR
   * Caller is the assigned approver for an active stage, OR
   * Caller is a resolver in the ticket's target department, OR
   * Caller is a System Administrator (`ROLE_ADMIN`).
   Unauthorized requests return **HTTP 403 Forbidden**.

---

## 6. Sensitive Field Masking (Salary Redaction)

In compliance with India's Digital Personal Data Protection (DPDP) Act 2023 and enterprise privacy standards:
* **The Rule:** The `salary` attribute in `Profile` is confidential compensation data (`FR-009`).
* **Enforcement Point:** Redaction is enforced in the **Service Layer** during entity-to-DTO mapping:
  ```java
  public ProfileResponseDTO toDto(Profile profile, UserPrincipal caller) {
      ProfileResponseDTO dto = new ProfileResponseDTO();
      dto.setId(profile.getId());
      dto.setEmployeeId(profile.getEmployeeId());
      dto.setFirstName(profile.getFirstName());
      dto.setLastName(profile.getLastName());
      dto.setDepartment(profile.getDepartment());
      dto.setDesignation(profile.getDesignation());
      dto.setReportingManager(profile.getReportingManager());

      // Mask salary unless caller is HR Administrator or the employee themselves
      boolean canViewSalary = caller.hasRole("ROLE_HR") || 
                              caller.getUsername().equals(profile.getUser().getUsername());
      
      dto.setSalary(canViewSalary ? profile.getSalary() : null);
      return dto;
  }
  ```

---

## 7. CSRF, CORS & Transport Security

### 7.1 Cross-Site Request Forgery (CSRF)
* Standard REST clients pass `X-XSRF-TOKEN` in header.
* Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()` sets the cookie `XSRF-TOKEN` on the browser.
* React's Axios client automatically extracts this cookie and attaches it as `X-XSRF-TOKEN` on all state-altering requests (`POST`, `PUT`, `DELETE`).
* Safe authentication endpoints (`/login`, `/refresh`) are exempted as they are stateless or use credentials.

### 7.2 Cross-Origin Resource Sharing (CORS)
* Whitelists strictly the frontend development host (`http://localhost:3000`) and production host.
* Wildcard `allowedOrigins("*")` is **strictly prohibited**.
* `allowCredentials(true)` enabled to permit cookies.

---

## 8. Brute-Force Defense, Rate Limiting & Account Lockout

1. **Authentication Attempt Tracking (`FR-002`):**
   * Every failed login attempt increments `user_account.failed_login_attempts`.
   * Upon reaching **5 consecutive failures**, the account status is updated to `AccountStatus.LOCKED`, and `locked_until` is set to `now() + 15 minutes`.
   * A security audit event (`ACCOUNT_LOCKED_BRUTE_FORCE`) is committed to the hash-chained audit log.
   * Subsequent login attempts return **HTTP 423 Locked** with a structured message indicating the lockout duration.
   * A successful login resets `failed_login_attempts = 0` and `locked_until = null`.
2. **Administrative Override (`FR-005`):**
   * Administrators can unlock the account immediately via `POST /api/v1/users/{id}/unlock`, resetting counters and logging an `ACCOUNT_UNLOCKED_BY_ADMIN` event.
3. **API Rate Limiting:**
   * Public ingress endpoints (`/api/v1/auth/login`) are throttled using an in-memory Token Bucket filter (Bucket4j) set to **10 requests per minute per client IP**.

