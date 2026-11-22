-- =============================================================================
-- EPAMS Migration V1: Existing Identity, Access Management and Profile Schema
-- Target: MySQL 8.0+ / H2 (Compatibility Mode)
-- Description: Establishes base IAM tables matching existing production JPA entities.
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    CONSTRAINT uk_user_account_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_role_role_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    role_id BIGINT NULL,
    CONSTRAINT fk_urm_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_urm_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(30) NULL,
    user_id BIGINT NULL,
    first_name VARCHAR(50) NULL,
    last_name VARCHAR(50) NULL,
    address VARCHAR(512) NULL,
    dob DATE NULL,
    gender VARCHAR(20) NULL,
    department VARCHAR(50) NULL,
    designation VARCHAR(100) NULL,
    salary DOUBLE NULL,
    date_of_joining DATE NULL,
    reporting_manager VARCHAR(50) NULL,
    emergency_contact VARCHAR(255) NULL,
    CONSTRAINT uk_profile_employee_id UNIQUE (employee_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_profile_department ON profile (department);
CREATE INDEX idx_profile_reporting_manager ON profile (reporting_manager);

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_username VARCHAR(50) NULL,
    action VARCHAR(50) NULL,
    target_username VARCHAR(50) NULL,
    details VARCHAR(1000) NULL,
    timestamp TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_target_user ON audit_log (target_username);
CREATE INDEX idx_audit_action ON audit_log (action);

CREATE TABLE IF NOT EXISTS account_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
