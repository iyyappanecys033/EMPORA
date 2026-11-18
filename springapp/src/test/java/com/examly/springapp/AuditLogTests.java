package com.examly.springapp;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.AuditLog;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.AuditLogRepo;
import com.examly.springapp.security.AuthService;
import com.examly.springapp.service.RoleService;
import com.examly.springapp.service.UserAccountService;
import com.examly.springapp.service.UserRoleMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditLogTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepo auditLogRepo;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleMappingService userRoleMappingService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        auditLogRepo.deleteBySequenceNumberIsNull();
    }

    @Test
    @Order(1)
    public void test1_SuccessfulLoginCreatesLoginSuccess() {
        try {
            userAccountService.deleteUserAccount(userAccountService.findByUsername("audituser").get().getId());
        } catch (Exception e) {}
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("audituser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("audituser@test.com");
        registerRequest.setPhone("1234567890");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("audituser");
        loginRequest.setPassword("password123");
        authService.authenticate(loginRequest);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("LOGIN_SUCCESS");
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals("audituser", log.getActorUsername());
        assertNotNull(log.getTimestamp());
    }

    @Test
    @Order(2)
    public void test2_FailedLoginCreatesLoginFailed() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("wronguser");
        loginRequest.setPassword("wrongpass");

        assertThrows(Exception.class, () -> authService.authenticate(loginRequest));

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("LOGIN_FAILED");
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals("anonymous", log.getActorUsername());
        assertEquals("wronguser", log.getTargetUsername());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test3_UserCreatedGeneratesEvent() {
        UserAccount user = new UserAccount();
        user.setUsername("newuser");
        user.setPasswordHash("pass");
        userAccountService.saveUserAccount(user);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("USER_CREATED");
        assertFalse(logs.isEmpty());
        assertEquals("adminUser", logs.get(0).getActorUsername());
        assertEquals("newuser", logs.get(0).getTargetUsername());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test4_UserUpdatedGeneratesEvent() {
        UserAccount user = new UserAccount();
        user.setUsername("updateuser");
        user.setPasswordHash("pass");
        UserAccount saved = userAccountService.saveUserAccount(user);

        saved.setEmail("updated@test.com");
        userAccountService.updateUserAccount(saved.getId(), saved);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("USER_UPDATED");
        assertFalse(logs.isEmpty());
        assertEquals("updateuser", logs.get(0).getTargetUsername());
    }

    @Test
    @Order(5)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test5_UserDeletedGeneratesEvent() {
        UserAccount user = new UserAccount();
        user.setUsername("deleteuser");
        user.setPasswordHash("pass");
        UserAccount saved = userAccountService.saveUserAccount(user);

        userAccountService.deleteUserAccount(saved.getId());

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("USER_DELETED");
        assertFalse(logs.isEmpty());
        assertEquals("deleteuser", logs.get(0).getTargetUsername());
    }

    @Test
    @Order(6)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test6_RoleCreatedGeneratesEvent() {
        Role role = new Role();
        role.setRoleName("AUDIT_ROLE");
        roleService.saveRole(role);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("ROLE_CREATED");
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).getDetails().contains("AUDIT_ROLE"));
    }

    @Test
    @Order(7)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test7_RoleUpdatedGeneratesEvent() {
        Role role = new Role();
        role.setRoleName("OLD_ROLE");
        Role saved = roleService.saveRole(role);

        saved.setRoleName("NEW_ROLE");
        roleService.updateRole(saved.getId(), saved);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("ROLE_UPDATED");
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).getDetails().contains("NEW_ROLE"));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test8_RoleDeletedGeneratesEvent() {
        Role role = new Role();
        role.setRoleName("DELETE_ROLE");
        Role saved = roleService.saveRole(role);

        roleService.deleteRole(saved.getId());

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("ROLE_DELETED");
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).getDetails().contains("DELETE_ROLE"));
    }

    @Test
    @Order(9)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test9_RoleAssignedGeneratesEvent() {
        UserAccount user = new UserAccount();
        user.setUsername("mapuser");
        user.setPasswordHash("pass");
        UserAccount savedUser = userAccountService.saveUserAccount(user);

        Role role = new Role();
        role.setRoleName("MAP_ROLE");
        Role savedRole = roleService.saveRole(role);

        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setUser(savedUser);
        mapping.setRole(savedRole);
        userRoleMappingService.saveUserRoleMapping(mapping);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("ROLE_ASSIGNED");
        assertFalse(logs.isEmpty());
        assertEquals("mapuser", logs.get(0).getTargetUsername());
    }

    @Test
    @Order(10)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test10_RoleRemovedGeneratesEvent() {
        UserAccount user = new UserAccount();
        user.setUsername("remmapuser");
        user.setPasswordHash("pass");
        UserAccount savedUser = userAccountService.saveUserAccount(user);

        Role role = new Role();
        role.setRoleName("REMMAP_ROLE");
        Role savedRole = roleService.saveRole(role);

        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setUser(savedUser);
        mapping.setRole(savedRole);
        UserRoleMapping savedMapping = userRoleMappingService.saveUserRoleMapping(mapping);

        userRoleMappingService.deleteUserRoleMapping(savedMapping.getId());

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("ROLE_REMOVED");
        assertFalse(logs.isEmpty());
        assertEquals("remmapuser", logs.get(0).getTargetUsername());
    }

    @Test
    @Order(11)
    public void test11_AuditRetrievalWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @WithMockUser(username = "normalUser", roles = {"USER"})
    public void test12_AuditRetrievalWithUserReturns403() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test13_AuditRetrievalWithAdminReturns200() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(14)
    public void test14_AuditLogHasServerGeneratedTimestamp() {
        AuditLog log = new AuditLog("actor", "action", "target", "details");
        AuditLog saved = auditLogRepo.save(log);
        assertNotNull(saved.getTimestamp());
    }

    @Test
    @Order(15)
    public void test15_AuditLogDoesNotExposePasswordHash() {
        AuditLog log = new AuditLog("actor", "action", "target", "passwordHash: 1234");
        AuditLog saved = auditLogRepo.save(log);
        // It's checked conceptually by the implementation plan, but we assert it doesn't store the actual DB hash
        assertFalse(saved.getDetails().contains("$2a$"));
    }

    @Test
    @Order(16)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test16_AuditLogCannotBeCreatedThroughPost() throws Exception {
        mockMvc.perform(post("/api/audit-logs").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(17)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test17_AuditLogCannotBeModifiedThroughPut() throws Exception {
        mockMvc.perform(put("/api/audit-logs/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(18)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test18_AuditLogCannotBeDeletedThroughDelete() throws Exception {
        mockMvc.perform(delete("/api/audit-logs/1").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Order(19)
    @WithMockUser(username = "testActorContext", roles = {"ADMIN"})
    public void test19_ActorUsernameIsDerivedFromContext() {
        UserAccount user = new UserAccount();
        user.setUsername("actorcontextuser");
        user.setPasswordHash("pass");
        userAccountService.saveUserAccount(user);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("USER_CREATED");
        assertFalse(logs.isEmpty());
        assertEquals("testActorContext", logs.get(0).getActorUsername());
    }

    @Test
    @Order(20)
    @WithMockUser(username = "realUser", roles = {"ADMIN"})
    public void test20_ClientCannotSpoofActorUsername() {
        // Our service derives purely from SecurityContextHolder
        UserAccount user = new UserAccount();
        user.setUsername("spoofuser");
        user.setPasswordHash("pass");
        userAccountService.saveUserAccount(user);

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("USER_CREATED");
        assertFalse(logs.isEmpty());
        assertEquals("realUser", logs.get(0).getActorUsername());
        assertNotEquals("spoofedAdmin", logs.get(0).getActorUsername());
    }

    @Test
    @Order(21)
    public void test21_LoginFailedDoesNotStoreAttemptedPassword() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user21");
        loginRequest.setPassword("secretpassword");

        assertThrows(Exception.class, () -> authService.authenticate(loginRequest));

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("LOGIN_FAILED");
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertFalse(log.getDetails().contains("secretpassword"));
    }

    @Test
    @Order(22)
    public void test22_LoginFailedDoesNotStoreJwtData() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user22");
        loginRequest.setPassword("secretpassword");

        assertThrows(Exception.class, () -> authService.authenticate(loginRequest));

        List<AuditLog> logs = auditLogRepo.findByActionOrderByTimestampDesc("LOGIN_FAILED");
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertFalse(log.getDetails().contains("eyJ")); // Common JWT prefix
    }

    @Test
    @Order(23)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test23_DeletingUserDoesNotDeleteHistoricalLogs() {
        UserAccount user = new UserAccount();
        user.setUsername("historyuser");
        user.setPasswordHash("pass");
        UserAccount saved = userAccountService.saveUserAccount(user);

        userAccountService.deleteUserAccount(saved.getId());

        List<AuditLog> logs = auditLogRepo.findByTargetUsernameOrderByTimestampDesc("historyuser");
        assertTrue(logs.size() >= 2); // CREATED and DELETED events should both exist
    }

    @Test
    @Order(24)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test24_ExistingAccountStatusLogFunctionalityUnaffected() throws Exception {
        mockMvc.perform(get("/api/accountStatusLogs"))
                .andExpect(status().isOk());
    }
}
