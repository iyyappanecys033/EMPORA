# EPAMS — P9.6 IDOR / BOLA & Cross-Scope Security Test Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage G: Broken Object Level Authorization (OWASP API1:2023)

## Summary of Executed IDOR/BOLA Scenarios (`100% Blocked`)

| Test Case | Attacker Persona | Target Victim Resource | HTTP Request Attempted | Expected | Actual | Result |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: |
| `IDOR-01` | `sec_emp_a` (`EMPLOYEE`) | `sec_emp_b`'s ticket | `GET /api/v1/tickets/{empB_ticketId}` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-02` | `sec_emp_a` (`EMPLOYEE`) | `sec_emp_b`'s comments | `GET /api/v1/tickets/{empB_ticketId}/comments` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-03` | `sec_emp_a` (`EMPLOYEE`) | `sec_emp_b`'s ticket mutation | `POST /api/v1/tickets/{empB_ticketId}/cancel` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-04` | `sec_emp_a` (`EMPLOYEE`) | `sec_emp_b`'s in-app alert | `PUT /api/v1/notifications/{empB_alertId}/read` | `403` / `404` | Denied (`404`/`403`) | **PASS** |
| `IDOR-05` | `sec_emp_a` (`EMPLOYEE`) | `sec_emp_b`'s HR profile details | `GET /api/profiles/{empB_profileId}/details` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-06` | `sec_emp_a` (`EMPLOYEE`) | `sec_emp_b`'s risk score | `GET /api/users/{empB_id}/risk` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-07` | `sec_it_res` (`IT_SUPPORT`) | Cross-department HR queue | `GET /api/v1/departments/HR/queue` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-08` | `sec_emp_a` (`EMPLOYEE`) | Colleague salary order inference | `GET /api/profiles/directory?sortBy=salary` | `400` (`QA-DEF-002`) | `400 Bad Request` | **PASS** |
| `IDOR-09` | `sec_emp_b` (`EMPLOYEE`) | Internal resolver note on own ticket | `GET /api/v1/tickets/{own_id}/comments` | Redacted | Internal note omitted | **PASS** |
| `IDOR-10` | `sec_auditor` (`AUDITOR`) | Admin profile/user/role mutations | `POST /api/users`, `POST /api/roles`, `DELETE /api/profiles/{id}` | `403` | `403 Forbidden` | **PASS** |
| `IDOR-11` | `sec_emp_a` (`EMPLOYEE`) | UI Direct URL navigation to `/analytics`, `/audit-chain`, `/approvals`, `/queue` | Live Chrome `Page.navigate` | `403` Guard | `403 — Unauthorized Scope` | **PASS** |

*Conclusion:* Every tested horizontal (user-to-user), cross-department, and vertical (role escalation) object-access boundary is enforced server-side independently of UI navigation visibility.
