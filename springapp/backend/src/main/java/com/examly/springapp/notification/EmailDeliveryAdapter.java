package com.examly.springapp.notification;

/**
 * Provider-neutral outbound email notification adapter (NFR-013).
 *
 * Implementations MUST:
 * - Never throw uncaught exceptions that disrupt in-app alert persistence or committed business transactions.
 * - Never embed secrets, credentials, PII, or internal specialist notes in outbound emails.
 * - Read host/port/auth strictly from externalized configuration/environment variables.
 */
public interface EmailDeliveryAdapter {

    /**
     * Dispatches a safe notification email if external email delivery is enabled.
     *
     * @param recipientEmail target email address
     * @param subject        sanitized subject line
     * @param body           safe plain-text notification body
     * @return true if accepted by SMTP relay, false if disabled or failed safely
     */
    boolean sendNotificationEmail(String recipientEmail, String subject, String body);

    /**
     * Dispatches a safe notification email with an optional independent email-level idempotency key.
     */
    default boolean sendNotificationEmail(String recipientEmail, String subject, String body, String idempotencyKey) {
        return sendNotificationEmail(recipientEmail, subject, body);
    }

    boolean isEnabled();
}
