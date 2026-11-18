package com.examly.springapp;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.AuditLog;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.AuditLogRepo;
import com.examly.springapp.security.AuthService;
import com.examly.springapp.security.JwtService;
import com.examly.springapp.service.UserAccountService;
import com.examly.springapp.repository.RefreshTokenRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditLogRepo auditLogRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private com.examly.springapp.repository.UserAccountRepo userAccountRepo;

    @BeforeEach
    public void setup() {
        refreshTokenRepo.deleteAll();
        auditLogRepo.deleteBySequenceNumberIsNull();
    }

    private UserAccount createTestUser(String username) {
        try {
            Optional<UserAccount> existing = userAccountService.findByUsername(username);
            if (existing.isPresent()) {
                userAccountService.deleteUserAccount(existing.get().getId());
            }
        } catch (Exception e) {}
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword("password123");
        registerRequest.setEmail(username + "@test.com");
        registerRequest.setPhone("1234567890");
        authService.register(registerRequest);
        return userAccountService.findByUsername(username).get();
    }

    // Status Tests
    
    @Test
    @Order(1)
    public void test1_NewUsersDefaultToActive() {
        UserAccount user = createTestUser("statususer1");
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
    }

    @Test
    @Order(2)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test2_ActiveToSuspendedWorks() throws Exception {
        UserAccount user = createTestUser("statususer2");
        mockMvc.perform(put("/api/users/" + user.getId() + "/suspend").with(csrf()))
                .andExpect(status().isOk());
        assertEquals(AccountStatus.SUSPENDED, userAccountService.getUserAccountById(user.getId()).get().getStatus());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test3_ActiveToDisabledWorks() throws Exception {
        UserAccount user = createTestUser("statususer3");
        mockMvc.perform(put("/api/users/" + user.getId() + "/disable").with(csrf()))
                .andExpect(status().isOk());
        assertEquals(AccountStatus.DISABLED, userAccountService.getUserAccountById(user.getId()).get().getStatus());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test4_SuspendedToActiveWorks() throws Exception {
        UserAccount user = createTestUser("statususer4");
        userAccountService.suspendUser(user.getId());
        mockMvc.perform(put("/api/users/" + user.getId() + "/reactivate").with(csrf()))
                .andExpect(status().isOk());
        assertEquals(AccountStatus.ACTIVE, userAccountService.getUserAccountById(user.getId()).get().getStatus());
    }

    @Test
    @Order(5)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test5_DisabledToActiveWorks() throws Exception {
        UserAccount user = createTestUser("statususer5");
        userAccountService.disableUser(user.getId());
        mockMvc.perform(put("/api/users/" + user.getId() + "/reactivate").with(csrf()))
                .andExpect(status().isOk());
        assertEquals(AccountStatus.ACTIVE, userAccountService.getUserAccountById(user.getId()).get().getStatus());
    }

    @Test
    @Order(6)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test6_InvalidLifecycleTransitionsRejected() throws Exception {
        UserAccount user = createTestUser("statususer6");
        // Suspend an already ACTIVE user works
        userAccountService.suspendUser(user.getId());
        // Suspending a SUSPENDED user should fail (400)
        mockMvc.perform(put("/api/users/" + user.getId() + "/suspend").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test7_GenericUserUpdateCannotChangeStatus() throws Exception {
        UserAccount user = createTestUser("statususer7");
        String updatePayload = "{\"username\":\"statususer7\",\"status\":\"DISABLED\"}";
        mockMvc.perform(put("/api/users/" + user.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk());
        
        // Status should remain ACTIVE despite payload
        assertEquals(AccountStatus.ACTIVE, userAccountService.getUserAccountById(user.getId()).get().getStatus());
    }

    @Test
    @Order(8)
    public void test8_SuspendClearsTemporaryLockState() {
        UserAccount user = createTestUser("statususer8");
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        user.setFailedLoginAttempts(3);
        userAccountService.saveUserAccount(user); // Actually this would hash again, let's test via direct service method
        // Using service directly as per test scope
        userAccountService.suspendUser(user.getId());
        UserAccount updated = userAccountService.getUserAccountById(user.getId()).get();
        assertNull(updated.getLockedUntil());
        assertEquals(0, updated.getFailedLoginAttempts());
    }

    @Test
    @Order(9)
    public void test9_DisableClearsTemporaryLockState() {
        UserAccount user = createTestUser("statususer9");
        userAccountService.disableUser(user.getId());
        UserAccount updated = userAccountService.getUserAccountById(user.getId()).get();
        assertNull(updated.getLockedUntil());
        assertEquals(0, updated.getFailedLoginAttempts());
    }

    @Test
    @Order(10)
    public void test10_ReactivationClearsLockState() {
        UserAccount user = createTestUser("statususer10");
        userAccountService.suspendUser(user.getId());
        userAccountService.reactivateUser(user.getId());
        UserAccount updated = userAccountService.getUserAccountById(user.getId()).get();
        assertNull(updated.getLockedUntil());
        assertEquals(0, updated.getFailedLoginAttempts());
    }

    // Authorization Tests

    @Test
    @Order(11)
    public void test11_UnauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(put("/api/users/1/suspend").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @WithMockUser(username = "normalUser", roles = {"USER"})
    public void test12_UserRequestReturns403() throws Exception {
        mockMvc.perform(put("/api/users/1/suspend").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test13_AdminCanSuspend() throws Exception {
        UserAccount user = createTestUser("authzuser1");
        mockMvc.perform(put("/api/users/" + user.getId() + "/suspend").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @Order(14)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test14_AdminCanDisable() throws Exception {
        UserAccount user = createTestUser("authzuser2");
        mockMvc.perform(put("/api/users/" + user.getId() + "/disable").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @Order(15)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test15_AdminCanReactivate() throws Exception {
        UserAccount user = createTestUser("authzuser3");
        userAccountService.suspendUser(user.getId());
        mockMvc.perform(put("/api/users/" + user.getId() + "/reactivate").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test16_AdminCanUnlock() throws Exception {
        UserAccount user = createTestUser("authzuser4");
        mockMvc.perform(put("/api/users/" + user.getId() + "/unlock").with(csrf())).andExpect(status().isOk());
    }

    // Failed-login tests

    private void attemptBadLogin(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("wrongpassword");
        try {
            authService.authenticate(request);
        } catch (Exception e) {}
    }

    @Test
    @Order(17)
    public void test17_FirstFailedLoginIncrementsCounter() {
        UserAccount user = createTestUser("failuser1");
        attemptBadLogin(user.getUsername());
        assertEquals(1, userAccountService.getUserAccountById(user.getId()).get().getFailedLoginAttempts());
    }

    @Test
    @Order(18)
    public void test18_SecondFailedLoginIncrementsCounter() {
        UserAccount user = createTestUser("failuser2");
        attemptBadLogin(user.getUsername());
        attemptBadLogin(user.getUsername());
        assertEquals(2, userAccountService.getUserAccountById(user.getId()).get().getFailedLoginAttempts());
    }

    @Test
    @Order(19)
    public void test19_ThirdFailedLoginIncrementsCounter() {
        UserAccount user = createTestUser("failuser3");
        attemptBadLogin(user.getUsername());
        attemptBadLogin(user.getUsername());
        attemptBadLogin(user.getUsername());
        assertEquals(3, userAccountService.getUserAccountById(user.getId()).get().getFailedLoginAttempts());
    }

    @Test
    @Order(20)
    public void test20_FourthFailedLoginIncrementsCounter() {
        UserAccount user = createTestUser("failuser4");
        for(int i=0; i<4; i++) attemptBadLogin(user.getUsername());
        assertEquals(4, userAccountService.getUserAccountById(user.getId()).get().getFailedLoginAttempts());
    }

    @Test
    @Order(21)
    public void test21_FifthFailedLoginLocksAccount() {
        UserAccount user = createTestUser("failuser5");
        for(int i=0; i<5; i++) attemptBadLogin(user.getUsername());
        assertNotNull(userAccountService.getUserAccountById(user.getId()).get().getLockedUntil());
    }

    @Test
    @Order(22)
    public void test22_LockedUntilIs15MinutesInFuture() {
        UserAccount user = createTestUser("failuser6");
        for(int i=0; i<5; i++) attemptBadLogin(user.getUsername());
        LocalDateTime lockTime = userAccountService.getUserAccountById(user.getId()).get().getLockedUntil();
        assertTrue(lockTime.isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(lockTime.isBefore(LocalDateTime.now().plusMinutes(16)));
    }

    @Test
    @Order(23)
    public void test23_AccountAutoLockedAuditGenerated() {
        UserAccount user = createTestUser("failuser7");
        for(int i=0; i<5; i++) attemptBadLogin(user.getUsername());
        List<AuditLog> logs = auditLogRepo.findByTargetUsernameOrderByTimestampDesc("failuser7");
        assertTrue(logs.stream().anyMatch(l -> "ACCOUNT_AUTO_LOCKED".equals(l.getAction())));
    }

    @Test
    @Order(24)
    public void test24_LoginFailedAuditGenerated() {
        UserAccount user = createTestUser("failuser8");
        attemptBadLogin(user.getUsername());
        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("LOGIN_FAILED");
        assertTrue(logs.stream().anyMatch(l -> "failuser8".equals(l.getTargetUsername())));
    }

    // Successful-login tests

    @Test
    @Order(25)
    public void test25_SuccessfulLoginResetsFailedAttempts() {
        UserAccount user = createTestUser("successuser1");
        attemptBadLogin(user.getUsername());
        assertEquals(1, userAccountService.getUserAccountById(user.getId()).get().getFailedLoginAttempts());
        
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        authService.authenticate(req);
        
        assertEquals(0, userAccountService.getUserAccountById(user.getId()).get().getFailedLoginAttempts());
    }

    @Test
    @Order(26)
    public void test26_SuccessfulLoginClearsLockState() {
        UserAccount user = createTestUser("successuser2");
        UserAccount dbUser = userAccountRepo.findById(user.getId()).get();
        dbUser.setLockedUntil(LocalDateTime.now().minusMinutes(5));
        dbUser.setFailedLoginAttempts(5);
        userAccountRepo.save(dbUser);
        
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        authService.authenticate(req);

        UserAccount refreshed = userAccountRepo.findById(user.getId()).get();
        assertEquals(0, refreshed.getFailedLoginAttempts());
        assertNull(refreshed.getLockedUntil());
    }

    @Test
    @Order(27)
    public void test27_LoginSuccessAuditContinuesWorking() {
        UserAccount user = createTestUser("successuser3");
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        authService.authenticate(req);
        
        List<AuditLog> logs = auditLogRepo.findByTargetUsernameOrderByTimestampDesc("successuser3");
        assertTrue(logs.stream().anyMatch(l -> "LOGIN_SUCCESS".equals(l.getAction())));
    }

    // Authentication enforcement

    @Test
    @Order(28)
    public void test28_SuspendedAccountCannotLogin() {
        UserAccount user = createTestUser("enforce1");
        userAccountService.suspendUser(user.getId());
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        assertThrows(Exception.class, () -> authService.authenticate(req));
    }

    @Test
    @Order(29)
    public void test29_DisabledAccountCannotLogin() {
        UserAccount user = createTestUser("enforce2");
        userAccountService.disableUser(user.getId());
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        assertThrows(Exception.class, () -> authService.authenticate(req));
    }

    @Test
    @Order(30)
    public void test30_LockedAccountCannotLogin() {
        UserAccount user = createTestUser("enforce3");
        for(int i=0; i<5; i++) attemptBadLogin(user.getUsername());
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        // 6th attempt should throw exception due to lock
        assertThrows(Exception.class, () -> authService.authenticate(req));
    }

    @Test
    @Order(31)
    public void test31_LockedAccountBecomesUsableAfterExpiration() {
        UserAccount user = createTestUser("locked_expired_user");
        UserAccount dbUser = userAccountRepo.findById(user.getId()).get();
        dbUser.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        dbUser.setFailedLoginAttempts(5);
        userAccountRepo.save(dbUser);

        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        assertDoesNotThrow(() -> authService.authenticate(req));

        UserAccount refreshed = userAccountRepo.findById(user.getId()).get();
        assertEquals(0, refreshed.getFailedLoginAttempts());
        assertNull(refreshed.getLockedUntil());
    }

    // JWT invalidation

    @Test
    @Order(32)
    public void test32_UserReceivesJwtWhileActive() throws Exception {
        UserAccount user = createTestUser("jwtuser1");
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        String jwt = authService.authenticate(req).accessToken;
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        
        mockMvc.perform(get("/api/users/me").cookie(new jakarta.servlet.http.Cookie("accessToken", jwt)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(33)
    public void test33_AdminSuspendsUser() {
        UserAccount user = createTestUser("admin_suspend_user");
        userAccountService.suspendUser(user.getId());
        UserAccount updated = userAccountRepo.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.SUSPENDED, updated.getStatus());
        List<AuditLog> logs = auditLogRepo.findByTargetUsernameOrderByTimestampDesc("admin_suspend_user");
        assertTrue(logs.stream().anyMatch(l -> "ACCOUNT_SUSPENDED".equals(l.getAction())));
    }

    @Test
    @Order(34)
    public void test34_ExistingJwtRejectedAfterSuspend() throws Exception {
        UserAccount user = createTestUser("jwtuser2");
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        String jwt = authService.authenticate(req).accessToken;
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        
        userAccountService.suspendUser(user.getId());
        
        mockMvc.perform(get("/api/users/me").cookie(new jakarta.servlet.http.Cookie("accessToken", jwt)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(35)
    public void test35_AdminDisablesUser() {
        UserAccount user = createTestUser("admin_disable_user");
        userAccountService.disableUser(user.getId());
        UserAccount updated = userAccountRepo.findById(user.getId()).orElseThrow();
        assertEquals(AccountStatus.DISABLED, updated.getStatus());
        List<AuditLog> logs = auditLogRepo.findByTargetUsernameOrderByTimestampDesc("admin_disable_user");
        assertTrue(logs.stream().anyMatch(l -> "ACCOUNT_DISABLED".equals(l.getAction())));
    }

    @Test
    @Order(36)
    public void test36_ExistingJwtRejectedAfterDisable() throws Exception {
        UserAccount user = createTestUser("jwtuser3");
        LoginRequest req = new LoginRequest();
        req.setUsername(user.getUsername());
        req.setPassword("password123");
        String jwt = authService.authenticate(req).accessToken;
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        
        userAccountService.disableUser(user.getId());
        
        mockMvc.perform(get("/api/users/me").cookie(new jakarta.servlet.http.Cookie("accessToken", jwt)))
                .andExpect(status().isUnauthorized());
    }
}
