-- Test Data for Vendor Analytics Dashboard
-- This script creates sample RFQs, Quotations, and Purchase Orders for testing analytics

-- First, get the user ID for manager (assuming it exists)
SET @manager_id = (SELECT id FROM users WHERE email = 'manager@svpms.com' LIMIT 1);

-- Insert test RFQs (spread across different months in 2025-2026)
INSERT INTO rfqs (rfq_number, title, description, terms, deadline, status, created_by, created_at, updated_at) VALUES
('RFQ-2025-001', 'Office Supplies Q1 2025', 'Bulk office supplies for Q1', 'Standard payment terms', '2025-03-15 23:59:59', 'CLOSED', @manager_id, '2025-01-10 10:00:00', '2025-03-16 10:00:00'),
('RFQ-2025-002', 'IT Equipment Feb 2025', 'Laptops and accessories', 'Standard payment terms', '2025-02-28 23:59:59', 'AWARDED', @manager_id, '2025-02-01 10:00:00', '2025-03-01 10:00:00'),
('RFQ-2025-003', 'Furniture March 2025', 'Office furniture procurement', 'Standard payment terms', '2025-03-31 23:59:59', 'AWARDED', @manager_id, '2025-03-05 10:00:00', '2025-04-01 10:00:00'),
('RFQ-2025-004', 'Stationery April 2025', 'Monthly stationery supplies', 'Standard payment terms', '2025-04-30 23:59:59', 'CLOSED', @manager_id, '2025-04-01 10:00:00', '2025-05-01 10:00:00'),
('RFQ-2025-005', 'Electronics May 2025', 'Electronic equipment', 'Standard payment terms', '2025-05-31 23:59:59', 'AWARDED', @manager_id, '2025-05-01 10:00:00', '2025-06-01 10:00:00'),
('RFQ-2025-006', 'Software Licenses June 2025', 'Annual software licenses', 'Standard payment terms', '2025-06-30 23:59:59', 'CLOSED', @manager_id, '2025-06-01 10:00:00', '2025-07-01 10:00:00'),
('RFQ-2025-007', 'Office Supplies Q3 2025', 'Bulk office supplies for Q3', 'Standard payment terms', '2025-09-30 23:59:59', 'AWARDED', @manager_id, '2025-07-10 10:00:00', '2025-10-01 10:00:00'),
('RFQ-2025-008', 'IT Equipment Oct 2025', 'Monitors and peripherals', 'Standard payment terms', '2025-10-31 23:59:59', 'CLOSED', @manager_id, '2025-10-01 10:00:00', '2025-11-01 10:00:00'),
('RFQ-2025-009', 'Furniture Nov 2025', 'Conference room furniture', 'Standard payment terms', '2025-11-30 23:59:59', 'AWARDED', @manager_id, '2025-11-01 10:00:00', '2025-12-01 10:00:00'),
('RFQ-2025-010', 'Year-End Supplies 2025', 'End of year procurement', 'Standard payment terms', '2025-12-31 23:59:59', 'CLOSED', @manager_id, '2025-12-01 10:00:00', '2026-01-02 10:00:00'),
('RFQ-2026-001', 'Q1 2026 Office Supplies', 'First quarter 2026 supplies', 'Standard payment terms', '2026-03-31 23:59:59', 'AWARDED', @manager_id, '2026-01-15 10:00:00', '2026-04-01 10:00:00'),
('RFQ-2026-002', 'IT Refresh April 2026', 'IT equipment refresh', 'Standard payment terms', '2026-04-30 23:59:59', 'OPEN', @manager_id, '2026-04-01 10:00:00', '2026-04-01 10:00:00');

-- Get RFQ IDs
SET @rfq1 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-001');
SET @rfq2 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-002');
SET @rfq3 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-003');
SET @rfq4 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-004');
SET @rfq5 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-005');
SET @rfq6 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-006');
SET @rfq7 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-007');
SET @rfq8 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-008');
SET @rfq9 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-009');
SET @rfq10 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2025-010');
SET @rfq11 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2026-001');
SET @rfq12 = (SELECT id FROM rfqs WHERE rfq_number = 'RFQ-2026-002');

