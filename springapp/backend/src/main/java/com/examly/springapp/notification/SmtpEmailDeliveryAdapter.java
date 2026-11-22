package com.examly.springapp.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero-dependency RFC 5321 / RFC 4616 / RFC 2047 SMTP email adapter supporting Mailpit (local dev/QA)
 * and authenticated SMTP relays without external library coupling or hardcoded credentials.
 *
 * Guarantees:
 * 1. Env-driven configuration; optional AUTH PLAIN when username/password are provided.
 * 2. UTF-8 MIME Base64 encoding for non-ASCII subjects and 8-bit UTF-8 body transmission.
 * 3. Strict CRLF/null header injection sanitization.
 * 4. Independent email-level idempotency guard when idempotencyKey is supplied.
 * 5. Complete failure isolation so SMTP outages/rejections/timeouts never impact in-app notifications.
 */
@Component
public class SmtpEmailDeliveryAdapter implements EmailDeliveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailDeliveryAdapter.class);
    private static final int MAX_IDEMPOTENCY_CACHE = 10_000;

    @Value("${epams.email.enabled:false}")
    private boolean enabled;

    @Value("${epams.email.host:localhost}")
    private String host;

    @Value("${epams.email.port:1025}")
    private int port;

    @Value("${epams.email.from:no-reply@epams.local}")
    private String fromAddress;

    @Value("${epams.email.username:}")
    private String username;

    @Value("${epams.email.password:}")
    private String password;

    @Value("${epams.email.timeout-ms:3000}")
    private int timeoutMs;

    @Value("${epams.email.max-retries:2}")
    private int maxRetries;

    private final Set<String> deliveredIdempotencyKeys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void clearIdempotencyCache() {
        deliveredIdempotencyKeys.clear();
    }

    @Override
    public boolean sendNotificationEmail(String recipientEmail, String subject, String body) {
        return sendNotificationEmail(recipientEmail, subject, body, null);
    }

    @Override
    public boolean sendNotificationEmail(String recipientEmail, String subject, String body, String idempotencyKey) {
        if (!enabled) {
            return false;
        }
        String safeRecipient = sanitizeHeader(recipientEmail);
        if (safeRecipient.isBlank() || !safeRecipient.contains("@")) {
            log.warn("Skipping outbound email: invalid recipient format");
            return false;
        }

        String compositeKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? safeRecipient.toLowerCase() + "|" + idempotencyKey.trim()
                : null;
        if (compositeKey != null && deliveredIdempotencyKeys.contains(compositeKey)) {
            log.debug("Suppressing duplicate outbound email via independent idempotency key");
            return false;
        }

        String safeFrom = sanitizeHeader(fromAddress);
        String encodedSubject = encodeMimeSubject(sanitizeHeader(subject));
        String safeBody = body != null ? body.replace("\r\n.", "\r\n..").replace("\n.", "\n..") : "";

        int attempts = Math.max(1, maxRetries);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                deliverViaSmtp(safeFrom, safeRecipient, encodedSubject, safeBody);
                if (compositeKey != null) {
                    if (deliveredIdempotencyKeys.size() >= MAX_IDEMPOTENCY_CACHE) {
                        deliveredIdempotencyKeys.clear();
                    }
                    deliveredIdempotencyKeys.add(compositeKey);
                }
                log.info("Outbound email dispatched cleanly: host={}, port={}, attempt={}", host, port, attempt);
                return true;
            } catch (IOException ex) {
                log.warn("Outbound email attempt {}/{} failed safely (host={}, port={}): {}",
                        attempt, attempts, host, port, ex.getMessage());
            }
        }
        log.error("Outbound email delivery exhausted retries safely; in-app notification remains unaffected");
        return false;
    }

    private void deliverViaSmtp(String from, String to, String encodedSubject, String body) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            expectCode(reader, "220");
            sendCommand(writer, reader, "EHLO epams.local", "250");

            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                String plainToken = Base64.getEncoder().encodeToString(
                        ("\0" + username + "\0" + password).getBytes(StandardCharsets.UTF_8));
                sendCommand(writer, reader, "AUTH PLAIN " + plainToken, "235");
            }

            sendCommand(writer, reader, "MAIL FROM:<" + from + ">", "250");
            sendCommand(writer, reader, "RCPT TO:<" + to + ">", "250");
            sendCommand(writer, reader, "DATA", "354");

            writer.write("From: " + from + "\r\n");
            writer.write("To: " + to + "\r\n");
            writer.write("Subject: " + encodedSubject + "\r\n");
            writer.write("MIME-Version: 1.0\r\n");
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n");
            writer.write("Content-Transfer-Encoding: 8bit\r\n");
            writer.write("\r\n");
            writer.write(body + "\r\n.\r\n");
            writer.flush();

            expectCode(reader, "250");
            try {
                sendCommand(writer, reader, "QUIT", "221");
            } catch (IOException ignored) {
                // Message already accepted with 250 OK
            }
        }
    }

    private void sendCommand(BufferedWriter writer, BufferedReader reader, String command, String expectedPrefix)
            throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
        expectCode(reader, expectedPrefix);
    }

    private void expectCode(BufferedReader reader, String expectedPrefix) throws IOException {
        String line = reader.readLine();
        if (line == null || !line.startsWith(expectedPrefix)) {
            throw new IOException("Unexpected SMTP response (expected " + expectedPrefix + "): " + line);
        }
        while (line.length() >= 4 && line.charAt(3) == '-') {
            line = reader.readLine();
            if (line == null) {
                break;
            }
        }
    }

    /**
     * Prevents SMTP header injection by stripping CR/LF/null control characters.
     */
    static String sanitizeHeader(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\r\\n\\u0000]", " ").trim();
    }

    /**
     * Encodes non-ASCII subjects using RFC 2047 UTF-8 Base64 MIME word encoding.
     */
    static String encodeMimeSubject(String sanitized) {
        if (sanitized == null || sanitized.isEmpty()) return "";
        boolean isAscii = sanitized.chars().allMatch(c -> c >= 32 && c <= 126);
        if (isAscii) {
            return sanitized;
        }
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(sanitized.getBytes(StandardCharsets.UTF_8)) + "?=";
    }
}
