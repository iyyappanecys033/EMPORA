# EPAMS — P9.6 Field-Level Access Control Matrix

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage G: Entity & DTO Field-Level Security Verification

| Entity / Resource | Sensitive Field | Non-Privileged Read | Privileged Read (`ADMIN`/`HR_MANAGER`) | Client Write / Mass-Assignment Protection | Verified Evidence |
| :--- | :--- | :---: | :---: | :--- | :--- |
| `UserAccount` | `passwordHash` | **NEVER** (`@JsonProperty(WRITE_ONLY)`) | **NEVER** | BCrypt-hashed server-side; never returned in JSON (`not(containsString("$2a$"))`) | `P96RbacFieldSecurityIdorTests` (`P96-SEC-03`) |
| `UserAccount` | `status`, `failedLoginAttempts`, `lockedUntil` | Redacted in directory; own status via `/me` | Full (`ADMIN` / `HR_MANAGER`) | Lifecycle transitions restricted to `ADMIN` endpoints (`/suspend`, `/disable`, `/unlock`) | `AccountLifecycleTests`, `P96RbacFieldSecurityIdorTests` |
| `Profile` (Directory) | `salary`, `dob`, `address`, `emergencyContact` | **Redacted (`null`)** + `sortBy=salary` blocked (`400`) | Full HR/Admin visibility | Writable only via `ADMIN` / `HR_MANAGER` (`POST/PUT /api/profiles/employee`) | `P96ApiAndFieldValidationTests` (`P96-VAL-03`), `P96RbacFieldSecurityIdorTests` (`P96-SEC-02`) |
| `Profile` (`/api/profiles/me`) | Own profile record | Self-only (`Principal`) | Self-only | Read-only for employees (`EmployeeProfile.js` discloses HR request flow) | `P95UiUxTransformation` (`P95-05`) |
| `ServiceTicket` | `status`, `requesterId`, `assigneeId`, `version`, `slaTargetDate` | Authorized ticket viewers only | Full | Server-controlled on `POST /api/v1/tickets`; injected JSON fields ignored (`CreateTicketRequestDTO`) | `P96RbacFieldSecurityIdorTests` (`P96-SEC-02`) |
| `TicketComment` | `isInternal` notes | **Filtered out** for requesters (`EMPLOYEE`) | Visible to assigned dept resolvers & `ADMIN` | Requester cannot post `isInternal=true` | `TicketWorkflowTests`, `P96RbacFieldSecurityIdorTests` (`P96-SEC-02`, `P96-SEC-04`) |
| `AuditLog` / `AuditChainHead` | `sequenceNumber`, `previousHash`, `currentHash` | **DENY (`403`)** | Read-only (`ADMIN`, `AUDITOR`) | Strictly server-computed; `resetChainToGenesis()` throws `UnsupportedOperationException` in production | `P96UnitBusinessLogicTests` (`P96-UNIT-05`), `Stage2RealMysqlIntegrationTests` |
| `RegisterRequest` | `role` (extra JSON field) | N/A | N/A | Ignored by DTO; self-registered users receive zero privileged roles | `RbacAuthenticationTests` (`test14`) |