-- Insert RFQ items for each RFQ
INSERT INTO rfq_items (rfq_id, item_name, description, quantity, unit, specifications) VALUES
-- RFQ 1 items
(@rfq1, 'A4 Paper', 'Premium quality A4 paper', 100, 'REAMS', '80 GSM, 500 sheets per ream'),
(@rfq1, 'Pens', 'Blue ballpoint pens', 500, 'PIECES', 'Smooth writing, long-lasting'),
-- RFQ 2 items
(@rfq2, 'Laptops', 'Business laptops', 10, 'PIECES', 'i5 processor, 8GB RAM, 256GB SSD'),
(@rfq2, 'Mouse', 'Wireless mouse', 10, 'PIECES', 'Ergonomic design'),
-- RFQ 3 items
(@rfq3, 'Office Chairs', 'Ergonomic office chairs', 20, 'PIECES', 'Adjustable height, lumbar support'),
(@rfq3, 'Desks', 'Office desks', 15, 'PIECES', '120cm x 60cm'),
-- RFQ 4 items
(@rfq4, 'Notebooks', 'Spiral notebooks', 200, 'PIECES', 'A5 size, 100 pages'),
(@rfq4, 'Markers', 'Whiteboard markers', 50, 'PIECES', 'Assorted colors'),
-- RFQ 5 items
(@rfq5, 'Projectors', 'HD projectors', 5, 'PIECES', '1080p, 3000 lumens'),
(@rfq5, 'Cables', 'HDMI cables', 20, 'PIECES', '2 meter length'),
-- RFQ 6 items
(@rfq6, 'MS Office', 'Microsoft Office licenses', 50, 'LICENSES', 'Office 365 Business'),
-- RFQ 7 items
(@rfq7, 'Paper', 'A4 paper bulk order', 200, 'REAMS', '80 GSM'),
(@rfq7, 'Folders', 'File folders', 300, 'PIECES', 'Plastic, assorted colors'),
-- RFQ 8 items
(@rfq8, 'Monitors', '24-inch monitors', 15, 'PIECES', 'Full HD, IPS panel'),
(@rfq8, 'Keyboards', 'Wireless keyboards', 15, 'PIECES', 'Ergonomic design'),
-- RFQ 9 items
(@rfq9, 'Conference Table', 'Large conference table', 2, 'PIECES', '3m x 1.2m, wooden'),
(@rfq9, 'Chairs', 'Conference chairs', 20, 'PIECES', 'Cushioned, swivel base'),
-- RFQ 10 items
(@rfq10, 'Calendars', '2026 wall calendars', 100, 'PIECES', 'A3 size'),
(@rfq10, 'Diaries', '2026 desk diaries', 50, 'PIECES', 'A5 size'),
-- RFQ 11 items
(@rfq11, 'Staplers', 'Heavy duty staplers', 30, 'PIECES', '50 sheet capacity'),
(@rfq11, 'Paper Clips', 'Metal paper clips', 100, 'BOXES', '100 pieces per box'),
-- RFQ 12 items
(@rfq12, 'Servers', 'Rack servers', 3, 'PIECES', '16GB RAM, 1TB storage');

