# EPAMS — TASK 07E.1 INDEPENDENT VERIFICATION REPORT
## API Security, Transaction Isolation & Regression Evidence Audit

**Document ID:** EPAMS-AUDIT-07E1-001  
**Audit Date:** November 15, 2026  
**Auditor Role:** Principal Security Architect, Independent QA Test Auditor & Database Specialist  
**Target Milestone:** Task 07E Implementation Review prior to Task 07F Authorization  
**Current Status:** COMPLETE & VERIFIED  
**Final Decision:** **CONDITIONALLY APPROVED FOR TASK 07F**  

---

## 1. Executive Verdict

An independent source code, test execution, cryptographic ledger, and HTTP contract verification of the Task 07E implementation was conducted on the OpenJDK 25 / Spring Boot 3.4.12 runtime.

### Summary of Verdict:
1. **Regression Safety & Test Evidence:** **[VERIFIED]**  
   All **152 automated tests** across all 9 test classes executed and passed with 0 failures, 0 errors, and 0 skipped tests (Maven exit code 0, elapsed testsuite time 57.088s, wall clock ~1m 00s). Zero tests were disabled, commented out, or excluded.
2. **Test Isolation & Order Independence:** **[VERIFIED]**  
   Reordered and reversed test execution (`TicketWorkflowTests` $\rightarrow$ `FlywayMigrationTests` $\rightarrow$ `ApiSecurityTests` $\rightarrow$ `JwtAuthenticationTests` $\rightarrow$ `RbacAuthenticationTests`) executed cleanly with 0 failures. Referential integrity constraint violations and audit hash sequence gaps across test class boundaries have been fully resolved.
3. **API Security & Access Control:** **[VERIFIED]**   
   Method security (`@PreAuthorize`), role authority normalization (`ROLE_` prefix handling), departmental queue containment, requester IDOR protection, internal comment privacy, anti-self-approval enforcement, and CSRF defense for cookie-based state mutations are actively enforced and verified.
