# EPAMS — Final Playwright Browser E2E Report (`BLK-02`)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Area:** Section 5.1 — Real Browser Chromium E2E Automation  
**Date:** 2026-11-18  
**Browser Execution Status:** **`BLOCKED` (`BLK-02`)**  

---

## 1. Honest Execution Evidence

- **Playwright Configuration & Suite Authored:**
  - `springapp/springbackend/playwright.config.js` (Desktop Chrome / Chromium project, HTML + list reporters, trace/screenshot/video on failure)
  - `springapp/springbackend/e2e/epams.spec.js` (Covers auth cookie security, catalog browsing, ticket creation, managerial approval, specialist queue & internal comments, notifications, analytics, audit ledger, and 403 route protection)
- **Why Browser Execution Is `BLOCKED` Here:**
  - `@playwright/test` is not installed in `node_modules` or global PATH (`task-3798`), and the sandbox proxy blocks `registry.npmjs.org` (`403 Forbidden ... not allowed by policy`, verified in `task-3806`), preventing installation of `@playwright/test` and Chromium browser binaries.
  - Per Non-Negotiable Rule #5, live Tomcat HTTP integration tests (`P9LiveServerE2EIntegrationTests`) and Jest component tests (`34/34`) are **NOT** substituted or claimed as real browser E2E completion.

---

## 2. Reproducible Online Execution Runbook

```powershell
# Terminal 1: Start backend on :8080
mvn spring-boot:run -f springapp/pom.xml

# Terminal 2: Start frontend on :3000
cd springapp/springbackend && npm start

# Terminal 3 (online runner): Install Playwright + Chromium once & execute
cd springapp/springbackend
npm install -D @playwright/test
npx playwright install chromium
npx playwright test --reporter=html,junit
```