-- Invite vendors to RFQs (assuming vendor IDs 1, 2, 3 exist)
INSERT INTO rfq_vendor_invites (rfq_id, vendor_id, invited_at) VALUES
-- All vendors invited to all RFQs
(@rfq1, 1, '2025-01-10 11:00:00'), (@rfq1, 2, '2025-01-10 11:00:00'), (@rfq1, 3, '2025-01-10 11:00:00'),
(@rfq2, 1, '2025-02-01 11:00:00'), (@rfq2, 2, '2025-02-01 11:00:00'), (@rfq2, 3, '2025-02-01 11:00:00'),
(@rfq3, 1, '2025-03-05 11:00:00'), (@rfq3, 2, '2025-03-05 11:00:00'), (@rfq3, 3, '2025-03-05 11:00:00'),
(@rfq4, 1, '2025-04-01 11:00:00'), (@rfq4, 2, '2025-04-01 11:00:00'), (@rfq4, 3, '2025-04-01 11:00:00'),
(@rfq5, 1, '2025-05-01 11:00:00'), (@rfq5, 2, '2025-05-01 11:00:00'), (@rfq5, 3, '2025-05-01 11:00:00'),
(@rfq6, 1, '2025-06-01 11:00:00'), (@rfq6, 2, '2025-06-01 11:00:00'), (@rfq6, 3, '2025-06-01 11:00:00'),
(@rfq7, 1, '2025-07-10 11:00:00'), (@rfq7, 2, '2025-07-10 11:00:00'), (@rfq7, 3, '2025-07-10 11:00:00'),
(@rfq8, 1, '2025-10-01 11:00:00'), (@rfq8, 2, '2025-10-01 11:00:00'), (@rfq8, 3, '2025-10-01 11:00:00'),
(@rfq9, 1, '2025-11-01 11:00:00'), (@rfq9, 2, '2025-11-01 11:00:00'), (@rfq9, 3, '2025-11-01 11:00:00'),
(@rfq10, 1, '2025-12-01 11:00:00'), (@rfq10, 2, '2025-12-01 11:00:00'), (@rfq10, 3, '2025-12-01 11:00:00'),
(@rfq11, 1, '2026-01-15 11:00:00'), (@rfq11, 2, '2026-01-15 11:00:00'), (@rfq11, 3, '2026-01-15 11:00:00'),
(@rfq12, 1, '2026-04-01 11:00:00'), (@rfq12, 2, '2026-04-01 11:00:00'), (@rfq12, 3, '2026-04-01 11:00:00');

-- Insert quotations from vendors (Vendor 2 submits most, wins some, loses some)
-- Vendor 2 quotations (our test vendor)
INSERT INTO quotations (rfq_id, vendor_id, total_amount, tax_percentage, currency, delivery_days, notes, status, is_awarded, submitted_at, updated_at) VALUES
(@rfq1, 2, 45000.00, 18.0, 'INR', 15, 'Competitive pricing for bulk order', 'REJECTED', 0, '2025-01-15 14:00:00', '2025-03-16 10:00:00'),
(@rfq2, 2, 550000.00, 18.0, 'INR', 10, 'Premium laptops with warranty', 'AWARDED', 1, '2025-02-10 14:00:00', '2025-03-01 10:00:00'),
(@rfq3, 2, 280000.00, 18.0, 'INR', 20, 'Ergonomic furniture package', 'AWARDED', 1, '2025-03-10 14:00:00', '2025-04-01 10:00:00'),
(@rfq4, 2, 12000.00, 18.0, 'INR', 7, 'Quick delivery available', 'REJECTED', 0, '2025-04-05 14:00:00', '2025-05-01 10:00:00'),
(@rfq5, 2, 95000.00, 18.0, 'INR', 15, 'Latest models with 2-year warranty', 'AWARDED', 1, '2025-05-05 14:00:00', '2025-06-01 10:00:00'),
(@rfq6, 2, 125000.00, 18.0, 'INR', 5, 'Immediate activation', 'REJECTED', 0, '2025-06-05 14:00:00', '2025-07-01 10:00:00'),
(@rfq7, 2, 85000.00, 18.0, 'INR', 12, 'Bulk discount applied', 'AWARDED', 1, '2025-07-15 14:00:00', '2025-10-01 10:00:00'),
(@rfq8, 2, 180000.00, 18.0, 'INR', 10, 'Premium monitors', 'REJECTED', 0, '2025-10-05 14:00:00', '2025-11-01 10:00:00'),
(@rfq9, 2, 320000.00, 18.0, 'INR', 25, 'Custom furniture design', 'AWARDED', 1, '2025-11-05 14:00:00', '2025-12-01 10:00:00'),
(@rfq10, 2, 18000.00, 18.0, 'INR', 10, 'Year-end special pricing', 'REJECTED', 0, '2025-12-05 14:00:00', '2026-01-02 10:00:00'),
(@rfq11, 2, 42000.00, 18.0, 'INR', 15, 'Quality office supplies', 'AWARDED', 1, '2026-01-20 14:00:00', '2026-04-01 10:00:00'),
(@rfq12, 2, 75000.00, 18.0, 'INR', 8, 'Latest IT equipment', 'SUBMITTED', 0, '2026-04-05 14:00:00', '2026-04-05 14:00:00');

