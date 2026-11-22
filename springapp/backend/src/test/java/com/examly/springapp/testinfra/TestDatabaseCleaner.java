package com.examly.springapp.testinfra;

import com.examly.springapp.service.AuditChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TestDatabaseCleaner {

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseCleaner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private AuditChainService auditChainService;

    // Deterministic deletion order for foreign key safety across all DB engines
    private static final List<String> TABLES_TO_TRUNCATE = Arrays.asList(
            "in_app_alert",
            "ticket_comment",
            "approval_task",
            "service_ticket",
            "service_catalog",
            "audit_log",
            "audit_chain_head",
            "refresh_token",
            "account_status_log",
            "profile",
            "user_role_mapping",
            "user_account",
            "role"
    );

    public void cleanDatabase() {
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        } catch (Exception ignored) {
            // Some drivers/modes may not support this statement
        }

        for (String table : TABLES_TO_TRUNCATE) {
            try {
                jdbcTemplate.execute("DELETE FROM " + table);
            } catch (Exception e) {
                // Table might not exist yet or have different casing in certain profiles
            }
        }

        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        } catch (Exception ignored) {}

        if (auditChainService != null) {
            try {
                auditChainService.resetChainToGenesis();
            } catch (Exception ignored) {}
        }
    }
}
