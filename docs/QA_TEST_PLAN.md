# EMPORA Application QA Test Plan

## 1. Scope and assumptions

This plan uses the current EMPORA repository as its source of truth: React web frontend, Spring Boot REST backend, and MySQL persistence. No PRD, deployment target, formal role-permission matrix, or separate field-rule specification was supplied. Therefore:

- Explicit validation annotations, HTML controls, state-machine rules, and role annotations are treated as the current implementation contract.
- Where the implementation defines no length, range, or format constraint, this plan does not invent one. Those cases are marked **contract gap** and need a product decision before release.
- Test accounts and data below are synthetic. Create role-specific fixtures in an isolated QA database; do not use production PII or credentials.
- This is a risk-based plan for the identified application modules and forms: authentication, service catalog, service tickets, approvals, profiles, user/role administration, notifications, preferences, analytics, and audit/ledger views.

## 2. Test strategy and environment

| Area | Approach |
|---|---|
| Unit/component | Validate field constraints, state transitions, and frontend control behavior with focused unit/component tests. |
| API/integration | Exercise authentication, authorization, validation, ticket lifecycle, approvals, CRUD, and persistence through REST endpoints against MySQL. |
| End-to-end | Use Playwright with the frontend and backend running against an isolated MySQL database. Cover login, ticket workflow, profile persistence, and logout. |
| Browser/device | Desktop and narrow viewport; current supported Chromium plus at least one additional supported browser. Check keyboard-only use and zoom. |
| Security | Verify access control, CSRF/cookie behavior, validation, output encoding, rate limits, CORS, and absence of secrets/PII in responses and logs. Use non-destructive test inputs only. |
| Regression | Run focused tests for changed modules, full frontend tests/build, backend tests, MySQL integration tests, and Playwright E2E before release. |

**Suggested local commands** (run from the corresponding project directory):

```text
Frontend: npm test -- --watchAll=false
Frontend production build: npm run build
Backend: mvn test
MySQL integration: run the backend integration suite with EPAMS_MYSQL_INTEGRATION enabled
Playwright: npx playwright test e2e/epams.spec.js
```

The database must be isolated and seeded with the synthetic roles and records in §6. Never run destructive cleanup against a shared or production database.

## 3. Core end-to-end journeys

| ID | Feature | Scenario | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|---|
| E2E-001 | Authentication | Employee signs in, opens a service, submits a ticket, verifies persistence, and signs out | QA employee and active catalog item exist; MySQL available | Sign in; open catalog; select item; submit valid department, supported priority, and description; open ticket list/detail; reload; sign out; revisit protected URL | Ticket is created once, associated with the employee, readable after reload, and no protected content remains after logout | P0 |
| E2E-002 | Ticket workflow | Ticket follows approval and resolution lifecycle | Employee, approver, resolver fixtures; configured item requiring approval | Submit ticket; approver approves; resolver accepts/assigns; mark in progress; place on hold with reason; resume; resolve; close | Each allowed state and actor is recorded; state/history persists; unauthorized or invalid transitions are rejected | P0 |
| E2E-003 | Ticket workflow | Ticket bypasses approval when policy permits | Employee and resolver fixtures; catalog item not requiring approval | Submit; inspect queue; assign; progress; resolve; close | No unintended approval is inserted; normal lifecycle is available to authorized actors | P1 |
| E2E-004 | Dispute | Employee reopens a recently resolved ticket | Resolved ticket with deterministic clock and resolution timestamp | Reopen at the inclusive 48-business-hour boundary; then repeat just beyond the boundary | Exact deadline is accepted; beyond-window request is rejected with a useful explanation; status/history remain consistent | P1 |
| E2E-005 | Profile / PII | Profile is created or edited and survives a reload | QA user; isolated database | Save synthetic name, email, phone, address, DOB, and gender; reload profile; inspect API/database as authorized QA | Saved values round-trip correctly; fields are not exposed to unrelated users; no data is lost or duplicated | P1 |
| E2E-006 | Administration | Admin creates a user, role, and mapping, then verifies access | Admin fixture; test user and role names unique for this run | Create user; create role; map them; authenticate as mapped user; remove/disable test data using supported UI/API | Mapping is persisted and the assigned authority takes effect; cleanup affects only this run's fixtures | P1 |
| E2E-007 | Session lifecycle | Expired access token is refreshed or rejected safely | QA user; short-lived test token configuration | Sign in; expire access token; make protected request; refresh; logout; replay old refresh token | Refresh rotates/renews as designed; logout clears cookies and invalidates refresh token; replay is rejected | P1 |
| E2E-008 | Regression | Preferences persist locally and reset cleanly | Browser profile available | Change theme, density, sidebar, motion, and text size; reload; reset to defaults; reload | Selected values persist in browser-local preferences; reset restores documented defaults without changing server-side data | P2 |

