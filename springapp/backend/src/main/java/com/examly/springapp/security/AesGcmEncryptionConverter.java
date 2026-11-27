package com.examly.springapp.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * [A04: Cryptographic Failures]
 * Production-grade AES-256-GCM JPA AttributeConverter for encrypting sensitive PII at rest in MySQL.
 * 
 * Cryptographic Specifications:
 * - Algorithm: AES/GCM/NoPadding (Authenticated Encryption with Associated Data)
 * - Key Length: 256 bits (32 bytes)
 * - Initialization Vector (IV): 12 bytes (96 bits) randomly generated per encryption via SecureRandom
 * - Authentication Tag Length: 128 bits
 * - Format: Base64(12-byte IV + Ciphertext + 16-byte GCM Tag)
 */
@Converter
public class AesGcmEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(AesGcmEncryptionConverter.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static SecretKey secretKey;

    static {
        initializeKey();
    }

    private static synchronized void initializeKey() {
        String envKey = System.getenv("EMPORA_ENCRYPTION_KEY");
        if (envKey == null || envKey.trim().isEmpty()) {
            byte[] ephemeralKey = new byte[32];
            SECURE_RANDOM.nextBytes(ephemeralKey);
            secretKey = new SecretKeySpec(ephemeralKey, "AES");
            log.warn("[A04: Cryptographic Failures] No external key supplied; using an ephemeral key for local development.");
            return;
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(envKey.trim());
        } catch (IllegalArgumentException e) {
            throw new ExceptionInInitializerError("EMPORA_ENCRYPTION_KEY must be Base64 encoded");
        }
        if (decodedKey.length != 32) {
            throw new ExceptionInInitializerError("EMPORA_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        secretKey = new SecretKeySpec(decodedKey, "AES");
        log.info("[A04: Cryptographic Failures] Initialized AES-256-GCM key from external environment.");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintextBytes = attribute.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("[A04: Cryptographic Failures] Encryption failed for sensitive attribute", e);
            throw new IllegalStateException("Failed to encrypt sensitive field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            if (decoded.length < GCM_IV_LENGTH_BYTES + 16) {
                // If legacy unencrypted data exists, return gracefully without crashing
                return dbData;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If ciphertext cannot be decrypted (e.g. was plain text pre-migration), return as-is
            log.debug("[A04: Cryptographic Failures] Value not in AES-256-GCM format or legacy plain text, returning verbatim.");
            return dbData;
        }
    }
}