4. **HTTP Contracts & Validation:** **[VERIFIED]**  
   Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`) is enforced on all mutation DTOs with RFC 7807 problem details emitted for invalid payloads. Optimistic locking failure correctly returns HTTP 409 Conflict.
5. **Contract Documentation Minor Gaps:** **[REPORT-ONLY]**  
   Two endpoint route naming discrepancies exist between `task-07e-rest-api-contracts.md` and the actual controller implementations:
   - Department Queue: Controller uses `/api/v1/departments/{department}/queue` (docs noted `/api/v1/queue/{department}`).
   - Approval Decision: Controller uses `POST /api/v1/approvals/{taskId}/decide` (docs noted `.../decision`).
6. **Live MySQL Execution:** **[BLOCKED]**  
   All tests ran against in-memory H2 in MySQL compatibility mode. Live MySQL 8.0+ verification remains blocked pending external database container/service availability.

---

## 2. Actual Test Execution Evidence

### 2.1 Surefire XML Report Evidence
Every test suite report in `springapp/target/surefire-reports/TEST-*.xml` was parsed and verified:

| Test Suite Class | Tests Executed | Failures | Errors | Skipped | Elapsed Time | Surefire XML Evidence File | Finding Classification |
|:---|:---:|:---:|:---:|:---:|:---:|:---|:---:|
| `AccountLifecycleTests` | 36 | 0 | 0 | 0 | 27.344 s | `TEST-com.examly.springapp.AccountLifecycleTests.xml` | **VERIFIED** |
| `ApiSecurityTests` | 19 | 0 | 0 | 0 | 10.334 s | `TEST-com.examly.springapp.ApiSecurityTests.xml` | **VERIFIED** |
| `AuditLogTests` | 24 | 0 | 0 | 0 | 1.432 s | `TEST-com.examly.springapp.AuditLogTests.xml` | **VERIFIED** |
| `FlywayMigrationTests` | 7 | 0 | 0 | 0 | 3.189 s | `TEST-com.examly.springapp.FlywayMigrationTests.xml` | **VERIFIED** |
| `JwtAuthenticationTests` | 11 | 0 | 0 | 0 | 2.733 s | `TEST-com.examly.springapp.JwtAuthenticationTests.xml` | **VERIFIED** |
| `RbacAuthenticationTests` | 15 | 0 | 0 | 0 | 8.340 s | `TEST-com.examly.springapp.RbacAuthenticationTests.xml` | **VERIFIED** |
| `RiskEngineTests` | 13 | 0 | 0 | 0 | 2.164 s | `TEST-com.examly.springapp.RiskEngineTests.xml` | **VERIFIED** |
| `TicketPersistenceTests` | 10 | 0 | 0 | 0 | 0.257 s | `TEST-com.examly.springapp.TicketPersistenceTests.xml` | **VERIFIED** |
| `TicketWorkflowTests` | 17 | 0 | 0 | 0 | 1.295 s | `TEST-com.examly.springapp.TicketWorkflowTests.xml` | **VERIFIED** |
| **Consolidated Total** | **152** | **0** | **0** | **0** | **57.088 s** | **9 / 9 Suites Present** | **VERIFIED** |

### 2.2 Maven Build Command & Exit Code
- **Command:** `mvn test "-Dskip.installnodenpm=true" "-Dskip.npm=true" -f springapp/pom.xml`
- **Exit Code:** `0` (BUILD SUCCESS)
- **Maven Output Log Evidence:** Total time: 01:00 min, Finished at: 2026-11-15T20:47:36+05:30.

---

## 3. Test-Count Reconciliation & Delta Analysis

### 3.1 Reconciliation Against Previous 133 Baseline
- **Pre-Task 07E Actual Baseline:** 133 tests ($36 + 24 + 7 + 11 + 15 + 13 + 10 + 17 = 133$).
- **Tests Added in Task 07E:** 19 tests in `ApiSecurityTests.java`.
- **Tests Removed:** 0.
- **Tests Renamed:** 0.
- **Tests Disabled (`@Disabled`):** 0 across all 9 source files.
- **Current Total:** $133 + 19 = 152\text{ tests}$ **[VERIFIED]**.

### 3.2 Inventory of Test Methods Added in Task 07E (`ApiSecurityTests.java`)
1. `test1_UnauthenticatedAccess_Returns401` — Unauthenticated GET `/api/v1/catalog` rejected with 401.
2. `test2_InvalidJwt_Returns401` — Tampered JWT cookie rejected with 401.
3. `test3_InsufficientRole_Returns403` — Employee persona forbidden from `/api/v1/approvals/pending`.
4. `test4_TicketCreation_InvalidPayload_Returns400_RFC7807` — Missing mandatory catalog ID returns 400 + RFC 7807 errors.
5. `test5_TicketStatusUpdate_MissingVersion_Returns400` — Omission of OCC version token returns 400.
6. `test6_ApprovalDecision_InvalidShortNotes_Returns400` — Decision notes exceeding 1000 characters returns 400.
7. `test7_TicketComment_BlankComment_Returns400` — Empty comment text returns 400.
8. `test8_ServiceCatalog_PublicAccess_Returns200` — Authenticated employee retrieves paginated catalog items.
9. `test9_TicketCreation_Success_Returns201` — Valid ticket creation succeeds returning HTTP 201 Created.
10. `test10_TicketIDOR_UnrelatedUserBlocked_Returns403` — Employee Charlie blocked from accessing Alice's ticket.
11. `test11_DepartmentQueue_PermittedForDepartmentResolver` — IT resolver permitted access to IT queue.
12. `test12_CrossDepartmentQueue_Blocked_Returns403` — Facilities resolver blocked from IT queue.
13. `test13_AssignedResolverCanUpdateStatus` — Assigned resolver successfully transitions ticket status.
14. `test14_InternalComments_HiddenFromRequester` — Requester views only public comments; resolver views public + internal.
15. `test15_AuditorCanVerifyChain_Returns200` — Dedicated `ROLE_AUDITOR` persona executes `/api/v1/audit/verify`.
16. `test16_RegularEmployeeCannotAccessAuditVerify_Returns403` — Regular employee forbidden from audit verify.
17. `test17_OptimisticLockingFailure_Returns409Conflict` — Stale version token triggers HTTP 409 Conflict.
18. `test18_CookieMutationWithoutCsrf_Forbidden` — State mutation via cookie without CSRF token returns 403 Forbidden.
19. `test19_BearerMutation_SucceedsWithoutCookieCsrf` — Bearer token mutation succeeds without cookie CSRF requirement.

---

## 4. Test Isolation Audit

### 4.1 Cross-Test Data Pollution Risk
In previous task stages, test suite execution order caused two primary cross-contamination defects:
1. **Foreign Key Integrity Violations:** IAM test suites (`JwtAuthenticationTests`, `RbacAuthenticationTests`) called `userAccountRepo.deleteAll()`. When ticket entities referencing those users remained in `service_ticket`, H2 threw referential integrity violations (`FK_ST_REQUESTER`, `FK_ST_ASSIGNEE`).
2. **Audit Hash Chain Sequence Gaps:** When test setups wiped `audit_log` records without resetting `audit_chain_head`, `verifyChain()` failed due to sequence gaps (e.g., expected sequence 1, found sequence 4).

### 4.2 Isolation Hardening Verification
- **`AuditLogRepo`:** Added `@Transactional` directly to derived delete methods:
  ```java
  @Transactional void deleteBySequenceNumberIsNull();
  @Transactional void deleteBySequenceNumberIsNotNull();
  ```
  This eliminates `InvalidDataAccessApiUsageException: No EntityManager with actual transaction available` when invoked outside test transactions **[VERIFIED]**.
- **`ApiSecurityTests`:**
  - `@BeforeEach` and `@AfterEach` clean up `approvalTaskRepository`, `ticketCommentRepository`, `serviceTicketRepository`, and `refreshTokenRepo`.
  - `@AfterAll` cleans up ticket repositories and resets the audit chain to genesis via `auditChainService.resetChainToGenesis()`.
- **`JwtAuthenticationTests` & `RbacAuthenticationTests`:**
  - Pre-delete guards clean up `approvalTaskRepository`, `ticketCommentRepository`, and `serviceTicketRepository` before invoking `userAccountRepo.deleteAll()`.
- **Order-Independence Verification:**
  - Executed reverse-ordered test subset: `mvn test -Dtest=TicketWorkflowTests,FlywayMigrationTests,ApiSecurityTests,JwtAuthenticationTests,RbacAuthenticationTests`.
  - Result: **69 / 69 tests passed, 0 failures, 0 errors** **[VERIFIED]**.

---

## 5. Audit-Chain Integrity Review

### 5.1 `AuditChainService.resetChainToGenesis()`
- **Implementation:**
  ```java
  @Transactional
  public void resetChainToGenesis() {
      AuditChainHead head = auditChainHeadRepository.findById(HEAD_ROW_ID).orElse(null);
      if (head != null) {
          head.setLastSequence(GENESIS_SEQUENCE); // 0L
          head.setCurrentHash(GENESIS_HASH);       // 64 zeros
          head.setUpdatedAt(LocalDateTime.now(clock));
          auditChainHeadRepository.saveAndFlush(head);
      }
      auditLogRepo.deleteBySequenceNumberIsNotNull();
  }
  ```
- **Atomicity & Consistency:** **[VERIFIED]**  
  The head row update and chained record deletion are bundled in a single `@Transactional` boundary. This guarantees that `audit_chain_head.last_sequence = 0` cannot be committed if deletion of chained logs fails.
- **Production Exposure Risk:** **[REPORT-ONLY]**  
  `resetChainToGenesis()` is NOT mapped to any controller endpoint. It is accessible only within the Java application context.  
  *Recommendation:* In a production hardening pass, annotate `resetChainToGenesis()` with `@Profile("test")` or add an environment guard to completely disallow invocation under `prod`/`production` profiles.

---

## 6. Transaction Boundary Review

1. **Ticket Creation & Mutation (`TicketCommandService`):** **[VERIFIED]**  
   Annotated `@Transactional` at service method boundaries. Ticket persistence, SLA date computation, notification persistence, and chained audit event recording execute within the same transactional context.
2. **Post-Commit Notification Events:** **[VERIFIED]**  
   Transactional event listeners (`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`) ensure that external notifications (such as `in_app_alert` dispatch) are triggered only after database commit.
3. **Optimistic Locking (`@Version`):** **[VERIFIED]**  
   The `version` field on `ServiceTicket` is incremented on every status, assignment, or lifecycle update. Concurrent modifications throw `OptimisticLockException` / `ObjectOptimisticLockingFailureException`, which `GlobalExceptionHandler` converts into an RFC 7807 response with status 409 Conflict.

---

## 7. API Security Findings

| Security Requirement | Implementation File & Method | Verification Evidence | Status |
|:---|:---|:---|:---:|
| **Authentication Enforcement** | `SecurityConfig.java: filterChain()` (`anyRequest().authenticated()`) | `ApiSecurityTests.test1_UnauthenticatedAccess_Returns401` | **VERIFIED** |
| **Invalid JWT Rejection** | `JwtFilter.java: doFilterInternal()` | `ApiSecurityTests.test2_InvalidJwt_Returns401` | **VERIFIED** |
| **Role Authority Normalization** | `CustomUserDetailsService.java` (`roleName.startsWith("ROLE_")`) | Prevents double-prefixing `"ROLE_ROLE_..."` across all roles | **VERIFIED** |
| **Ticket Requester IDOR Protection** | `TicketSecurityEvaluator.java: canViewTicket()` | `ApiSecurityTests.test10_TicketIDOR_UnrelatedUserBlocked_Returns403` | **VERIFIED** |
| **Department Queue Isolation** | `TicketSecurityEvaluator.java: canAccessDepartmentQueue()` | `ApiSecurityTests.test11` (allowed) & `test12` (blocked 403) | **VERIFIED** |
| **Specialist Resolver Roles** | `TicketSecurityEvaluator.java: isResolverRole()` | Recognizes `ROLE_IT_SUPPORT`, `ROLE_FACILITIES_CREW`, `ROLE_HR_SPECIALIST` | **VERIFIED** |
| **Internal Comment Confidentiality** | `TicketQueryService.java: getCommentsForTicket()` | `ApiSecurityTests.test14_InternalComments_HiddenFromRequester` | **VERIFIED** |
| **Anti-Self-Approval Guard** | `ApprovalService.java: decideApproval()` | Throws `AntiSelfApprovalException` if approver == requester | **VERIFIED** |
| **Server-Controlled Fields** | `TicketCommandService.java: createTicket()` | Requester ID, creation timestamp, and initial status are server-derived | **VERIFIED** |
| **Cookie CSRF Protection** | `SecurityConfig.java: CookieCsrfTokenRepository` | `ApiSecurityTests.test18_CookieMutationWithoutCsrf_Forbidden` | **VERIFIED** |
| **Bearer CSRF Exemption** | `SecurityConfig.java` + `ApiSecurityTests.test19` | Bearer token requests bypass cookie CSRF requirement | **VERIFIED** |
| **Auditor Read-Only Boundary** | `AuditApiController.java: verifyAuditChain()` | `@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")` (`ApiSecurityTests.test15`/`16`) | **VERIFIED** |

