package com.examly.springapp.security;

import com.examly.springapp.dto.AuthResponse;
import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${jwt.expiration:900000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    private HttpHeaders generateCookieHeaders(String accessToken, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/")
                    .maxAge(jwtExpirationMs / 1000)
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        }
        if (refreshToken != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/api/auth") // restrict to auth endpoints
                    .maxAge(refreshExpirationMs / 1000)
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
        return headers;
    }

    private HttpHeaders generateClearCookieHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(0)
                .build();
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        
        // Also clear CSRF token cookie by overwriting it
        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        headers.add(HttpHeaders.SET_COOKIE, csrfCookie.toString());
        return headers;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.AuthTokens tokens = authService.authenticate(request);
            HttpHeaders headers = generateCookieHeaders(tokens.accessToken, tokens.rawRefreshToken);
            return ResponseEntity.ok().headers(headers).body(new AuthResponse(tokens.username, "Login successful"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthService.AuthTokens tokens = authService.register(request);
            HttpHeaders headers = generateCookieHeaders(tokens.accessToken, tokens.rawRefreshToken);
            return ResponseEntity.ok().headers(headers).body(new AuthResponse(tokens.username, "Registration successful"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try {
            AuthService.AuthTokens tokens = authService.refresh(refreshToken);
            HttpHeaders headers = generateCookieHeaders(tokens.accessToken, tokens.rawRefreshToken);
            return ResponseEntity.ok().headers(headers).body(new AuthResponse(tokens.username, "Token refreshed successfully"));
        } catch (Exception e) {
            log.warn("Token refresh rejected: {}", e.getMessage(), e);
            HttpHeaders headers = generateClearCookieHeaders();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        String username = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        authService.logout(refreshToken, username);
        HttpHeaders headers = generateClearCookieHeaders();
        return ResponseEntity.ok().headers(headers).body(new AuthResponse(username, "Logout successful"));
    }

    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(CsrfToken csrfToken) {
        if (csrfToken != null) {
            // Materialize deferred CSRF token so CookieCsrfTokenRepository emits Set-Cookie: XSRF-TOKEN
            return ResponseEntity.ok(Map.of(
                    "headerName", csrfToken.getHeaderName(),
                    "parameterName", csrfToken.getParameterName(),
                    "token", csrfToken.getToken()
            ));
        }
        return ResponseEntity.ok().build();
    }
}