## 4. Feature, negative, boundary, and error-handling cases

| ID | Feature | Scenario | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|---|
| FUNC-001 | Login | Valid active account | Active QA account exists | Submit correct username/password | Auth succeeds; access/refresh cookies have expected HttpOnly, SameSite, path, and environment-appropriate Secure attributes | P0 |
| FUNC-002 | Login | Unknown user, wrong password, inactive account, or malformed body | QA accounts available | Try each negative login variant | Generic 401 or validation response; no account enumeration; no token issued; no secret echoed | P0 |
| FUNC-003 | Registration | Valid registration and duplicate username | Registration enabled | Register unique synthetic account; repeat username | First request succeeds; duplicate is rejected safely; no duplicate account or raw exception | P1 |
| FUNC-004 | Logout | Logout with valid, missing, and replayed refresh token | Signed-in and signed-out browser contexts | Logout; inspect cookies; replay refresh token | Logout is safe/idempotent; auth and CSRF cookies are cleared; refresh replay fails | P0 |
| FUNC-005 | CSRF | Mutating request with missing, incorrect, and valid CSRF token | Authenticated session and CSRF endpoint available | Send POST/PUT/PATCH/DELETE without token, with wrong token, then correct token | Unsafe requests are rejected unless explicitly exempted by the contract; valid token succeeds; GET remains non-mutating | P0 |
| FUNC-006 | Service catalog | Empty, unavailable, and valid catalog | Test catalog data seeded | Load catalog in each condition; select an item | Empty/error states are understandable; unavailable data cannot produce a malformed ticket; valid selection carries the correct item ID | P1 |
| FUNC-007 | Ticket creation | Required fields missing or blank | Authenticated ticket-capable role | Omit catalog ID/priority; submit blank department/description | API rejects invalid request with field-specific validation; no ticket is persisted | P0 |
| FUNC-008 | Ticket creation | Description at lengths 9, 10, 2000, and 2001 | Valid catalog item and supported priority | Submit each description length | 10 and 2000 are accepted; 9 and 2001 are rejected; boundary behavior matches API and UI | P0 |
| FUNC-009 | Ticket creation | Department at lengths 1, 50, and 51; whitespace-only | Valid remaining fields | Submit each value | Nonblank 1 and 50-character values are accepted; whitespace-only and 51 characters are rejected | P1 |
| FUNC-010 | Ticket state machine | Every valid and invalid transition | Ticket fixtures in each defined state | Attempt each edge in §5 and representative non-edges | Valid edges succeed; invalid edges fail without partial updates; terminal states remain terminal | P0 |
| FUNC-011 | Ticket state machine | Reason-required transitions | Tickets eligible for hold, reject, cancel, or reopen | Try null, blank, 14-char, 15-char, and padded reason | Required transitions accept trimmed length ≥15 only; error is clear; rejected attempt does not change state | P1 |
| FUNC-012 | Approval | Approve/reject and secondary approval paths | Roles and tickets configured for each path | Approve, reject with/without reason as applicable, repeat action, use wrong actor | Only authorized transition succeeds; duplicate/stale actions do not overwrite state; audit history identifies actor and outcome | P1 |
| FUNC-013 | Ticket access | Cross-user and cross-organization IDOR attempt | Two users in separate ownership/org scopes | Read, update, transition, and enumerate another user's ticket ID | No unauthorized data or mutation; response follows API's 403/404 policy; no sensitive data in errors | P0 |
| FUNC-014 | User administration | Create/edit user, password required only on create | Admin fixture | Create with valid values; omit password; edit without password; change status | Create enforces password; edit follows intended password semantics; status is limited to available options | P1 |
| FUNC-015 | Profile administration | Create/edit profile and linked user | Admin fixture and users exist | Submit valid profile; omit linked user/name/email; edit existing profile | Required controls block missing values; edit round-trips data; duplicate/invalid links fail without corrupting either record | P1 |
| FUNC-016 | Roles and mappings | Create role and assign mapping; invalid and duplicate mapping | Admin fixture; users/roles seeded | Create valid role/mapping; omit each selection; submit unknown IDs and duplicate mapping | Valid relation persists; invalid IDs do not create orphan records; duplicate handling is deterministic and surfaced | P1 |
| FUNC-017 | Notifications | Empty list, unread/read, and stale notification | Notifications seeded for current user | Load, mark read, reload, attempt another user's notification | Read state persists; empty/error states render; cross-user access is denied | P2 |
| FUNC-018 | Search, filters, pagination | Empty results, boundary page, sort/filter combination | Enough synthetic records for multiple pages | Search, filter, sort, go to first/last/out-of-range page, clear filters | Results and counts are consistent; no record leaks across users; invalid page inputs are handled safely | P2 |
| FUNC-019 | Analytics / metrics | Authorized read and unauthorized read | Admin and non-admin fixtures | Open pages and call backing endpoints as both roles | Authorized users receive scoped data; unauthorized calls fail; no PII or secrets appear in aggregates | P1 |
| FUNC-020 | Audit / cryptographic ledger | Read-only integrity and unauthorized mutation | Auditor/admin fixtures; audit records seeded | Read chain/history; attempt mutation with auditor and employee | Reads obey policy; audit evidence is stable; mutation is rejected for read-only roles | P1 |
| FUNC-021 | Error handling | Backend unavailable, timeout, malformed JSON, and 5xx | Test proxy/failure injection available | Trigger each error on form and list screens | No false success; entered data is preserved where safe; actionable user feedback appears; no stack trace or secrets shown | P1 |
| FUNC-022 | Concurrency | Two actors act on the same pending ticket | Two authorized sessions | Approve/assign/transition nearly simultaneously | One consistent final state; stale/conflicting request is rejected or reconciled; history has no duplicate/contradictory entries | P1 |
| FUNC-023 | Database reliability | Restart app after ticket creation | MySQL integration environment | Create a ticket; restart backend; create another; inspect records | Existing records survive; generated ticket identifiers do not collide after restart | P1 |
| FUNC-024 | Preferences | Each supported preference and reset | Browser-local storage enabled | Select every option; reload; reset; inspect local storage | Valid option is persisted; unsupported/corrupt stored value falls back safely; reset clears/restores defaults | P2 |
| FUNC-025 | Responsive UI | Main workflows at desktop, tablet, and narrow mobile widths | Browser viewport control | Navigate, complete login and ticket form, inspect tables, dialogs, and navigation at 1440/768/390 CSS px | No clipped controls or horizontal page overflow; reading and focus order remain usable; tables have an intentional narrow-screen behavior | P1 |
| FUNC-026 | Accessibility | Keyboard, screen reader semantics, zoom, and reduced motion | Browser accessibility tooling | Tab through navigation/forms/dialogs; use radio/select controls; zoom to 200%; enable reduced motion | Visible focus; labels and errors announced; no keyboard trap; contrast and zoom remain usable; motion preference is respected | P1 |
| FUNC-027 | Input/output security | Stored and reflected markup-like input | QA tenant and disposable records | Enter `<script>alert(1)</script>`, `"><img src=x>`, `& < > " '`, and SQL-looking text in permitted text fields; view list/detail/export | Data is rendered as text, not executable markup; no query manipulation; value is retained or safely rejected; logs do not expose credentials/tokens | P0 |
| FUNC-028 | Rate limiting | Repeated authentication attempts | Rate limit configured for QA | Perform controlled repeated login attempts, then wait for reset | Rate limit responds consistently (including Retry-After if configured); valid users are not permanently locked out; no sensitive response details | P1 |
| FUNC-029 | CORS / headers | Trusted and untrusted origins | Backend running with QA origin configuration | Preflight and request from allowed and unlisted origins; inspect security headers | Only configured origins receive credentialed CORS; CSP, HSTS (TLS), frame denial, nosniff, and referrer policy match environment policy | P1 |
| FUNC-030 | Production startup | Required secrets absent or invalid | Isolated production/mysql profile | Start with encryption key/JWT secret absent, malformed, or too short; then with valid secrets | Invalid configuration fails closed before serving requests; valid configuration starts; no secret values are printed | P0 |

