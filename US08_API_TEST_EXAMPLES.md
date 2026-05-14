# US 08: RFQ Award API Test Examples

## Test Scenarios with cURL Commands

### Scenario 1: Successful RFQ Award ✅

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "Best value for money with excellent delivery timeline and competitive pricing. Vendor has proven track record.",
    "confirmed": true
  }'
```

**Expected Response (200 OK):**

```json
{
  "success": true,
  "message": "RFQ awarded successfully. Purchase Order generation is now enabled.",
  "data": {
    "id": 1,
    "rfqNumber": "RFQ-202605-1001",
    "title": "Office Supplies Procurement",
    "description": "Procurement of office supplies for Q2 2026",
    "status": "AWARDED",
    "awardedVendorId": 3,
    "awardedVendorName": "ABC Supplies Ltd",
    "awardReason": "Best value for money with excellent delivery timeline and competitive pricing. Vendor has proven track record.",
    "awardedAt": "2026-05-11T10:30:00",
    "createdById": 2,
    "createdByName": "John Manager",
    "quotationCount": 4
  }
}
```

---

### Scenario 2: Award Without Confirmation ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "Best value for money",
    "confirmed": false
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Award confirmation is required. Please confirm the award action.",
  "data": null
}
```

---

### Scenario 3: Award Without Reason ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "",
    "confirmed": true
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Award reason is mandatory",
  "data": null
}
```

---

### Scenario 4: Award with Short Reason ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "Best",
    "confirmed": true
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Award reason must be at least 10 characters for meaningful documentation.",
  "data": null
}
```

---

### Scenario 5: Duplicate Award Attempt ❌

```bash
# First award succeeds
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "Best value for money with excellent delivery timeline",
    "confirmed": true
  }'

# Second award attempt fails
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 6,
    "awardReason": "Better pricing than previous selection",
    "confirmed": true
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "RFQ has already been awarded to ABC Supplies Ltd on 2026-05-11T10:30:00. Multiple awards are not allowed.",
  "data": null
}
```

---

### Scenario 6: Award to Non-Existent Quotation ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 99999,
    "awardReason": "Best value for money with excellent delivery timeline",
    "confirmed": true
  }'
```

**Expected Response (404 Not Found):**

```json
{
  "success": false,
  "message": "Quotation not found with id: 99999",
  "data": null
}
```

---

### Scenario 7: Award to Quotation from Different RFQ ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 15,
    "awardReason": "Best value for money with excellent delivery timeline",
    "confirmed": true
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "The selected quotation does not belong to this RFQ.",
  "data": null
}
```

---

### Scenario 8: Award by Non-Manager Role ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <VENDOR_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "Best value for money with excellent delivery timeline",
    "confirmed": true
  }'
```

**Expected Response (403 Forbidden):**

```json
{
  "success": false,
  "message": "Access Denied",
  "data": null
}
```

---

### Scenario 9: Award to Non-Approved Vendor ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 7,
    "awardReason": "Best value for money with excellent delivery timeline",
    "confirmed": true
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Cannot award to a vendor that is not approved. Vendor status: PENDING_APPROVAL",
  "data": null
}
```

---

### Scenario 10: Award to Non-Compliant Vendor ❌

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 8,
    "awardReason": "Best value for money with excellent delivery timeline",
    "confirmed": true
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Cannot award to a non-compliant vendor. Please ensure vendor compliance documents are valid.",
  "data": null
}
```

---

## Verification Queries

### Check RFQ Status After Award

```bash
curl -X GET http://localhost:8080/rfqs/1 \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>"
```

**Expected Fields:**

- `status`: "AWARDED"
- `awardedVendorId`: [vendor_id]
- `awardedVendorName`: [vendor_name]
- `awardReason`: [reason_text]
- `awardedAt`: [timestamp]

---

### Check Quotation Status After Award

```bash
curl -X GET http://localhost:8080/quotations/5 \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>"
```

**Expected Fields:**

- `status`: "AWARDED"
- `isAwarded`: true

---

### Check Vendor Performance After Award

```bash
curl -X GET http://localhost:8080/vendors/3 \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

**Expected Fields:**

- `totalRfqsWon`: [incremented by 1]
- `performanceScore`: [updated based on win rate]

---

### Check Audit Logs

```bash
curl -X GET "http://localhost:8080/audit-logs?entityType=RFQ&entityId=1&action=RFQ_AWARDED" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

**Expected Response:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "actorId": 2,
        "actorType": "USER",
        "actorName": "John Manager",
        "action": "RFQ_AWARDED",
        "entityType": "RFQ",
        "entityId": 1,
        "description": "RFQ RFQ-202605-1001 awarded to vendor: ABC Supplies Ltd (ID: 3). Quotation ID: 5. Award Amount: 50000.00 USD. Award Reason: Best value for money with excellent delivery timeline. Awarded At: 2026-05-11T10:30:00. Other quotations rejected: 3",
        "timestamp": "2026-05-11T10:30:00"
      }
    ]
  }
}
```

---

### Check Notifications

```bash
# Check winning vendor notifications
curl -X GET "http://localhost:8080/notifications?recipientId=3&recipientType=VENDOR" \
  -H "Authorization: Bearer <VENDOR_JWT_TOKEN>"

# Check non-selected vendor notifications
curl -X GET "http://localhost:8080/notifications?recipientId=4&recipientType=VENDOR" \
  -H "Authorization: Bearer <VENDOR_JWT_TOKEN>"
```

---

## Complete Test Flow

### Step 1: Create RFQ

