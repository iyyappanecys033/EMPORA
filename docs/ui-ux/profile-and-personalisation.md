# EPAMS — Employee Profile & Personalisation Architecture (Stage U3C)

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Components:** `EmployeeProfile.js` (`/profile`), `PreferencesPage.js` (`/preferences`), `PreferencesContext.js`, `Avatar.js`, `Navbar.js`
**Date:** 2026-11-18

---

## 1. Employee Self-Service Profile (`/profile`)
- **Backend Integration:** Fetches real employee data from `GET /api/profiles/me` (`ProfileController.java:116`), gracefully falling back to `AuthContext.currentUser` with an informational notice if an admin/service account does not have an HR profile row.
- **Displayed Sections:**
  - Identity header with generated deterministic `Avatar` monogram, full name, account status pill, designation, department, username, and employee ID.
  - Organization & Employment metadata (`employeeId`, `department`, `designation`, `reportingManager`, `dateOfJoining`, `status`).
  - Contact & Personal details (`email`, `phone`, `emergencyContact`, `dob`, `address` — visible exclusively to the authenticated owner via `/api/profiles/me`).
  - Assigned Access Roles & Governance panel.
- **Strict RBAC Honesty:** Because `PUT /api/profiles/employee` is restricted to `ROLE_ADMIN` and `ROLE_HR_MANAGER`, standard employees see a **Request Profile Update** shortcut to the Service Catalog rather than a fake editable form, while HR Managers and Admins see **Edit Profile Record**.
- **Avatar Policy:** No backend file-upload endpoint exists for profile images; therefore, a clean deterministic monogram `Avatar` is used across the workspace and transparently explained on `/profile`.

## 2. Preferences & Accessibility (`/preferences`)
- **Supported Settings:**
  - **Theme:** `Light` | `Dark` | `System` (OS `prefers-color-scheme` sync).
  - **Density:** `Comfortable` | `Compact` (adjusts table & card spacing tokens).
  - **Sidebar Default:** Expanded vs. Compact icon rail on desktop.
  - **Motion:** `Follow OS` | `Reduce Motion` | `Standard Motion`.
  - **Text Scale:** `Standard (100%)` | `Large (112.5%)`.
- **Storage Transparency:** Persisted in browser `localStorage` under `epams.preferences` and explicitly labeled in the UI as **browser/device-local** because no server-side user preference table exists in Flyway `V1`–`V4`.