## 5. Ticket states and role/permission coverage

### State transition coverage

The implementation defines these transitions; test all listed edges and at least one invalid edge from every source state.

| Current state | Allowed next states |
|---|---|
| DRAFT | SUBMITTED, CANCELLED |
| SUBMITTED | PENDING_APPROVAL, ASSIGNED, CANCELLED |
| PENDING_APPROVAL | PENDING_SECONDARY_APPROVAL, ASSIGNED, REJECTED, CANCELLED |
| PENDING_SECONDARY_APPROVAL | ASSIGNED, REJECTED, CANCELLED |
| ASSIGNED | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | ON_HOLD, RESOLVED |
| ON_HOLD | IN_PROGRESS, CANCELLED |
| RESOLVED | CLOSED, IN_PROGRESS (within inclusive 48 business-hour reopen window) |
| CLOSED, REJECTED, CANCELLED | No next state |

Reason is required for ON_HOLD, REJECTED, CANCELLED, and reopening RESOLVED to IN_PROGRESS. IN_PROGRESS cannot be unilaterally cancelled. Verify reason trim behavior at 14/15 characters and exact business-calendar deadline with a fixed clock.

### RBAC matrix

Known application authorities include `EMPLOYEE`, `MANAGER`, `DEPT_HEAD`, `RESOLVER`, `IT_SUPPORT`, `HR_SPECIALIST`, `FACILITIES_CREW`, `ADMIN`, and `AUDITOR`. The ticket-submit endpoint explicitly permits the first eight listed operational/admin roles and excludes `AUDITOR`; authenticated access is required elsewhere unless an endpoint is explicitly public.

