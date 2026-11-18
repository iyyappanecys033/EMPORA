# EPAMS P7 — Known Limitations & Deferred Work

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Phase:** P7 — Complete Frontend Integration & Quality Acceptance  
**Date:** 2026-11-17  
**Status:** FORMALLY DOCUMENTED & HONESTLY CLASSIFIED  

---

## 1. Scope Boundaries & Honest Classification

In accordance with Phase P7 acceptance rules, all non-integrated components or unexecuted environments are formally documented and classified below:

| Feature / Capability | Implementation Reality | P7 Classification | Smallest Future Requirement |
| :--- | :--- | :---: | :--- |
| **In-App Notification REST Controller** | Backend contains `InAppAlert.java`, `InAppAlertRepository.java`, and event publishers. However, no `@RestController` exposes `/api/v1/notifications`. The frontend UI refrains from polling or faking mock alerts. | **DEFERRED (REST Layer Gap)** | Create `NotificationController.java` exposing `GET /api/v1/notifications/my` and `PUT /api/v1/notifications/{id}/read`. |
| **Live MySQL 8.0+ Integration** | Flyway migrations V1–V3 and Hibernate `ddl-auto=validate` are verified against in-memory H2 in MySQL mode. No live MySQL server daemon is running in the current sandbox environment. | **NOT TESTED (BLOCKED by Environment)** | Execute live migration on a provisioned MySQL 8.0 container during Task 07G CI/CD pipeline setup. |
| **Legacy Phase 1 Administrative Consoles** | Phase 1 user, role, and profile screens (`UserList`, `RoleList`, `ProfileList`) use legacy table layouts scoped under `.admin-form`. They remain functional for Admins at `/users`, `/roles`, and `/profiles`. | **DEFERRED (Visual Modernization)** | Modernize legacy IAM tables with standard common components (`Badge`, `Pagination`) during post-P7 UI polish. |
| **Browser-Based End-to-End Suite** | Manual journeys and automated React Testing Library integration tests verified the full workflow. No Cypress or Playwright browser automation framework is installed in `package.json`. | **DEFERRED (Tooling Scope)** | Add Playwright or Cypress framework in future QA automation phase. |

---

## 2. Risk Mitigation & Impact Analysis

1. **Notification Gap:**
   - *Impact:* Users rely on ticket detail views and department queues for state progression rather than real-time notification bells.
   - *Mitigation:* The frontend communicates honestly and avoids misleading mock notifications.
2. **MySQL Environment:**
   - *Impact:* MySQL-specific syntax (e.g. `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`) has been verified via Flyway scripts and H2 MySQL mode, but live driver performance under high concurrency remains to be benchmarked.
   - *Mitigation:* `application-mysql.properties` is configured with `ddl-auto=validate` and strict Flyway baselines.