---

## 8. DTO & HTTP Contract Findings

### 8.1 Validation & Problem Details
- **Jakarta Bean Validation:** All incoming request bodies use `@Valid` with constraints (`@NotNull`, `@NotBlank`, `@Size`).
- **RFC 7807 Format:** `GlobalExceptionHandler` produces standard `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`, `timestamp`, and `errors` array.
- **Entity Exposure:** Zero JPA entities (`ServiceTicket`, `UserAccount`, `ApprovalTask`) are exposed directly from `/api/v1/*` endpoints. All controller methods return typed DTOs.

### 8.2 Endpoint Route Comparison Against Documentation
A comparison between the live code and `docs/api/task-07e-rest-api-contracts.md` revealed two discrepancies:

| Live Controller Path | Documented Path in `task-07e-rest-api-contracts.md` | Audit Observation | Status |
|:---|:---|:---|:---:|
| `GET /api/v1/departments/{department}/queue` | `GET /api/v1/queue/{department}` | The live code in `DepartmentQueueController` maps `@RequestMapping("/api/v1/departments")` + `/{department}/queue`. | **REPORT-ONLY** |
| `POST /api/v1/approvals/{taskId}/decide` | `POST /api/v1/approvals/{taskId}/decision` | The live code in `ApprovalController` uses `@PostMapping("/{taskId}/decide")`. | **REPORT-ONLY** |

