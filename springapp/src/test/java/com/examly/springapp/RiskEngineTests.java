package com.examly.springapp;

import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.RoleRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.security.AuthService;
import com.examly.springapp.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RiskEngineTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private UserRoleMappingRepo userRoleMappingRepo;

    @BeforeEach
    public void setup() {
        refreshTokenRepo.deleteAll();
    }

    private UserAccount createTestUser(String username) {
        try {
            userAccountService.findByUsername(username).ifPresent(u -> {
                userRoleMappingRepo.deleteAll(userRoleMappingRepo.findByUserId(u.getId()));
                userAccountService.deleteUserAccount(u.getId());
            });
        } catch (Exception e) {}
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword("password123");
        registerRequest.setEmail(username + "@test.com");
        registerRequest.setPhone("1234567890");
        authService.register(registerRequest);
        return userAccountService.findByUsername(username).get();
    }

    private void addRoleToUser(UserAccount user, String roleName) {
        Role role = roleRepo.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(roleName);
                    return roleRepo.save(r);
                });
        UserRoleMapping mapping = new UserRoleMapping();
        mapping.setUser(user);
        mapping.setRole(role);
        userRoleMappingRepo.save(mapping);
    }

    @Test
    @Order(1)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test1_NormalUserReturnsLowRisk() throws Exception {
        UserAccount user = createTestUser("riskuser1");
        
        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(0)))
                .andExpect(jsonPath("$.riskLevel", is("LOW")))
                .andExpect(jsonPath("$.explanations", empty()));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test2_AdminRoleReturnsPlus30() throws Exception {
        UserAccount user = createTestUser("riskuser2");
        addRoleToUser(user, "ADMIN");
        
        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(30)))
                .andExpect(jsonPath("$.riskLevel", is("MEDIUM")))
                .andExpect(jsonPath("$.explanations", hasItem("Privileged ADMIN role")));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test3_MultiplePrivilegedRolesReturnsPlus15() throws Exception {
        UserAccount user = createTestUser("riskuser3");
        addRoleToUser(user, "ADMIN");
        addRoleToUser(user, "MANAGER");
        
        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(45)))
                .andExpect(jsonPath("$.riskLevel", is("MEDIUM")))
                .andExpect(jsonPath("$.explanations", hasItems("Privileged ADMIN role", "Multiple privileged roles")));
    }

    @Test
    @Order(4)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test4_ThreeFailedAttemptsReturnsPlus10() throws Exception {
        UserAccount user = createTestUser("riskuser4");
        user.setFailedLoginAttempts(3);
        
        // Use direct save to skip hashing
        UserAccount savedUser = userAccountService.getUserAccountById(user.getId()).get();
        savedUser.setFailedLoginAttempts(3);
        userAccountService.saveUserAccount(savedUser); 
        // Wait, standard save hashes password. Since we don't login, it's fine.

        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(10)))
                .andExpect(jsonPath("$.riskLevel", is("LOW")))
                .andExpect(jsonPath("$.explanations", hasItem("Multiple failed login attempts")));
    }

    @Test
    @Order(5)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test5_FiveFailedAttemptsReturnsPlus20() throws Exception {
        UserAccount user = createTestUser("riskuser5");
        UserAccount dbUser = userAccountService.getUserAccountById(user.getId()).get();
        dbUser.setFailedLoginAttempts(5);
        userAccountService.saveUserAccount(dbUser); 

        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(20)))
                .andExpect(jsonPath("$.riskLevel", is("LOW")))
                .andExpect(jsonPath("$.explanations", hasItem("Repeated failed login attempts")));
    }

    @Test
    @Order(6)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test6_ActiveLockReturnsPlus20() throws Exception {
        UserAccount user = createTestUser("riskuser6");
        UserAccount dbUser = userAccountService.getUserAccountById(user.getId()).get();
        dbUser.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        userAccountService.saveUserAccount(dbUser); 

        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(20)))
                .andExpect(jsonPath("$.riskLevel", is("LOW")))
                .andExpect(jsonPath("$.explanations", hasItem("Active security lock")));
    }

    @Test
    @Order(7)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test7_ExpiredLockReturnsNoAdditionalRisk() throws Exception {
        UserAccount user = createTestUser("riskuser7");
        UserAccount dbUser = userAccountService.getUserAccountById(user.getId()).get();
        dbUser.setLockedUntil(LocalDateTime.now().minusMinutes(15));
        userAccountService.saveUserAccount(dbUser); 

        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(0)));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test8_SuspendedAccountReturnsPlus20() throws Exception {
        UserAccount user = createTestUser("riskuser8");
        userAccountService.suspendUser(user.getId());

        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(20)))
                .andExpect(jsonPath("$.explanations", hasItem("Account is administratively suspended")));
    }

    @Test
    @Order(9)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test9_DisabledAccountReturnsPlus30() throws Exception {
        UserAccount user = createTestUser("riskuser9");
        userAccountService.disableUser(user.getId());

        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(30)))
                .andExpect(jsonPath("$.explanations", hasItem("Account is administratively disabled")));
    }

    @Test
    @Order(10)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test10_CombinedRiskFactorsAndCap() throws Exception {
        UserAccount user = createTestUser("riskuser10");
        addRoleToUser(user, "ADMIN");
        addRoleToUser(user, "MANAGER");
        UserAccount dbUser = userAccountService.getUserAccountById(user.getId()).get();
        dbUser.setFailedLoginAttempts(5);
        dbUser.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        dbUser.setStatus(AccountStatus.DISABLED);
        userAccountService.saveUserAccount(dbUser);
        
        // Calculation: 30(ADMIN) + 15(Multiple) + 20(Failed) + 20(Lock) + 30(Disabled) = 115
        // Should be capped at 100.
        mockMvc.perform(get("/api/users/" + user.getId() + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(100)))
                .andExpect(jsonPath("$.riskLevel", is("CRITICAL")))
                .andExpect(jsonPath("$.explanations", hasItems(
                        "Privileged ADMIN role",
                        "Multiple privileged roles",
                        "Repeated failed login attempts",
                        "Active security lock",
                        "Account is administratively disabled"
                )));
    }

    @Test
    @Order(11)
    public void test11_UnauthenticatedAccessIs401() throws Exception {
        mockMvc.perform(get("/api/users/1/risk"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @WithMockUser(username = "normalUser", roles = {"USER"})
    public void test12_UserAccessIs403() throws Exception {
        mockMvc.perform(get("/api/users/1/risk"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void test13_UnknownUserIs404() throws Exception {
        mockMvc.perform(get("/api/users/999999/risk"))
                .andExpect(status().isNotFound());
    }
}
