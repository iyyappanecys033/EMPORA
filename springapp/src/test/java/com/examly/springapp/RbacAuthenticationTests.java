package com.examly.springapp;

import com.examly.springapp.model.Role;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.RoleRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import com.examly.springapp.repository.RefreshTokenRepo;
import com.examly.springapp.service.RoleService;
import com.examly.springapp.service.UserAccountService;
import com.examly.springapp.service.UserRoleMappingService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
public class RbacAuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRoleMappingRepo userRoleMappingRepo;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleMappingService userRoleMappingService;

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

    private String adminToken;
    private String userToken;
    
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() throws Exception {
        if (approvalTaskRepository != null) approvalTaskRepository.deleteAll();
        if (ticketCommentRepository != null) ticketCommentRepository.deleteAll();
        if (serviceTicketRepository != null) serviceTicketRepository.deleteAll();
        refreshTokenRepo.deleteAll();
        userRoleMappingRepo.deleteAll();
        profileRepo.deleteAll();
        userAccountRepo.deleteAll();
        roleRepo.deleteAll();

        // Create Roles
        adminRole = new Role();
        adminRole.setRoleName("ADMIN");
        adminRole = roleService.saveRole(adminRole);

        userRole = new Role();
        userRole.setRoleName("USER");
        userRole = roleService.saveRole(userRole);

        // Create Users
        UserAccount adminUser = new UserAccount();
        adminUser.setUsername("admin_user");
        adminUser.setPasswordHash("Password123!");
        adminUser = userAccountService.saveUserAccount(adminUser);

        UserAccount normalUser = new UserAccount();
        normalUser.setUsername("normal_user");
        normalUser.setPasswordHash("Password123!");
        normalUser = userAccountService.saveUserAccount(normalUser);

        // Map Roles
        UserRoleMapping adminMap = new UserRoleMapping();
        adminMap.setUser(adminUser);
        adminMap.setRole(adminRole);
        userRoleMappingService.saveUserRoleMapping(adminMap);

        UserRoleMapping userMap = new UserRoleMapping();
        userMap.setUser(normalUser);
        userMap.setRole(userRole);
        userRoleMappingService.saveUserRoleMapping(userMap);

        // Get Tokens
        adminToken = getToken("admin_user", "Password123!");
        userToken = getToken("normal_user", "Password123!");
    }

    private String getToken(String username, String password) throws Exception {
        String payload = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("accessToken").getValue();
    }

    @Test
    void test1_UserCanAuthenticate() throws Exception {
        assertNotNull(userToken);
    }

    @Test
    void test2_AdminCanAuthenticate() throws Exception {
        assertNotNull(adminToken);
    }

    @Test
    void test3_UserAccessesMe() throws Exception {
        mockMvc.perform(get("/api/users/me").cookie(new Cookie("accessToken", userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("normal_user"));
    }

    @Test
    void test4_AdminAccessesMe() throws Exception {
        mockMvc.perform(get("/api/users/me").cookie(new Cookie("accessToken", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin_user"));
    }

    @Test
    void test5_UserAccessesAdminEndpoint_Forbidden() throws Exception {
        String newRole = "{\"roleName\": \"MANAGER\"}";
        mockMvc.perform(post("/api/roles").with(csrf())
                .cookie(new Cookie("accessToken", userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRole))
                .andExpect(status().isForbidden());
    }

    @Test
    void test6_AdminAccessesAdminEndpoint_Success() throws Exception {
        String newRole = "{\"roleName\": \"MANAGER\"}";
        mockMvc.perform(post("/api/roles").with(csrf())
                .cookie(new Cookie("accessToken", adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRole))
                .andExpect(status().isCreated());
    }

    @Test
    void test7_NoJwt_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test8_InvalidJwt_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me").cookie(new Cookie("accessToken", "invalidtoken")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test9_UserAttemptsToAssignAdminRole_Forbidden() throws Exception {
        String payload = String.format("{\"user\": {\"id\": %d}, \"role\": {\"id\": %d}}", 
                userAccountRepo.findByUsername("normal_user").get().getId(),
                adminRole.getId());
        mockMvc.perform(post("/api/userRoleMappings").with(csrf())
                .cookie(new Cookie("accessToken", userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void test10_AdminAssignsRole_Success() throws Exception {
        String payload = String.format("{\"user\": {\"id\": %d}, \"role\": {\"id\": %d}}", 
                userAccountRepo.findByUsername("admin_user").get().getId(),
                userRole.getId());
        mockMvc.perform(post("/api/userRoleMappings").with(csrf())
                .cookie(new Cookie("accessToken", adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void test11_UserAttemptsToCreateRole_Forbidden() throws Exception {
        String newRole = "{\"roleName\": \"SUPERADMIN\"}";
        mockMvc.perform(post("/api/roles").with(csrf())
                .cookie(new Cookie("accessToken", userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRole))
                .andExpect(status().isForbidden());
    }

    @Test
    void test12_AdminCreatesRole_Success() throws Exception {
        String newRole = "{\"roleName\": \"SUPERADMIN\"}";
        mockMvc.perform(post("/api/roles").with(csrf())
                .cookie(new Cookie("accessToken", adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRole))
                .andExpect(status().isCreated());
    }

    @Test
    void test13_UserModifiesAnotherUser_Forbidden() throws Exception {
        Long adminId = userAccountRepo.findByUsername("admin_user").get().getId();
        String updatePayload = "{\"username\": \"hacked\"}";
        mockMvc.perform(put("/api/users/" + adminId).with(csrf())
                .cookie(new Cookie("accessToken", userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void test14_FakeRoleInPayload_Rejected() throws Exception {
        // Assume user tries to register with fake roles; our register endpoint ignores them completely.
        // It's covered by the DTO not having a role field.
        String payload = "{\"username\": \"sneaky\", \"password\": \"123!\", \"role\": \"ADMIN\"}";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk()); // Registration works, but role is ignored.
                
        // Verification: User is not ADMIN.
        UserAccount sneaky = userAccountRepo.findByUsername("sneaky").get();
        assertTrue(userRoleMappingRepo.findByUserId(sneaky.getId()).isEmpty());
    }

    @Test
    void test15_DuplicateRoleAssignment_Conflict() throws Exception {
        // Admin already has ADMIN role. Attempt to assign ADMIN role again.
        String payload = String.format("{\"user\": {\"id\": %d}, \"role\": {\"id\": %d}}", 
                userAccountRepo.findByUsername("admin_user").get().getId(),
                adminRole.getId());
                
        mockMvc.perform(post("/api/userRoleMappings").with(csrf())
                .cookie(new Cookie("accessToken", adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict());
    }
}