*Recommendation:* The frontend React integration in Task 07F must bind to the **live controller paths** (`/api/v1/departments/{department}/queue` and `/api/v1/approvals/{taskId}/decide`). The specification document should be updated to align with the live routes.

---

## 9. Runtime & Database Limitations

1. **Java Runtime:**
   - **Configured Version:** Java 25 LTS (`<java.version>25</java.version>`).
   - **Active JVM:** OpenJDK 64-Bit Server VM (build 25.0.2+10-ea-9).
   - **Audit Note:** **[ASSUMPTION]** While Spring Boot 3.4.12 runs and passes all 152 tests on Java 25.0.2, official Spring Boot 3.4.x baseline support targets Java 17 and 21. Java 25 compatibility is operational in this environment via ByteBuddy forward compatibility.
2. **Database & Schema Validation:**
   - **Production Engine Target:** MySQL 8.0+ with InnoDB.
   - **Test Database Engine:** H2 In-Memory (v2.3.232) configured with `MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`.
   - **Flyway Migrations:** Migrations V1, V2, and V3 applied cleanly in 0.021s.
   - **Hibernate Validation:** `spring.jpa.hibernate.ddl-auto=validate` passed validation against all 14 tables.
   - **Live MySQL Integration Status:** **[NOT TESTED (BLOCKED)]**  
     Live verification on a real MySQL server remains blocked because no active MySQL daemon/network service is configured in the environment. H2 in MySQL mode verifies syntax and schema compatibility, but does not substitute for a live MySQL cluster test.

