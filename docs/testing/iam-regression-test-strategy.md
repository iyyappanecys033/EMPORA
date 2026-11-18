# EPAMS Identity & Access Management (IAM) Regression Test Strategy

**Document ID:** EPAMS-TEST-002  
**Version:** 1.0.0  
**Date:** November 15, 2026  
**Status:** APPROVED & ACTIVE  

---

## 1. Purpose & Scope

The purpose of this strategy is to establish and maintain a dedicated **Identity and Access Management (IAM) Regression Test Suite** for the Employee Lifecycle and Service Management Platform (EPAMS).

As EPAMS expands in Phase 2 to introduce complex domain workflows (Service Catalog, Multi-Level Approvals, Ticketing State Machines, SLA Calculation, and Tamper-Evident Audit Hash Chains), the underlying IAM subsystem must remain completely stable, tamper-proof, and resilient to regression.

---

## 2. Protected Security Boundaries

The IAM regression test suite safeguards the following core security controls:

```
+-----------------------------------------------------------------------------+
|                          EPAMS Security Boundaries                          |
+-----------------------------------------------------------------------------+
|  1. Authentication:                                                         |
|     - BCrypt password hashing ($2a$ / $2y$, minimum 10 rounds)              |
|     - Brute-force account lockout (5 attempts threshold, 15-minute lock)    |
|     - Password complexity enforcement (length, upper, lower, digit, special)|
+-----------------------------------------------------------------------------+
|  2. Session & Token Management:                                             |
|     - HttpOnly cookie transport (accessToken & refreshToken)                |
|     - Secret signature validation & malformed token rejection               |
|     - Immediate revocation upon account status change (SUSPENDED / DISABLED)|
|     - Exclusion of sensitive credentials (passwords, salts) from payloads   |
+-----------------------------------------------------------------------------+
|  3. Authorization & RBAC:                                                   |
|     - Administrative privilege boundary (/api/roles, /api/userRoleMappings) |
|     - Self-service boundary (/api/users/me)                                 |
|     - Cross-user mutation prevention (users cannot modify other accounts)   |
|     - Prevention of self-promotion / fake role injection on registration    |
+-----------------------------------------------------------------------------+
|  4. Audit & Compliance:                                                     |
|     - Masking of credentials and raw tokens in audit event details          |
|     - Append-only preservation upon account deletion                        |
|     - Administrative-only visibility for audit logs (/api/audit-logs)       |
+-----------------------------------------------------------------------------+
|  5. Adaptive Risk Scoring:                                                  |
|     - IP and device anomaly tracking                                        |
|     - Bounded 0-100 risk score calculation                                  |
|     - Step-up authentication and critical lockout triggers                  |
+-----------------------------------------------------------------------------+
```

---

## 3. Regression Suite Architecture

The regression suite consists of 5 dedicated test classes totaling **99 verified automated tests**:

```
springapp/src/test/java/com/examly/springapp/
├── AccountLifecycleTests.java     [36 Tests] -> Registration, lockout, suspension, JWT revocation
├── AuditLogTests.java             [24 Tests] -> Event logging, data masking, role access, immutability
├── JwtAuthenticationTests.java    [11 Tests] -> Cookie transport, signature verification, BCrypt check
├── RbacAuthenticationTests.java   [15 Tests] -> Role separation, privilege escalation guards
└── RiskEngineTests.java           [13 Tests] -> Adaptive risk scoring, step-up triggers
```

### 3.1 Separation of Concerns: IAM vs Business Workflows
* **IAM Tests (`AccountLifecycleTests`, etc.):** Must focus strictly on authentication, authorization, session invalidation, and credential protection. They must NOT be overloaded with ticketing, service catalog, or approval business logic.
* **Business Workflow Tests (Phase 2):** Will reside in separate test packages (`com.examly.springapp.workflow`, `com.examly.springapp.approval`, `com.examly.springapp.sla`, etc.).

---

## 4. Test Execution & CI/CD Guardrails

### 4.1 Local Execution
Developers must execute the IAM suite prior to committing any database migrations or security filter changes:

```bash
# Execute only the IAM regression suite (bypassing frontend packaging)
mvn -f springapp/pom.xml surefire:test -Dtest=AccountLifecycleTests,AuditLogTests,JwtAuthenticationTests,RbacAuthenticationTests,RiskEngineTests -o
```

### 4.2 CI/CD Pipeline Integration
In the automated build pipeline, any regression in the 99 IAM tests triggers a hard build failure (`exit 1`), blocking deployment.

---

## 5. Maintenance Guidelines

1. **Never Weaken Assertions:** Any PR that replaces real assertions with `assertTrue(true)` or relaxes security criteria must be automatically rejected.
2. **Topological Setup Cleanup:** When new entity relationships are added to `UserAccount` (e.g. `ServiceTicket.requester_id`, `ApprovalTask.approver_id`), ensure test `@BeforeEach` or test transactions properly clean up child records before purging parent accounts.
3. **Preserve Password Hashing:** Direct entity manipulation in test setup should use repository methods (`userAccountRepo.save(user)`) when modifying account state to avoid double-encoding BCrypt hashes through `UserAccountService.saveUserAccount()`.