| ID | Feature | Scenario | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|---|
| RBAC-001 | Authentication | Anonymous access to protected API/UI | Signed-out browser | Request protected routes and API resources | API returns 401; UI redirects or presents login; no protected payload is rendered | P0 |
| RBAC-002 | Ticket creation | Submit as each permitted ticket-submit role | One fixture for each permitted role | POST a valid ticket as EMPLOYEE, MANAGER, DEPT_HEAD, RESOLVER, IT_SUPPORT, HR_SPECIALIST, FACILITIES_CREW, and ADMIN | Each role is authorized by the explicit endpoint rule; resulting ticket is attributed to the caller | P1 |
| RBAC-003 | Ticket creation | Auditor attempts to submit | AUDITOR fixture | POST a valid ticket | Request is denied; no ticket is persisted | P0 |
| RBAC-004 | Ticket read | Own, team, and cross-organization scope | Employee, manager, resolver, auditor fixtures in separate scopes | Read own, in-scope, and out-of-scope ticket IDs | Each endpoint enforces the documented scope; cross-scope data is never exposed | P0 |
| RBAC-005 | Ticket mutation | Read-only and wrong-role mutation | AUDITOR and non-owning user fixtures | Attempt update, approval, assignment, transition, and deletion where exposed | Only endpoint-authorized roles can mutate; auditor is denied mutation; no state change on denial | P0 |
| RBAC-006 | Approvals | Correct approver, wrong approver, and self-approval | Manager/department-head fixtures and approval-required ticket | Approve/reject as intended approver, unrelated role, and requester | Only configured approver(s) succeed; self-approval policy is enforced; secondary approval follows configured policy | P1 |
| RBAC-007 | Admin resources | User/role/profile/mapping CRUD | Admin and non-admin fixtures | Invoke each read/write endpoint as both roles | Admin policy is enforced consistently at API and UI; hiding a button alone is not authorization | P1 |
| RBAC-008 | Notifications / profile | Access another user's private records | Two users | Query by another user's notification/profile/ticket ID | Denied with no PII leakage | P0 |
| RBAC-009 | Token/session | Tampered, expired, missing, and replayed credentials | Valid and invalid test tokens | Call protected endpoints and refresh/logout routes | Invalid credentials are rejected; no privilege escalation by changing client claims or IDs | P0 |

**Release blocker:** confirm the intended role-to-endpoint matrix for approvals, ticket reads/transitions, analytics, audit/ledger, user/profile administration, and notification access. Do not treat frontend visibility as the permission contract.

## 6. Synthetic test data

| Fixture | Example data | Purpose |
|---|---|---|
| `qa-employee` | Username `qa.employee.01`; email `qa.employee.01@example.test`; department `Facilities`; role `EMPLOYEE` | Submitter and private-data ownership |
| `qa-manager` | Username `qa.manager.01`; email `qa.manager.01@example.test`; role `MANAGER` | First-stage approval |
| `qa-dept-head` | Username `qa.head.01`; email `qa.head.01@example.test`; role `DEPT_HEAD` | Secondary approval |
| `qa-resolver` | Username `qa.resolver.01`; email `qa.resolver.01@example.test`; role `RESOLVER` | Assignment and resolution |
| `qa-auditor` | Username `qa.auditor.01`; email `qa.auditor.01@example.test`; role `AUDITOR` | Read-only and denial checks |
| `qa-admin` | Username `qa.admin.01`; email `qa.admin.01@example.test`; role `ADMIN` | Administrative CRUD |
| Catalog item | `QA - Laptop support` with deterministic approval configuration | Ticket creation and approval paths |
| Boundary description | Repeat ASCII `D` to exact length 10, 2000, 9, and 2001 | DTO boundary validation |
| Safe markup marker | `<script>alert(1)</script>` and `"><img src=x>` | Verify encoding/rejection; never execute |
| Unicode marker | `Zoë 東京 😀`, `مرحبا`, and combining `e\u0301` | Unicode, emoji, RTL, and normalization checks |