---

## 10. Prioritized Defects & Recommendations

| Priority | Category | Finding / Recommendation | Action Required Before Task 07F |
|:---:|:---|:---|:---|
| **Low** | Documentation | Align `task-07e-rest-api-contracts.md` endpoint paths with live controllers: `/api/v1/departments/{department}/queue` and `/api/v1/approvals/{taskId}/decide`. | Update contract doc to avoid confusion during React UI implementation. |
| **Low** | Security | Add `@Profile("test")` or active-profile check to `AuditChainService.resetChainToGenesis()`. | Production hardening measure (can be deferred to Phase 3 deployment). |
| **Info** | Database | Live MySQL 8.0+ verification remains `BLOCKED` until CI/CD deployment environment configuration. | Perform live MySQL validation during staging pipeline setup. |

---

## 11. Task 07F Readiness Decision

### Decision: **CONDITIONALLY APPROVED FOR TASK 07F**

### Conditions for Proceeding to Task 07F:
1. **Frontend API Path Alignment:** During Task 07F implementation, frontend Axios client services must bind strictly to the live verified controller routes:
   - Department Queue: `/api/v1/departments/{department}/queue`
   - Approval Decision: `/api/v1/approvals/{taskId}/decide`
2. **Preserve Backend State:** No modifications to the 11-state ticket lifecycle machine, Flyway V1–V3 migrations, or `ddl-auto=validate` during frontend UI integration.
3. **Full Regression Guard:** All 152 automated backend tests must continue to pass throughout Task 07F frontend bundling and E2E testing.

---

## 12. Sign-Off Block

**Lead Auditor:** Principal Security Architect & QA Automation Specialist  
**Verification Result:** 152 / 152 Tests Passing (0 Failures, 0 Errors, 0 Skipped)  
**Deliverable Status:** Audit Report Generated at `docs/implementation/task-07e1-independent-verification-report.md`  
**Timestamp:** 2026-11-15T20:56:00+05:30  

