package com.examly.springapp.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Profile({"mysql", "prod"})
public class ProductionEncryptionKeyValidator {

    @Value("${empora.security.encryption-key}")
    private String encryptionKey;

    @PostConstruct
    void validateEncryptionKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);
            if (decodedKey.length != 32) {
                throw new IllegalArgumentException("key must decode to exactly 32 bytes");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("EMPORA_ENCRYPTION_KEY must be valid Base64 encoding of exactly 32 bytes", e);
        }
    }
}