Use unique suffixes per run. Avoid real employee names, addresses, phone numbers, or birth dates.

## 7. Field-by-field input test inventory

### 7.1 Authentication forms

#### Login: `username` (string, required; nonblank; max 100)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-LOGIN-USER-01 | Existing valid `qa.employee.01` | Submit with correct password | Login succeeds | P0 |
| FL-LOGIN-USER-02 | Length 1 and exactly 100 characters | Submit with matching fixture account | Boundary lengths are accepted by validation; authentication result depends on account existence | P1 |
| FL-LOGIN-USER-03 | Missing, `""`, `"   "`, and length 101 | Submit each value | Validation rejects before authentication; no account enumeration | P0 |
| FL-LOGIN-USER-04 | `" qa.employee.01 "`; `Zoë東京😀`; `مرحبا`; `"><img src=x>`; `qa' OR '1'='1` | Type and paste; submit; inspect response/log/UI | No injection or markup execution; trimming/case semantics must match product contract; errors are visible and do not echo unsafe markup | P1 |

#### Login: `password` (string, required; nonblank; max 128)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-LOGIN-PASS-01 | Valid fixture password `Qa!Passw0rd-OnlyForTest` | Submit as QA account | Login succeeds; password is never returned or logged | P0 |
| FL-LOGIN-PASS-02 | Exactly 1 and 128 characters with corresponding test account | Submit | DTO boundary accepts values; authentication follows account hash | P1 |
| FL-LOGIN-PASS-03 | Missing, `""`, `"   "`, and 129 characters | Submit each | Validation rejects; no token is issued | P0 |
| FL-LOGIN-PASS-04 | Pasted leading/trailing spaces; Unicode/emoji; `<script>alert(1)</script>`; SQL-looking string | Paste and submit | Password handling does not silently trim or expose value; no injection; failure response is generic | P1 |

#### Registration: `username` (string, required; 3–50 characters)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-REG-USER-01 | `qauser` | Register unique user | Account is created once | P1 |
| FL-REG-USER-02 | Exactly 3 and 50 characters | Register distinct values | Both boundaries accepted | P1 |
| FL-REG-USER-03 | `"ab"`, 51 chars, missing, `""`, `"   "` | Submit each | Rejected with field error; no record created | P1 |
| FL-REG-USER-04 | `Zoë東京😀`, `مرحبا`, `a&b`, `qa' OR '1'='1`, `<script>alert(1)</script>`, padded value | Type/paste, submit, then view account | Safe encoding and storage; normalization/allowed characters are a **contract gap** beyond the length/nonblank rules | P1 |

#### Registration: `password` (string, required; 4–100 characters)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-REG-PASS-01 | `Qa!2` and a 100-character test password | Register with each | Both boundaries accepted; stored as a password hash | P1 |
| FL-REG-PASS-02 | 3 and 101 chars, missing, empty, whitespace-only | Submit | Rejected; no account created | P1 |
| FL-REG-PASS-03 | Unicode `Pässwörd😀`, pasted whitespace, markup-looking text | Submit then authenticate | Behavior is consistent between registration and login; no trimming or secret logging; password policy beyond length is not defined | P1 |

#### Registration: `email` (optional string; `@Email`; max 100)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-REG-EMAIL-01 | `qa+01@example.test`; exactly 100 chars with valid email syntax | Register | Accepted if valid under Bean Validation email semantics | P1 |
| FL-REG-EMAIL-02 | 101 chars; `not-an-email`; `x@@example`; whitespace-only | Submit | Overlength and malformed email rejected; blank/null optional semantics verified and documented | P1 |
| FL-REG-EMAIL-03 | Unicode/IDN address, `qa&1@example.test`, markup-looking value | Paste and submit | No injection; supported internationalized email behavior is a **contract gap** | P2 |

#### Registration: `phone` (optional string; max 30; no format pattern)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-REG-PHONE-01 | `+1 202 555 0101`; exactly 30 chars | Register | Accepted by length validation | P2 |
| FL-REG-PHONE-02 | 31 chars; empty; whitespace | Submit | 31 rejected; optional blank/null semantics verified | P2 |
| FL-REG-PHONE-03 | `+`, `() -`, Unicode digits, `😀`, RTL marks, `<script>alert(1)</script>` | Paste and submit | No script execution; format acceptance is currently unspecified (no regex) and must be decided | P2 |

### 7.2 Service ticket form / API

