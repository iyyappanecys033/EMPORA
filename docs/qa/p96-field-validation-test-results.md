# EPAMS — P9.6 Field-by-Field Input Validation Test Results

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)
**Phase:** P9.6 — Stage F: Execution Results Across All `44` DTO Fields & Query/Path Parameters

| Input Category | Test Case / Payload | Endpoint Tested | Expected | Actual | Result |
| :--- | :--- | :--- | :---: | :---: | :---: |
| Missing required fields | `{}` empty JSON body | `POST /api/v1/tickets`, `POST /api/auth/register`, `POST /api/auth/login` | `400` RFC 7807 `errors[]` | `400` with exact field errors | **PASS** |
| Null / Empty / Whitespace | `{"username":"   ","password":""}` | `POST /api/auth/register`, `POST /api/auth/login` | `400` (`QA-DEF-003`) | `400` RFC 7807 | **PASS** |
| Below min length | `description` = `9` chars (`min=10`), `password` = `3` chars (`min=4`), `reason` = `14` chars (`min=15`) | `POST /api/v1/tickets`, `POST /api/auth/register`, `TicketStateMachine` | `400` / `BusinessRuleViolationException` | Rejected deterministically | **PASS** |
| Above max length | `description` = `2001` chars (`max=2000`), `department` = `51` chars (`max=50`) | `POST /api/v1/tickets` | `400` | `400` RFC 7807 | **PASS** |
| Invalid email format | `email = "not-an-email"` / `"invalid-email-format"` | `POST /api/auth/register`, `POST /api/profiles/employee` | `400` (`QA-DEF-003`) | `400` RFC 7807 | **PASS** |
| Negative number | `salary = -500.0`, `page = -10` | `POST /api/profiles/employee`, `GET /api/profiles/directory` | `400` on negative salary; `page` clamped to `0` | `400` & `number=0` | **PASS** |
| Excessive pagination | `size = 99999` / `size = 999` | `GET /api/profiles/directory`, `GET /api/v1/catalog` | Safely clamped to `100` | `size=100` | **PASS** |
| Invalid enum | `priority = "SUPER_URGENT"` | `POST /api/v1/tickets` | `400` malformed/invalid enum | `400` RFC 7807 | **PASS** |
| Invalid / disallowed `sortBy` | `sortBy = "nonExistentColumn"`, non-HR `sortBy = "salary"` | `GET /api/profiles/directory` | `400` (`QA-DEF-002`) | `400` RFC 7807 (zero 500s) | **PASS** |
| Unicode (Tamil + Emoji) | `"தமிழ் கணினி உதவி 🚀 ..."` | `POST /api/v1/tickets` | `201` UTF-8 preserved cleanly | `201` exact UTF-8 round-trip | **PASS** |
| XSS literal payload | `"<script>alert(1)</script>"` | `POST /api/v1/tickets`, React DOM rendering | Safely persisted as literal text; React auto-escapes JSX (zero script execution) | `201`, zero DOM XSS | **PASS** |
| SQLi metacharacters | `"' OR 1=1 --"` | `POST /api/v1/tickets`, directory search | Parameterized JPA bindings prevent SQL execution | `201` / safe empty match | **PASS** |
| Malformed JSON | `"{broken-json"` | `POST /api/auth/login` | `400` RFC 7807, zero stack traces | `400` clean RFC 7807 | **PASS** |
| Wrong `Content-Type` | `text/plain` to JSON API | `POST /api/auth/login` | `415` Unsupported Media Type (`QA-DEF-004`) | `415` RFC 7807 | **PASS** |
| Mass assignment | Injecting `status="CLOSED"`, `requesterId`, `assigneeId`, `role="ADMIN"` | `POST /api/v1/tickets`, `POST /api/auth/register` | Server-controlled fields ignored | Status/requester/role preserved by server | **PASS** |
| Unknown resource ID / code | `/api/v1/catalog/999999`, `/api/v1/catalog/code/NON-EXISTENT-CODE` | `ServiceCatalogController` | `404` RFC 7807 | `404` | **PASS** |