```bash
curl -X POST http://localhost:8080/rfqs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "title": "Office Supplies Procurement",
    "description": "Procurement of office supplies for Q2 2026",
    "terms": "Payment within 30 days, delivery within 15 days",
    "deadline": "2026-06-01T23:59:59",
    "items": [
      {
        "itemName": "A4 Paper",
        "description": "White A4 paper, 80gsm",
        "quantity": 100,
        "unit": "Reams",
        "specifications": "ISO certified"
      }
    ],
    "invitedVendorIds": [3, 4, 5, 6]
  }'
```

### Step 2: Vendors Submit Quotations

```bash
# Vendor 1 submits
curl -X POST http://localhost:8080/quotations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <VENDOR1_JWT_TOKEN>" \
  -d '{
    "rfqId": 1,
    "totalAmount": 50000,
    "taxPercentage": 18,
    "currency": "USD",
    "deliveryDays": 10,
    "notes": "Best quality products",
    "items": [...]
  }'

# Vendor 2 submits
curl -X POST http://localhost:8080/quotations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <VENDOR2_JWT_TOKEN>" \
  -d '{
    "rfqId": 1,
    "totalAmount": 48000,
    "taxPercentage": 18,
    "currency": "USD",
    "deliveryDays": 12,
    "notes": "Competitive pricing",
    "items": [...]
  }'
```

### Step 3: Evaluate Quotations (Optional)

```bash
curl -X PATCH http://localhost:8080/quotations/5/evaluate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "weightedScore": 85.5,
    "evaluationComment": "Excellent pricing and delivery timeline"
  }'
```

### Step 4: Award RFQ

```bash
curl -X PATCH http://localhost:8080/rfqs/1/award \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "quotationId": 5,
    "awardReason": "Best value for money with excellent delivery timeline and competitive pricing. Vendor has proven track record and meets all compliance requirements.",
    "confirmed": true
  }'
```

### Step 5: Verify Award

```bash
# Check RFQ status
curl -X GET http://localhost:8080/rfqs/1 \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>"

# Check audit logs
curl -X GET "http://localhost:8080/audit-logs?entityType=RFQ&entityId=1" \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"

# Check notifications
curl -X GET "http://localhost:8080/notifications?recipientId=3&recipientType=VENDOR" \
  -H "Authorization: Bearer <VENDOR_JWT_TOKEN>"
```

### Step 6: Generate Purchase Order

```bash
curl -X POST http://localhost:8080/purchase-orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MANAGER_JWT_TOKEN>" \
  -d '{
    "rfqId": 1,
    "quotationId": 5,
    "deliveryDate": "2026-05-25",
    "shippingAddress": "123 Main St, City, Country",
    "paymentTerms": "Net 30",
    "specialInstructions": "Handle with care"
  }'
```

---

## Postman Collection

Import this JSON into Postman for easy testing:

```json
{
  "info": {
    "name": "US 08 - RFQ Award Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Successful Award",
      "request": {
        "method": "PATCH",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{manager_token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"quotationId\": 5,\n  \"awardReason\": \"Best value for money with excellent delivery timeline and competitive pricing. Vendor has proven track record.\",\n  \"confirmed\": true\n}"
        },
        "url": {
          "raw": "{{base_url}}/rfqs/1/award",
          "host": ["{{base_url}}"],
          "path": ["rfqs", "1", "award"]
        }
      }
    },
    {
      "name": "Award Without Confirmation",
      "request": {
        "method": "PATCH",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "Authorization",
            "value": "Bearer {{manager_token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"quotationId\": 5,\n  \"awardReason\": \"Best value for money\",\n  \"confirmed\": false\n}"
        },
        "url": {
          "raw": "{{base_url}}/rfqs/1/award",
          "host": ["{{base_url}}"],
          "path": ["rfqs", "1", "award"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    },
    {
      "key": "manager_token",
      "value": "your_jwt_token_here"
    }
  ]
}
```

---

## Database Verification Queries

### Check RFQ Award Status

```sql
SELECT
  id, rfq_number, title, status,
  awarded_vendor_id, award_reason, awarded_at
FROM rfqs
WHERE id = 1;
```

### Check Quotation Statuses

```sql
SELECT
  id, vendor_id, status, is_awarded, total_amount
FROM quotations
WHERE rfq_id = 1;
```

### Check Vendor Performance

```sql
SELECT
  id, company_name, total_rfqs_won,
  total_rfqs_participated, performance_score
FROM vendors
WHERE id = 3;
```

### Check Audit Logs

```sql
SELECT
  actor_name, action, entity_type, entity_id,
  description, timestamp
FROM audit_logs
WHERE entity_type = 'RFQ'
  AND entity_id = 1
  AND action = 'RFQ_AWARDED'
ORDER BY timestamp DESC;
```

### Check Notifications

```sql
SELECT
  recipient_id, recipient_type, title, message,
  type, is_read, created_at
FROM notifications
WHERE reference_type = 'RFQ'
  AND reference_id = 1
  AND type = 'RFQ_AWARDED'
ORDER BY created_at DESC;
```

---

## Performance Testing

### Load Test: Multiple Concurrent Award Attempts

```bash
# Use Apache Bench or similar tool
ab -n 10 -c 5 -T 'application/json' \
  -H 'Authorization: Bearer <TOKEN>' \
  -p award_payload.json \
  http://localhost:8080/rfqs/1/award
```

**Expected:** Only one request should succeed, others should fail with duplicate award error.

---

## Conclusion

These test examples cover all acceptance criteria for US 08. Use them to verify the implementation works correctly in all scenarios.
