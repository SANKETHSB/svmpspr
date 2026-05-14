-- SVPMS Database Schema
-- Run: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS svpms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE svpms_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_count INT DEFAULT 0,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    gst_number VARCHAR(15) NOT NULL UNIQUE,
    registration_id VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15),
    address VARCHAR(500),
    contact_person VARCHAR(100),
    status ENUM('PENDING_APPROVAL','APPROVED','REJECTED','SUSPENDED') DEFAULT 'PENDING_APPROVAL',
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_compliant BOOLEAN DEFAULT TRUE,
    performance_score DOUBLE DEFAULT 0.0,
    total_rfqs_won INT DEFAULT 0,
    total_rfqs_participated INT DEFAULT 0,
    rejection_reason VARCHAR(500),
    approved_at DATETIME NULL,
    approved_by BIGINT NULL,
    failed_login_count INT DEFAULT 0,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS vendor_approval_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT NOT NULL,
    reason TEXT,
    performed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    FOREIGN KEY (performed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS compliance_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    is_expired BOOLEAN DEFAULT FALSE,
    expiry_warning_sent BOOLEAN DEFAULT FALSE,
    version INT DEFAULT 1,
    is_latest BOOLEAN DEFAULT TRUE,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    INDEX idx_compliance_vendor (vendor_id),
    INDEX idx_compliance_expiry (expiry_date),
    INDEX idx_compliance_latest (vendor_id, document_type, is_latest)
);

CREATE TABLE IF NOT EXISTS rfqs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_number VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    terms TEXT NOT NULL,
    deadline DATETIME NOT NULL,
    status ENUM('OPEN','CLOSED','AWARDED','ARCHIVED') DEFAULT 'OPEN',
    revision_number INT DEFAULT 1,
    created_by BIGINT NOT NULL,
    awarded_vendor_id BIGINT NULL,
    award_reason VARCHAR(500),
    awarded_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (awarded_vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS rfq_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_id BIGINT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    quantity INT NOT NULL,
    unit VARCHAR(50) NOT NULL,
    specifications TEXT,
    FOREIGN KEY (rfq_id) REFERENCES rfqs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rfq_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rfq_id) REFERENCES rfqs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rfq_revision_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    changed_by BIGINT NOT NULL,
    change_description TEXT,
    old_deadline DATETIME,
    new_deadline DATETIME,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rfq_id) REFERENCES rfqs(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS rfq_vendor_invites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    invited_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    has_responded BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uq_rfq_vendor (rfq_id, vendor_id),
    FOREIGN KEY (rfq_id) REFERENCES rfqs(id),
    FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS quotations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rfq_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    tax_percentage DOUBLE NOT NULL,
    currency VARCHAR(10) NOT NULL,
    delivery_days INT NOT NULL,
    notes TEXT,
    status ENUM('SUBMITTED','UNDER_EVALUATION','AWARDED','REJECTED') DEFAULT 'SUBMITTED',
    weighted_score DOUBLE,
    evaluation_comment TEXT,
    evaluated_by VARCHAR(100),
    evaluated_at DATETIME NULL,
    is_awarded BOOLEAN DEFAULT FALSE,
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rfq_vendor_quote (rfq_id, vendor_id),
    FOREIGN KEY (rfq_id) REFERENCES rfqs(id),
    FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT NOT NULL,
    rfq_item_id BIGINT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE,
    FOREIGN KEY (rfq_item_id) REFERENCES rfq_items(id)
);

CREATE TABLE IF NOT EXISTS quotation_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    checksum VARCHAR(64),
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number VARCHAR(50) NOT NULL UNIQUE,
    rfq_id BIGINT NOT NULL,
    quotation_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    delivery_date DATE NOT NULL,
    shipping_address TEXT,
    payment_terms VARCHAR(255),
    special_instructions TEXT,
    status ENUM('GENERATED','SENT','RECEIVED','CLOSED') DEFAULT 'GENERATED',
    generated_by BIGINT NOT NULL,
    pdf_path VARCHAR(500),
    generated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rfq_id) REFERENCES rfqs(id),
    FOREIGN KEY (quotation_id) REFERENCES quotations(id),
    FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    FOREIGN KEY (generated_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_name VARCHAR(200),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_value TEXT,
    new_value TEXT,
    description TEXT,
    ip_address VARCHAR(45),
    sensitive_data_masked BIT(1) NOT NULL DEFAULT 0,
    prev_hash VARCHAR(64),
    record_hash VARCHAR(64),
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_actor (actor_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_ts (timestamp)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('VENDOR_APPROVED','VENDOR_REJECTED','RFQ_ASSIGNED','RFQ_AWARDED','RFQ_CLOSED','PO_ISSUED','COMPLIANCE_EXPIRY','GENERAL','ACCOUNT_LOCKED') NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    reference_id BIGINT,
    reference_type VARCHAR(100),
    read_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recipient (recipient_id, recipient_type),
    INDEX idx_unread (recipient_id, is_read)
);

-- ============================================================================
-- US 13 AC #10: Notification Preferences Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- USER or VENDOR
    notification_type VARCHAR(50) NOT NULL,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_type_notif (user_id, user_type, notification_type),
    INDEX idx_user_prefs (user_id, user_type)
);

-- ============================================================================
-- US 12 AC #10: Search Optimization Indexes
-- ============================================================================
-- These indexes improve search and filter performance for advanced filtering

