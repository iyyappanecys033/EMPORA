-- =============================================================================
-- EPAMS Migration V4: Notification Lifecycle Metadata & Analytics Indexes
-- Target: MySQL 8.0+ / H2 (Compatibility Mode)
-- Description: Strictly additive schema extension for P8 Notification management
--              (read_at, related entity routing, deduplication) and composite
--              indexes optimizing operational analytics & notification queries.
-- Backward Compatibility: 100% additive; all new columns are NULLABLE and all
--                         existing IAM, ticket, approval, alert, and audit rows
--                         are preserved intact.
-- =============================================================================

-- 1. Extend in_app_alert with read timestamp, deep-link entity reference, and deduplication key
ALTER TABLE in_app_alert ADD COLUMN read_at TIMESTAMP NULL;
ALTER TABLE in_app_alert ADD COLUMN related_entity_type VARCHAR(50) NULL;
ALTER TABLE in_app_alert ADD COLUMN related_entity_id BIGINT NULL;
ALTER TABLE in_app_alert ADD COLUMN dedup_key VARCHAR(128) NULL;

-- 2. Notification query & duplicate-suppression indexes
CREATE INDEX idx_alert_recipient_type ON in_app_alert (recipient_id, alert_type, created_at);
CREATE UNIQUE INDEX uk_alert_recipient_dedup ON in_app_alert (recipient_id, dedup_key);

-- 3. Operational Analytics composite indexes (avoid full-table scans on date/department/priority aggregations)
CREATE INDEX idx_ticket_created_dept_status ON service_ticket (created_at, department, status);
CREATE INDEX idx_ticket_priority_dept ON service_ticket (priority, department);
CREATE INDEX idx_ticket_catalog_created ON service_ticket (catalog_item_id, created_at);
CREATE INDEX idx_approval_status_created ON approval_task (status, created_at);

