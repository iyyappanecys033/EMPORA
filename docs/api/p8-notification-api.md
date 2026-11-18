# EPAMS P8 — Notification REST API Specification

**Base Path:** `/api/v1/notifications`  
**Controller:** `com.examly.springapp.controller.NotificationController`  
**Authentication:** Mandatory HTTP-Only JWT Cookie (`accessToken`)  
**CSRF:** Required (`X-XSRF-TOKEN`) on all `PUT` mutations  

---

## 1. Retrieve My Notifications

- **Endpoint:** `GET /api/v1/notifications/my`
- **Authorization:** Any authenticated user (strictly scoped to `principal.getName()`).
- **Query Parameters:**
  - `page` (int, default `0`, clamped $\ge 0$)
  - `size` (int, default `20`, clamped `1..50`)
  - `unreadOnly` (boolean, optional — when `true`, filters `isRead = false`)
  - `eventType` (string, optional — e.g., `TICKET_SUBMITTED`, `APPROVAL_REQUIRED`, `TICKET_RESOLVED`)
- **Sorting:** Strictly newest-first (`created_at DESC, id DESC`).
- **Response (`200 OK`):** Spring Data `Page<NotificationDTO>`:
```json
{
  "content": [
    {
      "id": 42,
      "eventType": "APPROVAL_REQUIRED",
      "title": "Approval Required for Ticket TKT-2026-000012",
      "message": "Ticket TKT-2026-000012 submitted by alice requires your approval.",
      "ticketId": 12,
      "ticketNumber": "TKT-2026-000012",
      "relatedEntityType": "APPROVAL",
      "relatedEntityId": 5,
      "isRead": false,
      "createdAt": "2026-11-17T20:15:00",
      "readAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```
- **Error Responses:** `401 Unauthorized` if unauthenticated.

---

## 2. Retrieve Unread Count

- **Endpoint:** `GET /api/v1/notifications/my/unread-count`
- **Authorization:** Any authenticated user (strictly scoped to own account).
- **Response (`200 OK`):** `UnreadCountDTO`:
```json
{
  "unreadCount": 3
}
```

---

## 3. Mark Single Notification as Read

- **Endpoint:** `PUT /api/v1/notifications/{id}/read`
- **CSRF Header:** `X-XSRF-TOKEN` required.
- **Authorization:** Owner ONLY. Even `ROLE_ADMIN` receives `403 Forbidden` when attempting to mark another user's private notification as read.
- **Idempotency:** Repeated calls on an already-read owned notification succeed with `200 OK` and preserve the original `readAt` timestamp.
- **Response (`200 OK`):** Updated `NotificationDTO` with `"isRead": true` and populated `"readAt"`.
- **Error Responses:**
  - `401 Unauthorized` — Unauthenticated
  - `403 Forbidden` — Missing CSRF token OR notification belongs to another user (IDOR prevention)
  - `404 Not Found` — Notification ID does not exist

---

## 4. Mark All My Notifications as Read

- **Endpoint:** `PUT /api/v1/notifications/my/read-all`
- **CSRF Header:** `X-XSRF-TOKEN` required.
- **Authorization:** Authenticated owner only (updates only rows where `recipient_id = currentUser.id AND is_read = false`).
- **Response (`200 OK`):**
```json
{
  "updatedCount": 3,
  "unreadCount": 0
}
```

