-- ============================================================================
-- EMPORA - Production MySQL Least-Privilege User Provisioning Script
-- [A02: Security Misconfiguration]
-- 
-- Enforces segregation of duties between DDL (Schema Migrations) and 
-- runtime DML (Application Data Mutations).
-- Prohibits runtime app user from DROP, ALTER, GRANT, or administrative privileges.
-- ============================================================================

-- 1. Create dedicated production database with UTF8MB4
CREATE DATABASE IF NOT EXISTS empora_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 2. Create Migration User (Used exclusively by Flyway in CI/CD pipeline)
CREATE USER IF NOT EXISTS 'empora_flyway'@'%' 
    IDENTIFIED BY 'ChangeMeFlywaySecurePassword2026!';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES 
    ON empora_db.* 
    TO 'empora_flyway'@'%'
    REQUIRE SSL;

-- 3. Create Runtime Application User (Used by Spring Boot at runtime)
-- STRICT LEAST PRIVILEGE: No DROP, No ALTER, No GRANT, No FILE, No SUPER.
CREATE USER IF NOT EXISTS 'empora_app'@'%' 
    IDENTIFIED BY 'ChangeMeAppRuntimeSecurePassword2026!';

GRANT SELECT, INSERT, UPDATE, DELETE 
    ON empora_db.* 
    TO 'empora_app'@'%'
    REQUIRE SSL;

-- 4. Apply privileges
FLUSH PRIVILEGES;

-- Verification Queries:
-- SHOW GRANTS FOR 'empora_app'@'%';
-- SHOW GRANTS FOR 'empora_flyway'@'%';
