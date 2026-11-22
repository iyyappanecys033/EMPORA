package com.examly.springapp.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OWASP Top 10:2025 Defensive Unit Tests")
public class OwaspSecurityUnitTests {

    private final AesGcmEncryptionConverter encryptionConverter = new AesGcmEncryptionConverter();

    @Test
    @DisplayName("[A04: Cryptographic Failures] AES-256-GCM correctly encrypts and decrypts sensitive PII round-trip")
    void testAesGcmEncryptionRoundTrip() {
        String sensitiveAddress = "Flat 402, High-Tech Tower, Whitefield, Bangalore, Karnataka - 560066";
        
        // 1. Encrypt
        String ciphertext = encryptionConverter.convertToDatabaseColumn(sensitiveAddress);
        assertNotNull(ciphertext);
        assertNotEquals(sensitiveAddress, ciphertext, "Ciphertext must not match raw plaintext");
        assertFalse(ciphertext.contains("Bangalore"), "Ciphertext must never expose plaintext keywords");

        // 2. Decrypt
        String decrypted = encryptionConverter.convertToEntityAttribute(ciphertext);
        assertEquals(sensitiveAddress, decrypted, "Decrypted text must match original plaintext exactly");
    }

    @Test
    @DisplayName("[A04: Cryptographic Failures] AES-256-GCM uses non-repeating random IVs for semantic security")
    void testAesGcmRandomIvDistinctCiphertexts() {
        String emergencyContact = "+91-9876543210";

        String cipher1 = encryptionConverter.convertToDatabaseColumn(emergencyContact);
        String cipher2 = encryptionConverter.convertToDatabaseColumn(emergencyContact);

        assertNotEquals(cipher1, cipher2, "Encrypting the same plaintext twice must produce distinct ciphertexts due to fresh 96-bit random IVs");
        assertEquals(emergencyContact, encryptionConverter.convertToEntityAttribute(cipher1));
        assertEquals(emergencyContact, encryptionConverter.convertToEntityAttribute(cipher2));
    }

    @Test
    @DisplayName("[A04: Cryptographic Failures] Graceful handling of null and empty strings")
    void testAesGcmNullAndEmptyHandling() {
        assertNull(encryptionConverter.convertToDatabaseColumn(null));
        assertNull(encryptionConverter.convertToEntityAttribute(null));
        assertEquals("", encryptionConverter.convertToDatabaseColumn(""));
        assertEquals("", encryptionConverter.convertToEntityAttribute(""));
    }

    @Test
    @DisplayName("[A09: Security Logging] LogSanitizer neutralizes CRLF log injection attacks")
    void testLogSanitizerCrlfNeutralization() {
        String maliciousLogPayload = "admin\r\n[CRITICAL] 2026-11-22 10:00:00 - User role escalated to SUPER_ADMIN";
        String sanitized = LogSanitizer.sanitize(maliciousLogPayload);

        assertFalse(sanitized.contains("\r"), "Sanitized log must not contain carriage returns");
        assertFalse(sanitized.contains("\n"), "Sanitized log must not contain line feeds");
        assertEquals("admin__[CRITICAL] 2026-11-22 10:00:00 - User role escalated to SUPER_ADMIN", sanitized);
    }

    @Test
    @DisplayName("[A09: Security Logging] LogSanitizer masks sensitive tokens and PII")
    void testLogSanitizerTokenAndPiiMasking() {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNH";
        String maskedToken = LogSanitizer.maskToken(jwtToken);
        assertEquals("eyJh...VmNH", maskedToken);

        String phoneNumber = "9876543210";
        String maskedPhone = LogSanitizer.maskPii(phoneNumber);
        assertEquals("98******10", maskedPhone);
    }
}
