# US 11: Automatic RFQ Closure - Testing Guide

## Quick Test Scenarios

### 1. Test Automatic Closure (AC #1, #2, #5, #11)

**Setup:**

```sql
-- Create a test RFQ with deadline in the past
INSERT INTO rfq (rfq_number, title, description, terms, deadline, status, created_by_id, created_at, updated_at, revision_number)
VALUES ('RFQ-TEST-001', 'Test Expired RFQ', 'Test description', 'Test terms',
        '2026-05-11 10:00:00', 'OPEN', 1, NOW(), NOW(), 1);
```

**Trigger Scheduler Manually:**

```bash
# Wait for next hourly execution OR
# Trigger manually via Spring Boot Actuator (if enabled)
curl -X POST http://localhost:8081/api/actuator/scheduledtasks
```

**Verify:**

```sql
-- Check RFQ status changed to CLOSED
SELECT id, rfq_number, status, deadline, updated_at
FROM rfq
WHERE rfq_number = 'RFQ-TEST-001';

-- Check audit log created
SELECT * FROM audit_logs
WHERE action = 'RFQ_AUTO_CLOSED'
AND entity_id = (SELECT id FROM rfq WHERE rfq_number = 'RFQ-TEST-001')
ORDER BY created_at DESC;

-- Check notifications sent
SELECT * FROM notifications
WHERE entity_type = 'RFQ'
AND entity_id = (SELECT id FROM rfq WHERE rfq_number = 'RFQ-TEST-001')
AND notification_type = 'RFQ_CLOSED'
ORDER BY created_at DESC;
```

**Expected Results:**

- ✅ RFQ status = CLOSED
- ✅ Audit log entry with action = 'RFQ_AUTO_CLOSED'
- ✅ Notifications sent to Procurement Managers
- ✅ Notifications sent to invited vendors
- ✅ Log messages in application logs

---

### 2. Test Late Submission Blocked (AC #3)

**API Request:**

```bash
# Try to submit quotation to closed RFQ
curl -X POST http://localhost:8081/api/quotations \
  -H "Authorization: Bearer {vendor_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "rfqId": 123,
    "totalAmount": 50000,
    "taxPercentage": 18,
    "currency": "INR",
    "deliveryDays": 30,
    "notes": "Test quotation",
    "items": [
      {
        "rfqItemId": 1,
        "unitPrice": 1000,
        "quantity": 50,
        "notes": "Test item"
      }
    ]
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Cannot submit quotation. RFQ 'Test Expired RFQ' is CLOSED. The submission deadline (2026-05-11T10:00:00) has passed and the RFQ was automatically closed. Late submissions are not accepted to ensure fairness and compliance. If you believe this closure was premature, please contact the procurement team."
}
```

---

### 3. Test Visual Indicator (AC #4)

**Steps:**

1. Login as Vendor or Manager
2. Navigate to RFQ List page
3. Find a CLOSED RFQ

**Expected UI:**

- ✅ Lock icon (🔒) displayed next to RFQ title
- ✅ Red text: "Closed - No submissions accepted"
- ✅ Deadline column shows "CLOSED" in red with lock icon
- ✅ Status badge shows "CLOSED"
- ✅ "Quote" button not displayed for vendors

---

### 4. Test Admin Reopen (AC #6)

**API Request:**

```bash
# Reopen closed RFQ (requires ADMIN role)
curl -X PATCH http://localhost:8081/api/rfqs/123/reopen \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "newDeadline": "2026-06-01T23:59:59",
    "reason": "Insufficient quotations received. Extending deadline to allow more vendor participation and ensure competitive pricing."
  }'
```

**Expected Response (200 OK):**

```json
{
  "success": true,
  "message": "RFQ reopened successfully. Vendors have been notified.",
  "data": {
    "id": 123,
    "rfqNumber": "RFQ-202605-1001",
    "status": "OPEN",
    "deadline": "2026-06-01T23:59:59",
    "revisionNumber": 2,
    ...
  }
}
```

**Verify:**

```sql
-- Check RFQ status changed to OPEN
SELECT id, rfq_number, status, deadline, revision_number
FROM rfq
WHERE id = 123;

-- Check audit log for admin override
SELECT * FROM audit_logs
WHERE action = 'RFQ_REOPENED_ADMIN_OVERRIDE'
AND entity_id = 123
ORDER BY created_at DESC;

-- Check revision history
SELECT * FROM rfq_revision_history
WHERE rfq_id = 123
ORDER BY revision_number DESC;

-- Check notifications sent to vendors
SELECT * FROM notifications
WHERE entity_type = 'RFQ'
AND entity_id = 123
AND message LIKE '%reopened%'
ORDER BY created_at DESC;
```