-- Vendor 1 quotations (competitor - wins some)
INSERT INTO quotations (rfq_id, vendor_id, total_amount, tax_percentage, currency, delivery_days, notes, status, is_awarded, submitted_at, updated_at) VALUES
(@rfq1, 1, 42000.00, 18.0, 'INR', 12, 'Best quality paper', 'AWARDED', 1, '2025-01-14 14:00:00', '2025-03-16 10:00:00'),
(@rfq2, 1, 580000.00, 18.0, 'INR', 15, 'Standard laptops', 'REJECTED', 0, '2025-02-09 14:00:00', '2025-03-01 10:00:00'),
(@rfq4, 1, 11000.00, 18.0, 'INR', 5, 'Fast delivery', 'AWARDED', 1, '2025-04-04 14:00:00', '2025-05-01 10:00:00'),
(@rfq6, 1, 120000.00, 18.0, 'INR', 3, 'Bulk license discount', 'AWARDED', 1, '2025-06-04 14:00:00', '2025-07-01 10:00:00'),
(@rfq8, 1, 175000.00, 18.0, 'INR', 8, 'Quality monitors', 'AWARDED', 1, '2025-10-04 14:00:00', '2025-11-01 10:00:00'),
(@rfq10, 1, 16000.00, 18.0, 'INR', 7, 'Bulk order discount', 'AWARDED', 1, '2025-12-04 14:00:00', '2026-01-02 10:00:00');

-- Vendor 3 quotations (competitor - wins fewer)
INSERT INTO quotations (rfq_id, vendor_id, total_amount, tax_percentage, currency, delivery_days, notes, status, is_awarded, submitted_at, updated_at) VALUES
(@rfq1, 3, 48000.00, 18.0, 'INR', 20, 'Premium quality', 'REJECTED', 0, '2025-01-16 14:00:00', '2025-03-16 10:00:00'),
(@rfq3, 3, 290000.00, 18.0, 'INR', 25, 'Designer furniture', 'REJECTED', 0, '2025-03-11 14:00:00', '2025-04-01 10:00:00'),
(@rfq5, 3, 98000.00, 18.0, 'INR', 18, 'Standard electronics', 'REJECTED', 0, '2025-05-06 14:00:00', '2025-06-01 10:00:00'),
(@rfq7, 3, 88000.00, 18.0, 'INR', 15, 'Standard supplies', 'REJECTED', 0, '2025-07-16 14:00:00', '2025-10-01 10:00:00'),
(@rfq9, 3, 330000.00, 18.0, 'INR', 30, 'Premium furniture', 'REJECTED', 0, '2025-11-06 14:00:00', '2025-12-01 10:00:00'),
(@rfq11, 3, 45000.00, 18.0, 'INR', 20, 'Standard supplies', 'REJECTED', 0, '2026-01-21 14:00:00', '2026-04-01 10:00:00');

-- Get quotation IDs for Vendor 2's awarded quotations
SET @quot2 = (SELECT id FROM quotations WHERE rfq_id = @rfq2 AND vendor_id = 2);
SET @quot3 = (SELECT id FROM quotations WHERE rfq_id = @rfq3 AND vendor_id = 2);
SET @quot5 = (SELECT id FROM quotations WHERE rfq_id = @rfq5 AND vendor_id = 2);
SET @quot7 = (SELECT id FROM quotations WHERE rfq_id = @rfq7 AND vendor_id = 2);
SET @quot9 = (SELECT id FROM quotations WHERE rfq_id = @rfq9 AND vendor_id = 2);
SET @quot11 = (SELECT id FROM quotations WHERE rfq_id = @rfq11 AND vendor_id = 2);

