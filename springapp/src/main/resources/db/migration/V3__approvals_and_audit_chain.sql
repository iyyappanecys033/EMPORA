-- =============================================================================
-- EPAMS Migration V3: Multi-Level Approvals, Cryptographic Audit Anchor and Alerts
-- Target: MySQL 8.0+ / H2 (Compatibility Mode)
-- Description: Establishes approval workflows, audit chain anchor, and in-app alerts.
-- =============================================================================

CREATE TABLE IF NOT EXISTS approval_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    approval_stage INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_approval_ticket FOREIGN KEY (ticket_id) REFERENCES service_ticket (id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_approver FOREIGN KEY (approver_id) REFERENCES user_account (id),
    CONSTRAINT uk_approval_ticket_stage UNIQUE (ticket_id, approval_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_approval_approver_status ON approval_task (approver_id, status);

CREATE TABLE IF NOT EXISTS audit_chain_head (
    id BIGINT PRIMARY KEY,
    current_hash CHAR(64) NOT NULL,
    last_sequence BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed single-row genesis chain head anchor (Standard ANSI portable insert)
INSERT INTO audit_chain_head (id, current_hash, last_sequence, updated_at) 
SELECT 1, '0000000000000000000000000000000000000000000000000000000000000000', 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM audit_chain_head WHERE id = 1);

-- Extend existing audit_log with cryptographic hash-chain fields (Option A)
-- Preserves all existing legacy audit records (they retain NULL for these columns)
ALTER TABLE audit_log ADD COLUMN sequence_number BIGINT NULL;
ALTER TABLE audit_log ADD COLUMN previous_hash CHAR(64) NULL;
ALTER TABLE audit_log ADD COLUMN current_hash CHAR(64) NULL;

CREATE UNIQUE INDEX uk_audit_log_sequence ON audit_log (sequence_number);
CREATE INDEX idx_audit_log_curr_hash ON audit_log (current_hash);

CREATE TABLE IF NOT EXISTS in_app_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    ticket_id BIGINT NULL,
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_recipient FOREIGN KEY (recipient_id) REFERENCES user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_ticket FOREIGN KEY (ticket_id) REFERENCES service_ticket (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_alert_recipient_read ON in_app_alert (recipient_id, is_read, created_at);
