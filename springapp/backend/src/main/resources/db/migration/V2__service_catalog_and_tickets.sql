-- =============================================================================
-- EPAMS Migration V2: Service Catalog, 11-State Tickets and Contextual Comments
-- Target: MySQL 8.0+ / H2 (Compatibility Mode)
-- Description: Establishes ticketing and catalog foundations.
-- =============================================================================

CREATE TABLE IF NOT EXISTS service_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_code VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    estimated_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    sla_target_hours INT NOT NULL DEFAULT 24,
    requires_manager_approval BOOLEAN NOT NULL DEFAULT TRUE,
    requires_secondary_approval BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_service_catalog_item_code UNIQUE (item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_catalog_department ON service_catalog (department);
CREATE INDEX idx_catalog_category ON service_catalog (category);

CREATE TABLE IF NOT EXISTS service_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(32) NOT NULL,
    catalog_item_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    assignee_id BIGINT NULL,
    department VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    sla_target_date TIMESTAMP NOT NULL,
    total_paused_minutes INT NOT NULL DEFAULT 0,
    last_paused_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_ticket_number UNIQUE (ticket_number),
    CONSTRAINT fk_ticket_catalog FOREIGN KEY (catalog_item_id) REFERENCES service_catalog (id),
    CONSTRAINT fk_ticket_requester FOREIGN KEY (requester_id) REFERENCES user_account (id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ticket_dept_status ON service_ticket (department, status);
CREATE INDEX idx_ticket_requester_created ON service_ticket (requester_id, created_at);
CREATE INDEX idx_ticket_status_sla ON service_ticket (status, sla_target_date);
CREATE INDEX idx_ticket_resolved_status ON service_ticket (status, resolved_at);

CREATE TABLE IF NOT EXISTS ticket_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    comment_text TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES service_ticket (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_comment_ticket_created ON ticket_comment (ticket_id, created_at);
