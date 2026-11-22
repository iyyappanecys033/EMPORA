# EMPORA
### Employee Services & Workflow Platform
> **One platform. Connected operations.**

A platform for managing employee services, approvals, cross-department workflows and organisational operations.

---

## 🏛️ Overview

**EMPORA** is a production-grade enterprise employee service portal and cross-departmental operations orchestration system. Built for speed, governance, security, and developer clarity, EMPORA unifies:

- 📋 **Standardized Service Catalog:** Self-service offerings across IT, HR, Facilities, and Operations with transparent SLA commitments and automatic department routing.
- 🔄 **11-State Finite State Machine (FSM):** Complete ticket lifecycle governance (`DRAFT`, `SUBMITTED`, `PENDING_APPROVAL`, `PENDING_SECONDARY_APPROVAL`, `ASSIGNED`, `IN_PROGRESS`, `ON_HOLD`, `RESOLVED`, `CLOSED`, `REJECTED`, `CANCELLED`) with business calendar SLA computation and dispute window mechanics.
- 🛡️ **Role-Based Access Control (RBAC):** Strict 11-role enterprise matrix (`EMPLOYEE`, `MANAGER`, `HR_MANAGER`, `IT_SUPPORT`, `HR_SPECIALIST`, `FACILITIES_CREW`, `RESOLVER`, `DEPT_HEAD`, `AUDITOR`, `ADMIN`, `GUEST`) with row-level security and field-level IDOR protections.
- 🔒 **Enterprise Defense-in-Depth:** HttpOnly JWT authentication cookies, double-submit cookie CSRF tokens (`XSRF-TOKEN` / `X-XSRF-TOKEN`), strictly gated enterprise portal routes, and zero guest data leakage.
- ⛓️ **Cryptographic Audit Ledger:** Continuous SHA-256 hash-chained immutable audit log with automated genesis verification and tamper detection.
- 📊 **Operational Analytics:** Real-time SLA compliance tracking, department resolution KPIs, and queue throughput metrics.
- 🎨 **Modern Design System:** Accessible WCAG 2.2 AA compliant React UI featuring system/light/dark themes, density controls, and responsive workflows.

---

## 🛠️ Technology Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Backend** | Java 25 (OpenJDK) + Spring Boot 3.5.16 | Spring Security, Spring Data JPA, Flyway V1–V4, HikariCP, JJWT |
| **Frontend** | React 18 + React Router v6 | Design Token System, React Context, Axios Interceptors, Accessibility Tree |
| **Database** | MySQL 8.0+ / Testcontainers | InnoDB, `utf8mb4_unicode_ci`, Row-Level Locking, Repeatable Read |
| **Testing** | JUnit 5 + MockMvc + Jest + RTL + Playwright | 100% Role & Workflow Matrix, Database Isolation, Zero Flakiness |

---

## 🚀 Quick Start

### 1. Backend (Spring Boot)
```bash
cd springapp/backend
mvn spring-boot:run
```

### 2. Frontend (React)
```bash
cd springapp/frontend
npm install
npm start
```
The application will be accessible at `http://localhost:3000` (or `http://localhost:8080` when packaged).

---

## 🧪 Testing Suite

- **Frontend Component Tests (Jest + React Testing Library):**
  ```bash
  cd springapp/frontend
  npm test -- --watchAll=false
  ```
- **Backend Integration Suite (JUnit 5 + MockMvc):**
  ```bash
  cd springapp/backend
  mvn test "-Dfrontend.skip=true"
  ```

---

## 👥 Seeded Enterprise Roles & Personas

| Username | Role | Default Password | Department |
| :--- | :--- | :--- | :--- |
| `admin` | `ADMIN` | `admin123` | Executive |
| `hr_manager` | `HR_MANAGER` | `hr123` | Human Resources |
| `john_doe` | `EMPLOYEE` | `user123` | Engineering |
| `sarah_connor` | `EMPLOYEE` | `user123` | Engineering |
| `alex_chen` | `EMPLOYEE` | `user123` | Finance |
| `emily_watson` | `EMPLOYEE` | `user123` | Operations |
