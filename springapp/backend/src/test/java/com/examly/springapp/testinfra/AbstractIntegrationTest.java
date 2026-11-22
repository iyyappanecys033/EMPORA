package com.examly.springapp.testinfra;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    // Singleton MySQL Testcontainer: starts once per test run if Docker daemon is available
    protected static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MySQLContainer<?> container = null;
        boolean enableTestcontainers = Boolean.parseBoolean(System.getProperty("testcontainers.enabled",
                System.getenv().getOrDefault("TESTCONTAINERS_ENABLED", "false")));
        if (enableTestcontainers) {
            try {
                if (DockerClientFactory.instance().isDockerAvailable()) {
                    log.info("Docker environment detected. Booting MySQL 8.0.45 Testcontainer...");
                    container = new MySQLContainer<>("mysql:8.0.45")
                            .withDatabaseName("empora_test_db")
                            .withUsername("test_user")
                            .withPassword("test_pass")
                            .withReuse(true);
                    container.start();
                    log.info("MySQL Testcontainer successfully started at {}", container.getJdbcUrl());
                } else {
                    log.info("Docker daemon not running; tests running against isolated test profile schema.");
                }
            } catch (Throwable t) {
                log.warn("Docker unavailable or failed to initialize Testcontainers: {}. Running with test profile datasource.", t.getMessage());
                container = null;
            }
        } else {
            log.info("Running tests against isolated test profile schema (MySQL mode).");
        }
        MYSQL_CONTAINER = container;
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        if (MYSQL_CONTAINER != null && MYSQL_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
            registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", MYSQL_CONTAINER::getDriverClassName);
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDatabaseCleaner databaseCleaner;

    @Autowired
    protected AuthCookieCsrfHelper authHelper;

    @BeforeEach
    void resetStateBeforeEach() {
        databaseCleaner.cleanDatabase();
        seedBaseTestData();
    }

    /**
     * Seeds base roles and admin user required by standard test assertions.
     */
    protected void seedBaseTestData() {
        authHelper.getOrCreateUser("admin", "ADMIN", "Executive", "Board", 150000.0);
    }
}
