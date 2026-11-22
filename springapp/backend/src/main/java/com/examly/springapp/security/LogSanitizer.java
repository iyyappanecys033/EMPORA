package com.examly.springapp.security;

/**
 * [A09: Security Logging and Alerting Failures]
 * Security utility for sanitizing and masking log entries:
 * 1. CRLF log injection defense: Removes Carriage Return (\r) and Line Feed (\n) characters.
 * 2. Sensitive data masking: Redacts JWTs, bearer tokens, passwords, and phone numbers.
 */
public final class LogSanitizer {

    private LogSanitizer() {}

    /**
     * Strips CR (\r) and LF (\n) to prevent log injection and forging of false log lines.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace('\r', '_').replace('\n', '_');
    }

    /**
     * Masks sensitive tokens or credentials before writing to log streams.
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "********";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /**
     * Masks phone numbers or PII (e.g. 9876543210 -> 98******10).
     */
    public static String maskPii(String pii) {
        if (pii == null || pii.length() <= 4) {
            return "****";
        }
        int len = pii.length();
        return pii.substring(0, 2) + "*".repeat(Math.max(0, len - 4)) + pii.substring(len - 2);
    }
}