-- Get RFQ item IDs
SET @item3 = (SELECT id FROM rfq_items WHERE rfq_id = @rfq2 AND item_name = 'Laptops');
SET @item4 = (SELECT id FROM rfq_items WHERE rfq_id = @rfq2 AND item_name = 'Mouse');
SET @item5 = (SELECT id FROM rfq_items WHERE rfq_id = @rfq3 AND item_name = 'Office Chairs');
SET @item6 = (SELECT id FROM rfq_items WHERE rfq_id = @rfq3 AND item_name = 'Desks');

-- Insert quotation items for Vendor 2's awarded quotations
INSERT INTO quotation_items (quotation_id, rfq_item_id, unit_price, total_price) VALUES
-- Quotation for RFQ 2 (Laptops) - WON
(@quot2, @item3, 52000.00, 520000.00),
(@quot2, @item4, 800.00, 8000.00),
-- Quotation for RFQ 3 (Furniture) - WON
(@quot3, @item5, 12000.00, 240000.00),
(@quot3, @item6, 8000.00, 120000.00);

-- Insert Purchase Orders for awarded quotations (Vendor 2)
INSERT INTO purchase_orders (po_number, rfq_id, vendor_id, quotation_id, total_amount, currency, delivery_date, shipping_address, payment_terms, status, generated_by, generated_at) VALUES
('PO-202502-0001', @rfq2, 2, @quot2, 550000.00, 'INR', '2025-03-15', 'Corporate Office, Bangalore', 'Payment within 30 days', 'RECEIVED', @manager_id, '2025-03-02 10:00:00'),
('PO-202504-0001', @rfq3, 2, @quot3, 280000.00, 'INR', '2025-04-25', 'Corporate Office, Bangalore', 'Payment within 30 days', 'RECEIVED', @manager_id, '2025-04-02 10:00:00'),
('PO-202506-0001', @rfq5, 2, @quot5, 95000.00, 'INR', '2025-06-20', 'Corporate Office, Bangalore', 'Payment within 30 days', 'RECEIVED', @manager_id, '2025-06-02 10:00:00'),
('PO-202510-0001', @rfq7, 2, @quot7, 85000.00, 'INR', '2025-10-25', 'Corporate Office, Bangalore', 'Payment within 30 days', 'RECEIVED', @manager_id, '2025-10-02 10:00:00'),
('PO-202512-0001', @rfq9, 2, @quot9, 320000.00, 'INR', '2025-12-30', 'Corporate Office, Bangalore', 'Payment within 30 days', 'RECEIVED', @manager_id, '2025-12-02 10:00:00'),
('PO-202604-0001', @rfq11, 2, @quot11, 42000.00, 'INR', '2026-04-20', 'Corporate Office, Bangalore', 'Payment within 30 days', 'SENT', @manager_id, '2026-04-02 10:00:00');

-- Update vendor performance metrics
UPDATE vendors SET 
    total_rfqs_participated = 12,
    total_rfqs_won = 6,
    performance_score = 75.5
WHERE id = 2;

-- Add evaluation scores to awarded quotations
UPDATE quotations SET weighted_score = 85.5 WHERE id = @quot2;
UPDATE quotations SET weighted_score = 88.0 WHERE id = @quot3;
UPDATE quotations SET weighted_score = 82.5 WHERE id = @quot5;
UPDATE quotations SET weighted_score = 86.0 WHERE id = @quot7;
UPDATE quotations SET weighted_score = 90.0 WHERE id = @quot9;
UPDATE quotations SET weighted_score = 84.0 WHERE id = @quot11;

COMMIT;
