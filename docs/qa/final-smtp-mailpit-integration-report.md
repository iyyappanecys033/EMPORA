# EPAMS — Final SMTP & Mailpit Integration Verification Report

**Project:** Employee Lifecycle and Service Management Platform (EPAMS)  
**Area:** Section 5.4 — External Email Delivery (`GAP-SMTP-HARDEN` & `BLK-SMTP-EXT`)  
**Date:** 2026-11-18  

---

## 1. Honest Execution Classification

| Verification Target | Status | Explanation |
| :--- | :---: | :--- |
| **RFC 5321 / 4616 / 2047 Protocol & Failure Harness** (`Stage1RemediationVerificationTests.test3` + `P7P9ConsolidatedAcceptanceTests.scenario5`) | **`VERIFIED`** | Executed against a real localhost TCP `ServerSocket` SMTP responder verifying `EHLO`, `AUTH PLAIN`, `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT`, Unicode RFC 2047 subjects, CRLF header sanitization, `550`/`535` rejection, socket timeout, independent email idempotency, and outage isolation. |
| **External Mailpit Daemon Integration (`localhost:1025` + Web API `:8025`)** | **`BLOCKED` (`BLK-SMTP-EXT`)** | `mailpit` binary is not installed on host PATH and Docker Desktop daemon is stopped (`open //./pipe/docker_engine` unavailable). In-process TCP SMTP testing is **NOT** claimed as a substitute for a real external Mailpit daemon run. |

---

## 2. Hardening Implemented in `SmtpEmailDeliveryAdapter`

1. **Optional SMTP Authentication (`AUTH PLAIN`, RFC 4616):** Enabled automatically when env-configured `epams.email.username` & `epams.email.password` are non-blank; zero hardcoded credentials.
2. **Full Unicode Support (RFC 2047 + UTF-8):** Non-ASCII subjects encoded as `=?UTF-8?B?...?=` with `MIME-Version: 1.0`, `Content-Type: text/plain; charset=UTF-8`, and `Content-Transfer-Encoding: 8bit`.
3. **Header Injection Prevention:** `sanitizeHeader()` strips `\r`, `\n`, `\0` from `From`, `To`, and `Subject`.
4. **Independent Email-Level Idempotency:** `sendNotificationEmail(..., idempotencyKey)` maintains an independent per-recipient idempotency set so direct retries cannot resend duplicate emails even independently of DB `uk_alert_recipient_dedup`.
5. **Failure & Privacy Isolation:** Minimal email templates exclude internal comments/PII; SMTP `550` rejections, `535` auth failures, `SO_TIMEOUT` stalls, and closed sockets return `false` cleanly without rolling back or affecting in-app alerts.

---

## 3. Reproducible Real Mailpit Verification Command (Online / Docker Host)

```powershell
docker run -d --name epams-mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
$env:EPAMS_EMAIL_ENABLED="true"; $env:EPAMS_EMAIL_HOST="127.0.0.1"; $env:EPAMS_EMAIL_PORT="1025"
# Trigger ticket resolution via API, then inspect captured messages:
Invoke-RestMethod http://127.0.0.1:8025/api/v1/messages
```
