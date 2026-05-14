-- Cleanup existing test data
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM purchase_orders WHERE po_number LIKE 'PO-2025%' OR po_number LIKE 'PO-2026%';
DELETE FROM quotation_items WHERE quotation_id IN (SELECT id FROM quotations WHERE rfq_id IN (SELECT id FROM rfqs WHERE rfq_number LIKE 'RFQ-2025%' OR rfq_number LIKE 'RFQ-2026%'));
DELETE FROM quotations WHERE rfq_id IN (SELECT id FROM rfqs WHERE rfq_number LIKE 'RFQ-2025%' OR rfq_number LIKE 'RFQ-2026%');
DELETE FROM rfq_vendor_invites WHERE rfq_id IN (SELECT id FROM rfqs WHERE rfq_number LIKE 'RFQ-2025%' OR rfq_number LIKE 'RFQ-2026%');
DELETE FROM rfq_items WHERE rfq_id IN (SELECT id FROM rfqs WHERE rfq_number LIKE 'RFQ-2025%' OR rfq_number LIKE 'RFQ-2026%');
DELETE FROM rfqs WHERE rfq_number LIKE 'RFQ-2025%' OR rfq_number LIKE 'RFQ-2026%';

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;
