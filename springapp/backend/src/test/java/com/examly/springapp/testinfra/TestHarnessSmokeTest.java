package com.examly.springapp.testinfra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Phase 2 Core Test Infrastructure Smoke Test")
public class TestHarnessSmokeTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("INFRA-01: Application context loads and base test beans are injected")
    void testContextLoads() {
        assertNotNull(mockMvc);
        assertNotNull(objectMapper);
        assertNotNull(databaseCleaner);
        assertNotNull(authHelper);
    }

    @Test
    @DisplayName("INFRA-02: Public health endpoint is accessible without authentication")
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("INFRA-03: Mutating endpoint rejects missing or mismatched CSRF tokens (403)")
    void testCsrfRejectionOnMutatingPost() throws Exception {
        // Attempting to post to an authenticated endpoint with valid JWT cookie but mismatched CSRF token
        mockMvc.perform(post("/api/v1/tickets")
                        .with(authHelper.withRole("EMPLOYEE"))
                        .with(authHelper.withMismatchedCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("INFRA-04: Deterministic table cleaner can be invoked repeatedly without constraint failures")
    void testDatabaseCleanerIsolation() {
        databaseCleaner.cleanDatabase();
        databaseCleaner.cleanDatabase();
    }
}
