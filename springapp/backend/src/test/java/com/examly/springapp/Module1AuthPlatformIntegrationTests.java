package com.examly.springapp;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.testinfra.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Module 1: Authentication & Platform Foundational Tests (AUTH-* and PLAT-*)")
public class Module1AuthPlatformIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private com.examly.springapp.security.RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("AUTH-01: GET /api/auth/csrf generates and returns XSRF token")
    void testAuth01_CsrfTokenEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName", is("X-XSRF-TOKEN")))
                .andExpect(jsonPath("$.parameterName", is("_csrf")))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("AUTH-02: POST /api/auth/register creates user account and rejects duplicates")
    void testAuth02_RegisterEndpoint() throws Exception {
        RegisterRequest validReq = new RegisterRequest();
        validReq.setUsername("new_emp_01");
        validReq.setPassword("SecureP@ssw0rd123");
        validReq.setEmail("new.emp@empora.local");
        validReq.setPhone("9876543210");

        // 1. Success 200/201
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"));

        Optional<UserAccount> created = userAccountRepo.findByUsername("new_emp_01");
        assertTrue(created.isPresent(), "User must be persisted to MySQL database");
        assertEquals(AccountStatus.ACTIVE, created.get().getStatus());

        // 2. Duplicate registration rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AUTH-03: POST /api/auth/login authenticates valid user with HttpOnly cookies and rejects bad credentials")
    void testAuth03_LoginEndpoint() throws Exception {
        authHelper.getOrCreateUser("login_user", "EMPLOYEE", "IT", "admin", 75000.0);

        // 1. Valid login
        LoginRequest validLogin = new LoginRequest();
        validLogin.setUsername("login_user");
        validLogin.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("login_user")))
                .andExpect(jsonPath("$.message", containsString("Login successful")))
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        Cookie accessCookie = result.getResponse().getCookie("accessToken");
        assertNotNull(accessCookie);
        assertTrue(accessCookie.isHttpOnly(), "accessToken cookie MUST be HttpOnly to prevent XSS theft");

        // 2. Bad password
        LoginRequest badLogin = new LoginRequest();
        badLogin.setUsername("login_user");
        badLogin.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH-04: POST /api/auth/refresh rotates accessToken using valid refresh token")
    void testAuth04_RefreshTokenEndpoint() throws Exception {
        authHelper.getOrCreateUser("refresh_user", "EMPLOYEE", "IT", "admin", 75000.0);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("refresh_user");
        loginReq.setPassword("Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshValue = extractCookieValue(loginResult, "refreshToken");
        assertNotNull(refreshValue, "Login must issue refreshToken cookie");
        assertFalse(refreshValue.isEmpty(), "refreshToken value must not be empty");

        Cookie refreshCookieToSend = new Cookie("refreshToken", refreshValue);

        // Use refresh token cookie to get refreshed access token
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookieToSend))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"));

        // Invalid refresh cookie returns 401
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "invalid-fake-token-hash")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AUTH-05: POST /api/auth/logout wipes auth cookies and revokes refresh token")
    void testAuth05_LogoutEndpoint() throws Exception {
        UserAccount user = authHelper.getOrCreateUser("logout_user", "EMPLOYEE", "IT", "admin", 75000.0);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("logout_user");
        loginReq.setPassword("Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String accessValue = extractCookieValue(loginResult, "accessToken");
        String refreshValue = extractCookieValue(loginResult, "refreshToken");

        Cookie accessCookie = new Cookie("accessToken", accessValue != null ? accessValue : "");
        Cookie refreshCookie = new Cookie("refreshToken", refreshValue != null ? refreshValue : "");

        // Perform logout with valid session
        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .with(authHelper.withCsrf())
                        .cookie(accessCookie, refreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        // Check wiped cookies (Max-Age=0)
        Cookie clearedAccess = logoutResult.getResponse().getCookie("accessToken");
        assertNotNull(clearedAccess);
        assertEquals(0, clearedAccess.getMaxAge(), "Logout must expire accessToken cookie");

        // Verify refresh token revoked in database
        if (refreshValue != null && !refreshValue.isEmpty()) {
            String tokenHash = refreshTokenService.hashToken(refreshValue);
            Optional<RefreshToken> storedToken = refreshTokenRepo.findByTokenHash(tokenHash);
            storedToken.ifPresent(token -> assertTrue(token.isRevoked(), "Refresh token must be marked revoked in DB"));
        }
    }

    private String extractCookieValue(MvcResult result, String cookieName) {
        for (String header : result.getResponse().getHeaders("Set-Cookie")) {
            if (header.startsWith(cookieName + "=")) {
                String tokenPart = header.split(";")[0];
                return tokenPart.substring((cookieName + "=").length());
            }
        }
        Cookie c = result.getResponse().getCookie(cookieName);
        return c != null ? c.getValue() : null;
    }

    @Test
    @DisplayName("PLAT-01: GET /api/v1/health is publicly accessible and returns UP")
    void testPlat01_HealthProbe() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.database", is("UP")));
    }

    @Test
    @DisplayName("PLAT-02: GET /api/docs provides EMPORA REST API schema inventory")
    void testPlat02_ApiDocs() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", containsString("EMPORA")))
                .andExpect(jsonPath("$.tagline", is("One platform. Connected operations.")))
                .andExpect(jsonPath("$.modules", not(empty())));
    }

    @Test
    @DisplayName("CSRF Security: Mutating POST without CSRF or mismatched header is rejected with 403 Forbidden")
    void testCsrf_DoubleSubmitValidation() throws Exception {
        // Missing CSRF
        mockMvc.perform(post("/api/v1/tickets")
                        .with(authHelper.withRole("EMPLOYEE"))
                        .with(authHelper.withMissingCsrfHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Mismatched CSRF
        mockMvc.perform(post("/api/v1/tickets")
                        .with(authHelper.withRole("EMPLOYEE"))
                        .with(authHelper.withMismatchedCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