---

### 5. Test Prevent Editing Closed RFQ (AC #7)

**API Request:**

```bash
# Try to update closed RFQ
curl -X PUT http://localhost:8081/api/rfqs/123 \
  -H "Authorization: Bearer {manager_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Title",
    "description": "Updated description",
    "terms": "Updated terms",
    "deadline": "2026-06-15T23:59:59",
    "items": [...],
    "invitedVendorIds": [1, 2, 3]
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Cannot edit RFQ after it has been closed. RFQ was closed on 2026-05-12T10:30:00. Contact an administrator if you need to reopen this RFQ."
}
```

---

### 6. Test Grace Period (AC #10)

**Configuration:**

```properties
# Set 30-minute grace period in application.properties
rfq.closure.grace-period-minutes=30
```

**Setup:**

```sql
-- Create RFQ with deadline 15 minutes ago
INSERT INTO rfq (rfq_number, title, description, terms, deadline, status, created_by_id, created_at, updated_at, revision_number)
VALUES ('RFQ-TEST-GRACE', 'Test Grace Period', 'Test description', 'Test terms',
        DATE_SUB(NOW(), INTERVAL 15 MINUTE), 'OPEN', 1, NOW(), NOW(), 1);
```

**Trigger Scheduler:**

```bash
# Wait for scheduler execution
```

**Expected Results:**

- ✅ RFQ remains OPEN (within 30-minute grace period)
- ✅ After 30 minutes from deadline, RFQ closes automatically
- ✅ Log shows: "Grace period: 30 minutes (from configuration)"

---

### 7. Test Scheduler Error Handling (AC #12)

**Simulate Error:**

```sql
-- Temporarily break database connection or
-- Create invalid data that causes exception
```

**Expected Results:**

