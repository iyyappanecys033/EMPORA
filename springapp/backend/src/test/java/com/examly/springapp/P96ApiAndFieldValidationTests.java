package com.examly.springapp;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.dto.CreateTicketRequestDTO;
import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P9.6 Stages E & F — Exhaustive API contract, field-by-field validation,
 * RFC 7807 error hygiene, Unicode/XSS/SQLi boundary inputs, and SPA routing verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class P96ApiAndFieldValidationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private ProfileRepo profileRepo;
    @Autowired private ServiceCatalogRepository catalogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Validator validator;

    private ServiceCatalogItem activeCatalogItem;

    @BeforeEach
    void setUp() {
        ensureUser("p96_admin", "ADMIN", "Executive");
        ensureUser("p96_emp", "EMPLOYEE", "Engineering");

        activeCatalogItem = catalogRepository.findByItemCode("P96-IT-VAL").orElseGet(() -> {
            ServiceCatalogItem item = new ServiceCatalogItem();
            item.setItemCode("P96-IT-VAL");
            item.setTitle("Validation Test IT Service");
            item.setCategory("Hardware");
            item.setDepartment("IT");
            item.setDescription("Catalog offering for P9.6 field validation tests.");
            item.setEstimatedCost(new BigDecimal("150.00"));
            item.setSlaTargetHours(24);
            item.setRequiresManagerApproval(false);
            item.setRequiresSecondaryApproval(false);
            item.setActive(true);
            return catalogRepository.save(item);
        });
    }

    private UserAccount ensureUser(String username, String roleName, String department) {
        return userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount ua = new UserAccount();
            ua.setUsername(username);
            ua.setPasswordHash(passwordEncoder.encode("P96ValidPass!"));
            ua.setEmail(username + "@epams.test");
            ua.setPhone("9000011111");
            ua.setStatus(AccountStatus.ACTIVE);
            ua = userAccountRepo.save(ua);

            Role role = roleRepo.findByRoleName(roleName).orElseGet(() -> {
                Role r = new Role();
                r.setRoleName(roleName);
                return roleRepo.save(r);
            });
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(ua);
            m.setRole(role);
            userRoleMappingRepo.save(m);

            Profile p = new Profile();
            p.setEmployeeId("EMP-" + username.toUpperCase());
            p.setFirstName("Test");
            p.setLastName(username);
            p.setDepartment(department);
            p.setDesignation("Engineer");
            p.setSalary(85000.0);
            p.setUser(ua);
            profileRepo.save(p);
            return ua;
        });
    }

    @Test
    @DisplayName("P96-API-01: Public endpoints (/api/v1/health, /api/docs, /api/auth/csrf) and SPA deep-link routes (QA-DEF-001)")
    void testPublicPlatformEndpointsAndSpaForwarding() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk());

        // QA-DEF-001 regression: modern SPA routes forward cleanly to /index.html instead of HTTP 500
        for (String spaRoute : new String[]{"/catalog", "/tickets", "/tickets/42", "/approvals", "/queue", "/queue/IT",
                "/audit-chain", "/notifications", "/analytics", "/profile", "/preferences", "/admin/metrics"}) {
            mockMvc.perform(get(spaRoute))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    @DisplayName("P96-VAL-01: RegisterRequest & LoginRequest reject blank, whitespace, short password, invalid email, malformed JSON, and 415 Content-Type (QA-DEF-003 & QA-DEF-004)")
    void testAuthFieldValidationAndErrorContracts() throws Exception {
        // Blank username & short password -> 400 RFC 7807
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"   \",\"password\":\"123\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(2))));

        // Blank login payload -> 400 RFC 7807
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Malformed JSON -> 400 RFC 7807 without stack trace
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://empora.internal/errors/malformed-json"))
                .andExpect(content().string(not(containsString("org.springframework"))));

        // Wrong Content-Type -> 415 Unsupported Media Type (QA-DEF-004 regression)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("username=admin&password=bad"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @WithMockUser(username = "p96_emp", roles = {"EMPLOYEE"})
    @DisplayName("P96-VAL-02: CreateTicketRequestDTO field-by-field boundaries (null, empty, <10 chars, >2000 chars, invalid enum, Unicode/Tamil/emoji, XSS, SQLi)")
    void testTicketCreationFieldBoundariesAndPayloads() throws Exception {
        // Missing required fields -> 400 with field errors
        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(3))));

        // Description 9 chars (below min 10) -> 400
        Map<String, Object> tooShort = new LinkedHashMap<>();
        tooShort.put("catalogItemId", activeCatalogItem.getId());
        tooShort.put("department", "IT");
        tooShort.put("priority", "HIGH");
        tooShort.put("description", "123456789");
        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooShort)))
                .andExpect(status().isBadRequest());

        // Description 2001 chars (above max 2000) -> 400
        Map<String, Object> tooLong = new LinkedHashMap<>(tooShort);
        tooLong.put("description", "A".repeat(2001));
        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLong)))
                .andExpect(status().isBadRequest());

        // Department > 50 chars -> 400
        Map<String, Object> longDept = new LinkedHashMap<>(tooShort);
        longDept.put("department", "D".repeat(51));
        longDept.put("description", "Valid ticket description exceeding ten characters.");
        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longDept)))
                .andExpect(status().isBadRequest());

        // Unknown priority enum -> 400 malformed JSON/enum
        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogItemId\":" + activeCatalogItem.getId()
                                + ",\"department\":\"IT\",\"priority\":\"SUPER_URGENT\",\"description\":\"Valid description over ten chars.\"}"))
                .andExpect(status().isBadRequest());

        // Unicode (Tamil + emoji) + SQL metacharacters + XSS literal safely persisted via parameterized JPA
        Map<String, Object> specialChars = new LinkedHashMap<>();
        specialChars.put("catalogItemId", activeCatalogItem.getId());
        specialChars.put("department", "IT");
        specialChars.put("priority", "MEDIUM");
        specialChars.put("description", "தமிழ் கணினி உதவி 🚀 <script>alert(1)</script> ' OR 1=1 -- valid");
        mockMvc.perform(post("/api/v1/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(specialChars)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").isNotEmpty())
                .andExpect(jsonPath("$.description", containsString("தமிழ்")));
    }

    @Test
    @WithMockUser(username = "p96_emp", roles = {"EMPLOYEE"})
    @DisplayName("P96-VAL-03: Directory sortBy allowlist blocks salary oracle and invalid properties (QA-DEF-002)")
    void testDirectorySortByAllowlistAndPaginationBounds() throws Exception {
        // Valid sortBy + extreme page/size bounds safely clamped
        mockMvc.perform(get("/api/profiles/directory")
                        .param("page", "-10")
                        .param("size", "99999")
                        .param("sortBy", "firstName")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.number").value(0));

        // Non-HR/Admin attempting sortBy=salary oracle -> 400 (QA-DEF-002)
        mockMvc.perform(get("/api/profiles/directory")
                        .param("sortBy", "salary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Non-existent property -> 400 (never 500!)
        mockMvc.perform(get("/api/profiles/directory")
                        .param("sortBy", "nonExistentColumn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Service catalog endpoints & invalid ID / code -> 404
        mockMvc.perform(get("/api/v1/catalog").param("department", "IT").param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
        mockMvc.perform(get("/api/v1/catalog/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/catalog/code/NON-EXISTENT-CODE"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/catalog/code/P96-IT-VAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("P96-IT-VAL"));
    }

    @Test
    @WithMockUser(username = "p96_admin", roles = {"ADMIN"})
    @DisplayName("P96-VAL-04: HR/Admin EmployeeRequestDTO validation, CRUD endpoints (Profiles, Roles, Mappings, StatusLogs, Audit)")
    void testAdminCrudValidationAndCoverage() throws Exception {
        // Negative salary & invalid email -> 400 (QA-DEF-003)
        Map<String, Object> badEmp = new LinkedHashMap<>();
        badEmp.put("username", "p96_new_bad");
        badEmp.put("email", "invalid-email-format");
        badEmp.put("salary", -500.0);
        mockMvc.perform(post("/api/profiles/employee").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEmp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Valid HR employee create + update
        Map<String, Object> validEmp = new LinkedHashMap<>();
        validEmp.put("employeeId", "EMP-P96-9001");
        validEmp.put("username", "p96_hr_created");
        validEmp.put("password", "StrongPass!99");
        validEmp.put("email", "p96.created@epams.test");
        validEmp.put("phone", "9876500111");
        validEmp.put("role", "EMPLOYEE");
        validEmp.put("firstName", "Kavya");
        validEmp.put("lastName", "Rao");
        validEmp.put("department", "Engineering");
        validEmp.put("designation", "QA Architect");
        validEmp.put("salary", 95000.0);

        mockMvc.perform(post("/api/profiles/employee").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value("EMP-P96-9001"))
                .andExpect(jsonPath("$.salary").value(95000.0));

        // Admin can sort directory by salary
        mockMvc.perform(get("/api/profiles/directory").param("sortBy", "salary"))
                .andExpect(status().isOk());

        // Role, UserRoleMapping, AccountStatusLog, AuditLog & AuditApiController endpoints
        mockMvc.perform(get("/api/roles")).andExpect(status().isOk());
        mockMvc.perform(get("/api/roles/page/0/10")).andExpect(status().isOk());
        mockMvc.perform(get("/api/userRoleMappings")).andExpect(status().isOk());
        mockMvc.perform(get("/api/userRoleMappings/expanded")).andExpect(status().isOk());
        mockMvc.perform(get("/api/accountStatusLogs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/audit-logs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit/logs").param("page", "0").param("size", "10")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/audit/verify")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/metrics")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("P96-VAL-05: Authentication and ticket DTO accept exact field limits and reject values beyond them")
    void testExactAuthenticationAndTicketFieldBoundaries() {
        LoginRequest login = new LoginRequest();
        login.setUsername("u".repeat(100));
        login.setPassword("p".repeat(128));
        assertTrue(validator.validate(login).isEmpty());
        login.setUsername("u".repeat(101));
        assertTrue(hasViolation(login, "username"));
        login.setUsername("u");
        login.setPassword("p".repeat(129));
        assertTrue(hasViolation(login, "password"));

        RegisterRequest registration = new RegisterRequest();
        registration.setUsername("u".repeat(50));
        registration.setPassword("p".repeat(100));
        String uniqueLocalPart = "p96" + Long.toString(System.nanoTime(), 36);
        registration.setEmail(uniqueLocalPart + "a".repeat(64 - uniqueLocalPart.length())
                + "@" + "d".repeat(22) + ".example.test");
        registration.setPhone("1".repeat(30));
        assertEquals(100, registration.getEmail().length());
        assertTrue(validator.validate(registration).isEmpty());

        registration.setUsername("u".repeat(3));
        registration.setPassword("p".repeat(4));
        assertTrue(validator.validate(registration).isEmpty());
        registration.setUsername("u".repeat(51));
        assertTrue(hasViolation(registration, "username"));
        registration.setUsername("u".repeat(2));
        assertTrue(hasViolation(registration, "username"));
        registration.setUsername("u".repeat(3));
        registration.setPassword("p".repeat(101));
        assertTrue(hasViolation(registration, "password"));
        registration.setPassword("p".repeat(3));
        assertTrue(hasViolation(registration, "password"));
        registration.setPassword("p".repeat(4));
        registration.setEmail(registration.getEmail() + "a");
        assertTrue(hasViolation(registration, "email"));
        registration.setEmail(null);
        registration.setPhone("1".repeat(31));
        assertTrue(hasViolation(registration, "phone"));

        CreateTicketRequestDTO ticket = new CreateTicketRequestDTO(
                activeCatalogItem.getId(),
                "D".repeat(50),
                TicketPriority.MEDIUM,
                "D".repeat(10)
        );
        assertTrue(validator.validate(ticket).isEmpty());
        ticket.setDepartment("D".repeat(51));
        assertTrue(hasViolation(ticket, "department"));
        ticket.setDepartment("D");
        ticket.setDescription("D".repeat(2000));
        assertTrue(validator.validate(ticket).isEmpty());
        ticket.setDescription("D".repeat(2001));
        assertTrue(hasViolation(ticket, "description"));
        ticket.setDescription("D".repeat(9));
        assertTrue(hasViolation(ticket, "description"));
    }

    private boolean hasViolation(Object target, String field) {
        return validator.validate(target).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString()));
    }
}