#### `catalogItemId` (Long, required, non-null)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-TICKET-CAT-01 | ID of an existing enabled QA catalog item | Select item and submit | Ticket references the selected catalog item | P0 |
| FL-TICKET-CAT-02 | Missing/null, `0`, `-1`, very large integer, string `"abc"` | Submit API request | Null/type errors rejected; unknown/non-positive IDs fail safely without orphan ticket; positivity and maximum numeric range are **contract gaps** unless enforced by service | P1 |
| FL-TICKET-CAT-03 | Paste/modify another tenant's valid catalog ID | Submit | Only permitted catalog items can be referenced; no cross-tenant information leak | P1 |

#### `department` (string, required; nonblank; max 50)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-TICKET-DEPT-01 | `Facilities`; exactly 1 and 50 characters | Submit | Accepted if nonblank and within 50 | P1 |
| FL-TICKET-DEPT-02 | Missing, `""`, `"   "`, 51 chars | Submit each | Rejected; no ticket persisted | P1 |
| FL-TICKET-DEPT-03 | `R&D / West`, `東京`, `مرحبا`, `😀`, markup-looking and SQL-looking values; padded text | Type/paste and submit | Safe storage/rendering; department normalization/allowlist is unspecified and should be agreed | P1 |

#### `priority` (TicketPriority enum, required, non-null)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-TICKET-PRI-01 | Each value offered by the live UI/API enum | Select and submit one at a time | Supported enum values accepted and displayed consistently | P1 |
| FL-TICKET-PRI-02 | Missing/null, unknown string `URGENT_TEST`, wrong JSON type | Submit API request | Rejected with validation/enum error; no fallback to a more privileged priority | P1 |
| FL-TICKET-PRI-03 | Keyboard selection, paste into any exposed control, rapid double submit | Operate form | Only supported option can be committed; one ticket is created | P2 |

#### `description` (string, required; nonblank; 10–2000 characters)

| ID | Scenario and concrete value | Steps | Expected Result | Priority |
|---|---|---|---|---|
| FL-TICKET-DESC-01 | Ordinary description length 10; exact 2000 | Enter and submit | Both accepted | P0 |
| FL-TICKET-DESC-02 | Length 9, length 2001, missing, empty, whitespace-only | Submit each | Rejected at UI/API; no ticket persisted | P0 |
| FL-TICKET-DESC-03 | Paste multiline text, tabs, leading/trailing spaces, `Zoë 東京 😀`, `مرحبا`, combining characters | Paste and submit | No unintended truncation; safe text round-trip; define whether trimming is applied | P1 |
| FL-TICKET-DESC-04 | `<script>alert(1)</script>`, `"><img src=x>`, `'; DROP TABLE qa;--` | Submit, then view ticket list/detail/export | Treated as inert text or rejected; no script, markup, or SQL effect | P0 |
| FL-TICKET-DESC-05 | Paste exactly 10/2000 and 9/2001 characters; tab through controls | Paste; check character counter/error; navigate with keyboard | UI boundary matches DTO; focus order is logical; error is associated with field | P1 |

### 7.3 User administration form

Constraints shown in the frontend: `username` required; `passwordHash` required only on create; `email` required and `type=email`; `phone` optional; `status` offers `active`/`inactive`. No max lengths or backend constraints were surfaced for these legacy form fields.

| Field | Valid / boundary data | Invalid / special data | Expected checks |
|---|---|---|---|
| `username` | `qa.admin.user`; 1 char; long value within transport limits | empty, whitespace, 1000 chars, `Zoë東京😀`, `مرحبا`, `"><img src=x>`, SQL-looking text, padded paste | Required check; no assumed min/max; server validation and visible error are a **contract gap** |
| `passwordHash` | New account: use synthetic test password; edit: omit/leave blank | Create blank; >128; Unicode; paste spaces; markup-looking text | Create requires password; edit omission must not erase/change stored credential; never expose or log credential |
| `email` | `qa.user@example.test`; `qa+test@example.test` | blank, malformed `a@`, >100, Unicode/IDN, markup-looking text | Required and browser email validation; backend format/length contract must be confirmed |
| `phone` | `+1 202 555 0101`; empty | 31+ chars, letters, Unicode digits, emoji, markup | Optional; no regex/max in this form; safe handling and desired format are open |
| `status` | `active`, `inactive` | Missing/unknown value via API; keyboard navigation | Only listed options accepted; status changes take effect according to account policy |

For each text field above, repeat with direct typing and paste, verify Ctrl+A replacement, tab order, focus indication, validation text, retained input after server errors, and no horizontal overflow at narrow widths. Test `<`, `>`, `&`, `"`, `'`, emoji, RTL, and combining marks as data—not executable payloads.

### 7.4 Profile administration form