- ✅ Error logged at ERROR level
- ✅ Stack trace captured in logs
- ✅ Audit log created with action = 'RFQ_AUTO_CLOSE_SCHEDULER_FAILED'
- ✅ Scheduler continues running (doesn't crash)
- ✅ Next execution proceeds normally

**Check Logs:**

```bash
# Search for error logs
grep "US 11 SCHEDULER.*FAILED" application.log

# Expected log format:
# [US 11 SCHEDULER] RFQ auto-closure job FAILED. Start: {time}, Failure: {time}, Duration: {seconds} seconds. Error: {error}
```

---

## Integration Test Scenarios

### Scenario 1: Complete Lifecycle

1. Create RFQ with deadline 1 hour in future
2. Invite 3 vendors
3. 2 vendors submit quotations
4. Wait for deadline to pass
5. Scheduler runs and closes RFQ
6. Verify all vendors notified
7. Verify audit logs created
8. Try to submit late quotation (should fail)
9. Admin reopens RFQ with new deadline
10. Vendor submits quotation successfully

### Scenario 2: Multiple RFQs Closure

1. Create 10 RFQs with past deadlines
2. Trigger scheduler
3. Verify all 10 RFQs closed
4. Verify 10 audit logs created
5. Verify notifications sent to all stakeholders
6. Check scheduler logs for summary

### Scenario 3: Concurrent Operations

1. RFQ deadline passes
2. Vendor attempts submission (should fail)
3. Scheduler runs simultaneously
4. Verify no race conditions
5. Verify data consistency

---

## Performance Testing

### Load Test: Bulk Closure

```sql
-- Create 1000 expired RFQs
INSERT INTO rfq (rfq_number, title, description, terms, deadline, status, created_by_id, created_at, updated_at, revision_number)
SELECT
  CONCAT('RFQ-LOAD-', LPAD(seq, 4, '0')),
  CONCAT('Load Test RFQ ', seq),
  'Load test description',
  'Load test terms',
  DATE_SUB(NOW(), INTERVAL 1 HOUR),
  'OPEN',
  1,
  NOW(),
  NOW(),
  1
FROM (
  SELECT @row := @row + 1 AS seq
  FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) t1,
       (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) t2,
       (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) t3,
       (SELECT @row := 0) r
  LIMIT 1000
) numbers;
```

**Measure:**

- Execution time
- Database query performance
- Memory usage
- Log file size

**Expected:**

- < 30 seconds for 1000 RFQs
- No memory leaks
- No database deadlocks

---

## Monitoring Queries

### Dashboard Queries

```sql
-- Count RFQs by status
SELECT status, COUNT(*) as count
FROM rfq
GROUP BY status;

-- Recently closed RFQs
SELECT rfq_number, title, deadline, updated_at
FROM rfq
WHERE status = 'CLOSED'
ORDER BY updated_at DESC
LIMIT 10;

-- RFQs closed today
SELECT COUNT(*) as closed_today
FROM rfq
WHERE status = 'CLOSED'
AND DATE(updated_at) = CURDATE();

-- Scheduler execution history
SELECT action, created_at, details
FROM audit_logs
WHERE action IN ('RFQ_AUTO_CLOSED', 'RFQ_AUTO_CLOSE_SCHEDULER_FAILED')
ORDER BY created_at DESC
LIMIT 20;
```

---

## Troubleshooting

### Issue: RFQs Not Closing Automatically

**Check:**

1. Scheduler enabled: `@EnableScheduling` in main application class
2. Cron expression valid: `scheduler.rfq.close-cron=0 0 * * * *`
3. Database query returns expired RFQs
4. No exceptions in logs

**Debug Query:**

```sql
-- Find RFQs that should be closed
SELECT id, rfq_number, status, deadline
FROM rfq
WHERE status = 'OPEN'
AND deadline < NOW()
ORDER BY deadline DESC;
```

### Issue: Late Submissions Still Accepted

**Check:**

1. Quotation validation logic in `QuotationServiceImpl.submit()`
2. RFQ status in database
3. Server time vs. deadline comparison
4. Exception handling not swallowing errors

### Issue: Admin Cannot Reopen RFQ

**Check:**

1. User has ADMIN role
2. RFQ status is CLOSED (not AWARDED or ARCHIVED)
3. New deadline is in future
4. Reason provided (min 20 characters)
5. JWT token valid and not expired

---

## Test Data Cleanup

```sql
-- Clean up test RFQs
DELETE FROM rfq WHERE rfq_number LIKE 'RFQ-TEST-%';
DELETE FROM rfq WHERE rfq_number LIKE 'RFQ-LOAD-%';

-- Clean up test audit logs
DELETE FROM audit_logs WHERE details LIKE '%TEST%';

-- Clean up test notifications
DELETE FROM notifications WHERE message LIKE '%Test%';
```

---

## Automated Test Script

```bash
#!/bin/bash

echo "US 11 Automated Test Suite"
echo "=========================="

# Test 1: Create expired RFQ
echo "Test 1: Creating expired RFQ..."
curl -X POST http://localhost:8081/api/rfqs \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d @test-expired-rfq.json

# Test 2: Wait for scheduler
echo "Test 2: Waiting for scheduler execution..."
sleep 3600  # Wait 1 hour

# Test 3: Verify closure
echo "Test 3: Verifying RFQ closed..."
curl -X GET http://localhost:8081/api/rfqs/123 \
  -H "Authorization: Bearer $MANAGER_TOKEN"

# Test 4: Try late submission
echo "Test 4: Attempting late submission..."
curl -X POST http://localhost:8081/api/quotations \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d @test-quotation.json

# Test 5: Admin reopen
echo "Test 5: Admin reopening RFQ..."
curl -X PATCH http://localhost:8081/api/rfqs/123/reopen \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newDeadline": "2026-06-01T23:59:59",
    "reason": "Automated test: Reopening for additional vendor participation"
  }'

echo "Test suite completed!"
```

---

## Success Criteria Checklist

- [ ] Scheduler runs hourly without errors
- [ ] Expired RFQs automatically close
- [ ] Late submissions return HTTP 400
- [ ] Visual indicators display correctly
- [ ] Audit logs created for all closures
- [ ] Admin can reopen closed RFQs
- [ ] Closed RFQs cannot be edited
- [ ] Dashboard reflects closures immediately
- [ ] Server-side time used for validation
- [ ] Grace period configuration works
- [ ] Notifications sent to all stakeholders
- [ ] Scheduler failures logged properly

---

**Last Updated:** May 12, 2026  
**Test Coverage:** All 12 acceptance criteria  
**Status:** Ready for QA Testing
