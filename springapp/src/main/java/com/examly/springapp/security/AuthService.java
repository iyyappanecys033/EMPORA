package com.examly.springapp.security;

import com.examly.springapp.dto.AuthResponse;
import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.service.UserAccountService;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserAccountService userAccountService;
    private final UserAccountRepo userAccountRepo;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       UserAccountService userAccountService,
                       UserAccountRepo userAccountRepo,
                       AuditLogService auditLogService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userAccountService = userAccountService;
        this.userAccountRepo = userAccountRepo;
        this.auditLogService = auditLogService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthTokens authenticate(LoginRequest request) {
        try {
            org.springframework.security.core.Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (org.springframework.security.core.AuthenticationException e) {
            Optional<UserAccount> optionalUser = userAccountService.findByUsername(request.getUsername());
            if (optionalUser.isPresent()) {
                UserAccount user = optionalUser.get();
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                    auditLogService.recordEvent("ACCOUNT_AUTO_LOCKED", user.getUsername(), "Account automatically locked after 5 failed login attempts");
                }
                userAccountRepo.save(user);
            }
            auditLogService.recordEvent("LOGIN_FAILED", request.getUsername(), "Authentication failed for user");
            throw e;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        
        Optional<UserAccount> successUserOpt = userAccountService.findByUsername(request.getUsername());
        if (successUserOpt.isPresent()) {
            UserAccount successUser = successUserOpt.get();
            successUser.setFailedLoginAttempts(0);
            successUser.setLockedUntil(null);
            userAccountRepo.save(successUser);
        }

        String jwtToken = jwtService.generateToken(userDetails);
        
        RefreshTokenService.RefreshTokenResult refreshTokenResult = null;
        if (successUserOpt.isPresent()) {
            refreshTokenResult = refreshTokenService.createRefreshToken(successUserOpt.get());
        }

        auditLogService.recordEvent("LOGIN_SUCCESS", userDetails.getUsername(), "User logged in successfully");
        return new AuthTokens(jwtToken, refreshTokenResult != null ? refreshTokenResult.rawToken : null, userDetails.getUsername());
    }

    public static class AuthTokens {
        public String accessToken;
        public String rawRefreshToken;
        public String username;
        public AuthTokens(String a, String r, String u) {
            this.accessToken = a;
            this.rawRefreshToken = r;
            this.username = u;
        }
    }

    public AuthTokens register(RegisterRequest request) {
        if (userAccountService.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(request.getUsername());
        userAccount.setPasswordHash(request.getPassword()); // UserAccountServiceImpl handles BCrypt encoding
        userAccount.setEmail(request.getEmail());
        userAccount.setPhone(request.getPhone());

        UserAccount savedUser = userAccountService.saveUserAccount(userAccount);
        UserDetails user = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String jwtToken = jwtService.generateToken(user);
        RefreshTokenService.RefreshTokenResult refreshTokenResult = refreshTokenService.createRefreshToken(savedUser);
        return new AuthTokens(jwtToken, refreshTokenResult.rawToken, user.getUsername());
    }

    public AuthTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh token is missing");
        }
        String tokenHash = refreshTokenService.hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenService.findByTokenHash(tokenHash);
        if (token == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid refresh token");
        }
        if (token.isRevoked()) {
            refreshTokenService.revokeAllUserTokens(token.getUserAccount());
            auditLogService.recordEvent("REFRESH_TOKEN_REUSE_DETECTED", token.getUserAccount().getUsername(), "Revoked refresh token was used, revoked all tokens");
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh token was revoked");
        }
        refreshTokenService.verifyExpiration(token);

        UserAccount userAccount = token.getUserAccount();
        if (userAccount.getStatus() != com.examly.springapp.model.AccountStatus.ACTIVE) {
            throw new org.springframework.security.authentication.LockedException("Account is not active");
        }
        if (userAccount.getLockedUntil() != null && userAccount.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new org.springframework.security.authentication.LockedException("Account is locked");
        }

        refreshTokenService.revokeToken(token);
        RefreshTokenService.RefreshTokenResult newRefreshTokenResult = refreshTokenService.createRefreshToken(userAccount);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(userAccount.getUsername());
        String newJwt = jwtService.generateToken(userDetails);
        
        auditLogService.recordEvent("REFRESH_TOKEN_ROTATED", userDetails.getUsername(), "Refresh token rotated successfully");
        return new AuthTokens(newJwt, newRefreshTokenResult.rawToken, userDetails.getUsername());
    }

    public void logout(String rawRefreshToken, String username) {
        if (rawRefreshToken != null) {
            String tokenHash = refreshTokenService.hashToken(rawRefreshToken);
            RefreshToken token = refreshTokenService.findByTokenHash(tokenHash);
            if (token != null && !token.isRevoked()) {
                refreshTokenService.revokeToken(token);
            }
        }
        auditLogService.recordEvent("LOGOUT", username != null ? username : "unknown", "User logged out");
    }
}