-- Vendor search indexes (US 12 AC #1: Search vendors by name, GST, registration ID)
CREATE INDEX idx_vendor_company_name ON vendors(company_name);
CREATE INDEX idx_vendor_gst_number ON vendors(gst_number);
CREATE INDEX idx_vendor_registration_id ON vendors(registration_id);
CREATE INDEX idx_vendor_status ON vendors(status);
CREATE INDEX idx_vendor_compliant ON vendors(is_compliant);

-- RFQ search indexes (US 12 AC #2: Filter RFQs by status)
CREATE INDEX idx_rfq_number ON rfqs(rfq_number);
CREATE INDEX idx_rfq_title ON rfqs(title);
CREATE INDEX idx_rfq_status ON rfqs(status);
CREATE INDEX idx_rfq_deadline ON rfqs(deadline);
CREATE INDEX idx_rfq_created_by ON rfqs(created_by);
CREATE INDEX idx_rfq_created_at ON rfqs(created_at);

-- Purchase Order search indexes (US 12 AC #3: Filter POs by vendor, date range, status)
CREATE INDEX idx_po_number ON purchase_orders(po_number);
CREATE INDEX idx_po_vendor_id ON purchase_orders(vendor_id);
CREATE INDEX idx_po_status ON purchase_orders(status);
CREATE INDEX idx_po_generated_at ON purchase_orders(generated_at);
CREATE INDEX idx_po_rfq_id ON purchase_orders(rfq_id);

-- Quotation search indexes
CREATE INDEX idx_quotation_rfq_id ON quotations(rfq_id);
CREATE INDEX idx_quotation_vendor_id ON quotations(vendor_id);
CREATE INDEX idx_quotation_status ON quotations(status);
CREATE INDEX idx_quotation_amount ON quotations(total_amount);

-- Full-text search indexes for advanced text search (optional, for very large datasets)
-- CREATE FULLTEXT INDEX idx_vendor_search ON vendors(company_name, gst_number, registration_id);
-- CREATE FULLTEXT INDEX idx_rfq_search ON rfqs(title, description, rfq_number);
-- CREATE FULLTEXT INDEX idx_po_search ON purchase_orders(po_number);

-- ============================================================================
-- US-RBAC: Role & Permission Management Tables
-- ============================================================================

-- AC #1,#9: Custom roles (system roles are seeded and protected)
CREATE TABLE IF NOT EXISTS custom_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_system BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_name (name),
    INDEX idx_role_system (is_system)
);

-- AC #2,#3: Module-level CRUD permissions per role
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    module VARCHAR(100) NOT NULL,
    can_create BOOLEAN DEFAULT FALSE,
    can_read BOOLEAN DEFAULT FALSE,
    can_update BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_role_module (role_id, module),
    FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE,
    INDEX idx_rp_role (role_id)
);

-- AC #10: Role assignment history
CREATE TABLE IF NOT EXISTS role_assignment_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(200),
    previous_role VARCHAR(100),
    new_role VARCHAR(100) NOT NULL,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(200),
    reason VARCHAR(500),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rah_user (user_id),
    INDEX idx_rah_actor (actor_id)
);

-- AC #4: Add custom_role_name column to users for granular role assignment
ALTER TABLE users ADD COLUMN IF NOT EXISTS custom_role_name VARCHAR(100) NULL;

-- AC #9: Seed default system roles (protected — cannot be deleted)
INSERT IGNORE INTO custom_roles (name, description, is_system, is_active) VALUES
('ADMIN',               'Full system administrator with all permissions',          TRUE, TRUE),
('PROCUREMENT_MANAGER', 'Manages RFQs, quotations, and purchase orders',           TRUE, TRUE),
('COMPLIANCE_OFFICER',  'Monitors vendor compliance and audit logs',               TRUE, TRUE);

-- AC #2,#3: Seed default permissions for system roles
-- ADMIN: full CRUD on all modules
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, m.module, TRUE, TRUE, TRUE, TRUE
FROM custom_roles r
CROSS JOIN (
    SELECT 'VENDORS' AS module UNION SELECT 'RFQS' UNION SELECT 'QUOTATIONS'
    UNION SELECT 'PURCHASE_ORDERS' UNION SELECT 'USERS' UNION SELECT 'AUDIT_LOGS'
    UNION SELECT 'NOTIFICATIONS' UNION SELECT 'COMPLIANCE' UNION SELECT 'ANALYTICS'
    UNION SELECT 'DASHBOARD' UNION SELECT 'ROLES'
) m
WHERE r.name = 'ADMIN';

-- PROCUREMENT_MANAGER: CRUD on RFQs, Quotations, POs; Read on Vendors/Dashboard
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'RFQS', TRUE, TRUE, TRUE, FALSE FROM custom_roles r WHERE r.name = 'PROCUREMENT_MANAGER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'QUOTATIONS', FALSE, TRUE, TRUE, FALSE FROM custom_roles r WHERE r.name = 'PROCUREMENT_MANAGER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'PURCHASE_ORDERS', TRUE, TRUE, TRUE, FALSE FROM custom_roles r WHERE r.name = 'PROCUREMENT_MANAGER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'VENDORS', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'PROCUREMENT_MANAGER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'DASHBOARD', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'PROCUREMENT_MANAGER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'ANALYTICS', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'PROCUREMENT_MANAGER';

-- COMPLIANCE_OFFICER: Read on Vendors, Compliance, Audit Logs
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'VENDORS', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'COMPLIANCE_OFFICER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'COMPLIANCE', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'COMPLIANCE_OFFICER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'AUDIT_LOGS', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'COMPLIANCE_OFFICER';
INSERT IGNORE INTO role_permissions (role_id, module, can_create, can_read, can_update, can_delete)
SELECT r.id, 'DASHBOARD', FALSE, TRUE, FALSE, FALSE FROM custom_roles r WHERE r.name = 'COMPLIANCE_OFFICER';

-- ============================================================================
-- End of US-RBAC Tables
-- ============================================================================
