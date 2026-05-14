-- ============================================================================
-- US 12 AC #10: Database Indexes for Performance Optimization
-- ============================================================================
-- These indexes optimize search and filter queries for vendors, RFQs, and POs
-- Execute this script after initial schema creation

-- Vendor table indexes
-- US 12 AC #1: Search vendors by name, GST, registration ID
CREATE INDEX IF NOT EXISTS idx_vendor_company_name ON vendor(company_name);
CREATE INDEX IF NOT EXISTS idx_vendor_gst_number ON gst_number);
CREATE INDEX IF NOT EXISTS idx_vendor_registration_id ON vendor(registration_id);
CREATE INDEX IF NOT EXISTS idx_vendor_status ON vendor(status);
CREATE INDEX IF NOT EXISTS idx_vendor_compliant ON vendor(compliant);
CREATE INDEX IF NOT EXISTS idx_vendor_registered_at ON vendor(registered_at);

-- Composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_vendor_status_compliant ON vendor(status, compliant);

-- RFQ table indexes
-- US 12 AC #2: Filter RFQs by status, deadline, creator
CREATE INDEX IF NOT EXISTS idx_rfq_title ON rfq(title);
CREATE INDEX IF NOT EXISTS idx_rfq_number ON rfq(rfq_number);
CREATE INDEX IF NOT EXISTS idx_rfq_status ON rfq(status);
CREATE INDEX IF NOT EXISTS idx_rfq_deadline ON rfq(deadline);
CREATE INDEX IF NOT EXISTS idx_rfq_created_by_id ON rfq(created_by_id);
CREATE INDEX IF NOT EXISTS idx_rfq_created_at ON rfq(created_at);

-- Composite indexes for common filter combinations
CREATE INDEX IF NOT EXISTS idx_rfq_status_deadline ON rfq(status, deadline);
CREATE INDEX IF NOT EXISTS idx_rfq_status_created_at ON rfq(status, created_at);

-- Purchase Order table indexes
-- US 12 AC #3: Filter POs by vendor, date range, status
CREATE INDEX IF NOT EXISTS idx_po_number ON purchase_order(po_number);
CREATE INDEX IF NOT EXISTS idx_po_vendor_id ON purchase_order(vendor_id);
CREATE INDEX IF NOT EXISTS idx_po_status ON purchase_order(status);
CREATE INDEX IF NOT EXISTS idx_po_generated_at ON purchase_order(generated_at);
CREATE INDEX IF NOT EXISTS idx_po_rfq_id ON purchase_order(rfq_id);

-- Composite indexes for common filter combinations
CREATE INDEX IF NOT EXISTS idx_po_vendor_status ON purchase_order(vendor_id, status);
CREATE INDEX IF NOT EXISTS idx_po_status_generated_at ON purchase_order(status, generated_at);
CREATE INDEX IF NOT EXISTS idx_po_vendor_generated_at ON purchase_order(vendor_id, generated_at);

-- Additional indexes for related tables
CREATE INDEX IF NOT EXISTS idx_quotation_rfq_id ON quotation(rfq_id);
CREATE INDEX IF NOT EXISTS idx_quotation_vendor_id ON quotation(vendor_id);
CREATE INDEX IF NOT EXISTS idx_quotation_status ON quotation(status);

CREATE INDEX IF NOT EXISTS idx_rfq_vendor_invite_rfq_id ON rfq_vendor_invite(rfq_id);
CREATE INDEX IF NOT EXISTS idx_rfq_vendor_invite_vendor_id ON rfq_vendor_invite(vendor_id);

-- Full-text search indexes (MySQL specific)
-- Uncomment if using MySQL and want full-text search capabilities
-- ALTER TABLE vendor ADD FULLTEXT INDEX ft_vendor_search (company_name, gst_number, registration_id);
-- ALTER TABLE rfq ADD FULLTEXT INDEX ft_rfq_search (title, description, rfq_number);
-- ALTER TABLE purchase_order ADD FULLTEXT INDEX ft_po_search (po_number);

-- Performance notes:
-- 1. These indexes improve SELECT query performance but slightly slow down INSERT/UPDATE
-- 2. Indexes are automatically used by the query optimizer when appropriate
-- 3. Monitor index usage with: SHOW INDEX FROM table_name;
-- 4. Check query performance with: EXPLAIN SELECT ...
-- 5. Rebuild indexes periodically: OPTIMIZE TABLE table_name;
