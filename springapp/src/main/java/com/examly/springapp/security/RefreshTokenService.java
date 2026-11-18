package com.examly.springapp.security;

import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.RefreshTokenRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    public static class RefreshTokenResult {
        public String rawToken;
        public RefreshToken refreshToken;
        public RefreshTokenResult(String rawToken, RefreshToken refreshToken) {
            this.rawToken = rawToken;
            this.refreshToken = refreshToken;
        }
    }

    @Transactional
    public RefreshTokenResult createRefreshToken(UserAccount userAccount) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserAccount(userAccount);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000));
        refreshToken.setRevoked(false);

        refreshToken = refreshTokenRepo.save(refreshToken);
        return new RefreshTokenResult(rawToken, refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepo.delete(token);
            throw new IllegalArgumentException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    public RefreshToken findByTokenHash(String tokenHash) {
        return refreshTokenRepo.findByTokenHash(tokenHash).orElse(null);
    }
    
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepo.save(token);
    }

    @Transactional
    public void revokeAllUserTokens(UserAccount userAccount) {
        refreshTokenRepo.findByUserAccountAndRevokedFalse(userAccount).forEach(t -> {
            t.setRevoked(true);
            refreshTokenRepo.save(t);
        });
    }
}
