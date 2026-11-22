package com.examly.springapp;

import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.service.UserAccountService;
import com.examly.springapp.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtAuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private com.examly.springapp.repository.UserRoleMappingRepo userRoleMappingRepo;

    @Autowired
    private com.examly.springapp.repository.AuditLogRepo auditLogRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private com.examly.springapp.repository.ProfileRepo profileRepo;

    @Autowired(required = false)
    private com.examly.springapp.repository.ApprovalTaskRepository approvalTaskRepository;

    @Autowired(required = false)
    private com.examly.springapp.repository.TicketCommentRepository ticketCommentRepository;

    @Autowired(required = false)
    private com.examly.springapp.repository.ServiceTicketRepository serviceTicketRepository;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        if (approvalTaskRepository != null) approvalTaskRepository.deleteAll();
        if (ticketCommentRepository != null) ticketCommentRepository.deleteAll();
        if (serviceTicketRepository != null) serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();
        auditLogRepo.deleteBySequenceNumberIsNull();
        userRoleMappingRepo.deleteAll();
        profileRepo.deleteAll();
        userAccountRepo.deleteAll();
        UserAccount newUser = new UserAccount();
        newUser.setUsername("jwt_user");
        newUser.setPasswordHash("Password123!");
        newUser.setEmail("jwt@test.com");
        testUser = userAccountService.saveUserAccount(newUser);
    }

    @Test
    void test1_ValidLogin() throws Exception {
        String payload = "{\"username\": \"jwt_user\", \"password\": \"Password123!\"}";
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.token").doesNotExist()) // Token shouldn't be in JSON anymore
                .andExpect(jsonPath("$.username").value("jwt_user"));
    }

    @Test
    void test2_WrongPassword() throws Exception {
        String payload = "{\"username\": \"jwt_user\", \"password\": \"WrongPassword\"}";
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized()); // Or 403 depending on default error handler, typically 401/403
    }

    @Test
    void test3_UnknownUsername() throws Exception {
        String payload = "{\"username\": \"nobody\", \"password\": \"Password123!\"}";
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized()); 
    }

    @Test
    void test4_GetMeWithValidJwt() throws Exception {
        // First login to get token
        String payload = "{\"username\": \"jwt_user\", \"password\": \"Password123!\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andReturn();
        
        Cookie accessToken = result.getResponse().getCookie("accessToken");
        assertNotNull(accessToken);

        mockMvc.perform(get("/api/users/me")
                .cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jwt_user"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void test5_GetMeWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized()); // Usually 403 or 401
    }

    @Test
    void test6_GetProtectedApiWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test7_GetProtectedApiWithMalformedJwt() throws Exception {
        mockMvc.perform(get("/api/roles")
                .cookie(new Cookie("accessToken", "malformed.token.here")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test8_GetProtectedApiWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/roles")
                .cookie(new Cookie("accessToken", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmYWtlIn0.invalid_signature")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test9_JwtMustNotContainPassword() throws Exception {
        String payload = "{\"username\": \"jwt_user\", \"password\": \"Password123!\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andReturn();
                
        Cookie accessToken = result.getResponse().getCookie("accessToken");
        assertNotNull(accessToken);
        String token = accessToken.getValue();

        String payloadBase64 = token.split("\\.")[1];
        String decodedPayload = new String(Base64.getUrlDecoder().decode(payloadBase64));
        
        assertFalse(decodedPayload.contains("Password123!"));
        assertFalse(decodedPayload.contains("password"));
        assertFalse(decodedPayload.contains("passwordHash"));
    }

    @Test
    void test10_ExistingBcryptHashRemainsCompatible() throws Exception {
        // Our setUp() uses UserAccountService which uses BCrypt. 
        // This is implicitly tested by test1_ValidLogin successfully logging in.
        // We ensure that saving it natively allows login.
        assertTrue(testUser.getPasswordHash().startsWith("$2a$") || testUser.getPasswordHash().startsWith("$2y$"));
    }

    @Test
    void test11_RegisterAndLoginFlow() throws Exception {
        // 1. Register a new user
        String registerPayload = "{\"username\": \"new_user\", \"password\": \"NewPassword123!\", \"email\": \"new@test.com\", \"phone\": \"1234567890\"}";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(jsonPath("$.username").value("new_user"));

        // 2. Verify BCrypt hashing in database
        UserAccount savedUser = userAccountRepo.findByUsername("new_user").orElseThrow();
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$") || savedUser.getPasswordHash().startsWith("$2y$"));
        
        // 3. Login with the new user
        String loginPayload = "{\"username\": \"new_user\", \"password\": \"NewPassword123!\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andReturn();
                
        Cookie accessToken = result.getResponse().getCookie("accessToken");

        // 4. Verify /api/users/me works with the token
        mockMvc.perform(get("/api/users/me")
                .cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_user"))
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