Frontend controls: required linked user, first name, last name, email; optional phone, DOB, address; gender options `female`, `male`, `other`. Except browser `type=email`/`type=date`, no field length or pattern rules were surfaced.

| Field | Valid / boundary data | Invalid / special data | Expected checks |
|---|---|---|---|
| `userId` / Linked User | Existing QA user ID | empty, stale ID, nonexistent ID, another tenant's ID | Required selection; server verifies existence and scope; stale options handled |
| `firstName` | `Zoë`; `A`; 250 chars | empty, whitespace, 251+, `東京😀`, `مرحبا`, `<script>alert(1)</script>`, pasted padded text | Required; no assumed length; safe round-trip; confirm trimming/Unicode policy |
| `lastName` | `O'Neil`; `A`; 250 chars | empty, whitespace, 251+, `Łukasz`, RTL, markup-looking text | Same checks as first name; apostrophe must not break persistence/rendering |
| `email` | `qa.profile@example.test` | empty, malformed, very long, Unicode/IDN | Required; native email validation and server-side validation agree |
| `phone` | `+1 202 555 0101`; empty | overlong, letters, Unicode digits, emoji, markup | Optional; no pattern is defined in form; safe storage/rendering |
| `dob` | Valid date; leap day `2000-02-29`; empty | Invalid date string/API value; future date; extreme past date | Optional date control; reject malformed input; allowed age/date range is a **business-rule gap** |
| `gender` | `female`, `male`, `other` | Missing/unknown API enum | UI supports only listed values; server accepts/rejects consistently; confirm whether options are complete |
| `address` | `12 Test Street`; multiline; Unicode/RTL | Very long text, `<script>alert(1)</script>`, SQL-looking text, pasted line breaks | Optional; no max is specified; safe text rendering; define practical max and line-break handling |

### 7.5 Role, mapping, preferences, and action controls

| Form/control | Valid values | Invalid/boundary values | Expected checks |
|---|---|---|---|
| Role `roleName` | `QA Resolver`; Unicode and punctuation if supported | empty; whitespace; 1000 chars; `<script>alert(1)</script>`; duplicate role | Required; no max/pattern surfaced; uniqueness and allowed characters are contract gaps; no unsafe rendering |
| Role `description` | Empty; short text; multiline/Unicode | Very long value; markup-looking or SQL-looking text | Optional; no max surfaced; safe rendering and persistence; define bound |
| Mapping `userId` | Existing user selected from list | Empty; deleted/stale ID; unauthorized user | Required; server checks existence and caller authority; visible feedback on failure |
| Mapping `roleId` | Existing role selected from list | Empty; deleted/stale ID; unauthorized role | Required; server checks existence and caller authority; privilege escalation denied |
| Mapping pair | One valid user-role pair | Duplicate pair; invalid IDs; rapid double submit | Duplicate policy must be explicit; no duplicate/orphan mappings or silent failure |
| Preferences: theme | `light`, `dark`, `system` | Corrupt/unknown localStorage enum | Only supported values; fallback to safe default |
| Preferences: density | `comfortable`, `compact` | Corrupt/unknown stored value | Supported choice persists after reload |
| Preferences: sidebar | `true`, `false` | Malformed stored value | Boolean preference restores safely |
| Preferences: motion | `system`, `on`, `off` | Corrupt enum | Reduced-motion selection is respected |
| Preferences: font scale | `standard`, `large` | Corrupt enum | Text-size selection persists and remains usable at zoom |
| Approval/reason controls | Valid action; reason length 15+ when backend rule applies | Missing, blank, 14 chars; stale action; double click | Correct actor/state required; reason validation and status update are atomic |

No string length tests can assert a max/min for fields lacking constraints. Before release, product/API owners must decide maximum lengths, normalization, allowed characters, email/phone formats, DOB range, role uniqueness, duplicate mapping behavior, and field-level error UX.

## 8. Cross-cutting UI and security checks

1. **Validation parity:** repeat client-side-invalid inputs directly against the API; server-side validation remains authoritative.
2. **Whitespace/paste:** test empty, whitespace-only, padded text, pasted boundary-length values, multiline paste, and replacement by selecting all.
3. **Focus/error UX:** labels are programmatically associated; invalid fields receive visible and announced errors; focus moves predictably; values remain after recoverable failure.
4. **Unicode/RTL:** verify persistence, sorting, search, clipping, bidi isolation, and database round-trip for `Zoë 東京 😀`, `مرحبا`, and combining marks.
5. **Injection safety:** use benign, non-executing markup/SQL-looking strings. Verify output encoding in every view, error page, notification, and export.
6. **PII:** inspect network responses, browser storage, logs, analytics, and audit views; only the owning/authorized user can access profile data.
7. **Session security:** check access and refresh cookie attributes; tokens are not placed in localStorage; logout clears cookies and invalidates refresh tokens.
8. **Availability/errors:** simulate backend and DB failures; do not show success on failed saves; retry must not duplicate records.
9. **Accessibility:** keyboard-only operation, visible focus, semantic headings/regions, accessible names, contrast, screen-reader announcements, 200% zoom, and reduced motion.
10. **Responsive:** test 1440, 1024, 768, and 390 CSS-pixel widths; no clipped form actions, overlapping navigation, or inaccessible table content.

## 9. Entry, exit, and release checklist

### Entry

- QA build and isolated MySQL database are available.
- Synthetic accounts exist for employee, manager, department head, resolver, auditor, and admin.
- Catalog items and ticket approval policies are deterministic.
- QA environment secrets are valid and are not checked into source control.
- Product owner has reviewed the unresolved contract gaps below.

### Exit / Go-No-Go

- [ ] All P0 cases pass; no open Blocker/High defects.
- [ ] Full ticket lifecycle and persistence pass against MySQL, including restart behavior.
- [ ] Unauthorized, cross-user, and auditor mutation attempts are denied at the API.
- [ ] Login, refresh, logout, CSRF, and cookie behavior pass.
- [ ] Field boundary tests match client and server behavior; undocumented bounds are either specified or accepted as explicit risk.
- [ ] Frontend tests and production build pass; backend unit/integration suites pass; Playwright E2E passes.
- [ ] No credential, token, or PII leakage in responses, browser storage, logs, or analytics.
- [ ] Responsive and keyboard/accessibility checks pass on the supported browser set.
- [ ] Deployment secrets, trusted MySQL TLS identity, health check, monitoring, backup, and rollback procedure are confirmed for the actual target environment.

**Current recommendation:** No-Go until the role-permission contract and unspecified field constraints are confirmed, then execute this plan against the release candidate. A local pass alone does not establish production readiness.

## 10. Source anchors

The plan's implementation-derived contracts were identified in:

- `springapp/backend/src/main/java/com/examly/springapp/dto/LoginRequest.java`
- `springapp/backend/src/main/java/com/examly/springapp/dto/RegisterRequest.java`
- `springapp/backend/src/main/java/com/examly/springapp/dto/CreateTicketRequestDTO.java`
- `springapp/backend/src/main/java/com/examly/springapp/workflow/TicketStateMachine.java`
- `springapp/backend/src/main/java/com/examly/springapp/security/AuthController.java`
- `springapp/backend/src/main/java/com/examly/springapp/security/SecurityConfig.java`
- `springapp/backend/src/main/java/com/examly/springapp/controller/TicketController.java`
- `springapp/frontend/src/components/UserForm.js`
- `springapp/frontend/src/components/ProfileForm.js`
- `springapp/frontend/src/components/RoleForm.js`
- `springapp/frontend/src/components/UserRoleMappingForm.js`
- `springapp/frontend/src/components/profile/PreferencesPage.js`
- `springapp/frontend/e2e/epams.spec.js`

## 11. Automated coverage added / linked

| Plan area | Automated coverage |
|---|---|
| Auth and ticket DTO exact boundaries | `P96ApiAndFieldValidationTests.testExactAuthenticationAndTicketFieldBoundaries` covers min/max and max+1 validation for login, registration, and ticket fields. |
| Legacy user/profile/role/mapping forms | `P96LegacyAdminForms.test.js` verifies required/optional controls, create-versus-edit password behavior, enum choices, Unicode/multiline payloads, and incomplete mapping rejection. |
| Preferences UI | `P961CriticalFrontendCoverage.test.js` verifies all preference controls, accessible radio groups, local persistence, and reset behavior. |
| Ticket lifecycle and business-hour boundary | `TicketWorkflowTests`, `BusinessCalendarReopenWindowTests`, and `P96FrontendComprehensive.test.js` cover state transitions, reasons, approval paths, and reopen deadlines. |
| Access control and data privacy | `P96RbacFieldSecurityIdorTests` covers cross-user IDOR, internal comments, PII redaction, mass assignment, auditor read-only policy, department scoping, and security headers. |
| Full-stack browser journeys | `springapp/frontend/e2e/epams.spec.js` covers login/logout, catalog ticket creation, approval/resolver screens, notifications, analytics, audit, access boundaries, and encrypted profile persistence. |

Responsive behavior across real browser widths, assistive-technology announcements, and fields without explicit business constraints remain manual/product-contract checks; they are not represented as fully automated by this change.
