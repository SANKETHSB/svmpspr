# SVPMS API Documentation - Postman Testing Guide

## Base URL

```
http://localhost:8081/api
```

## Authentication

Most endpoints require JWT authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

---

## 1. AUTHENTICATION ENDPOINTS

### 1.1 Login (User)

**POST** `/auth/login`

**Request Body:**

```json
{
  "email": "admin@svpms.com",
  "password": "Admin@123456"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "userId": 1,
    "name": "Admin User",
    "email": "admin@svpms.com",
    "role": "ADMIN"
  }
}
```

**Test Accounts:**

- Admin: `admin@svpms.com` / `Admin@123456`
- Manager: `manager@svpms.com` / `Manager@123456`
- Vendor: `vendor@techsupply.com` / `Vendor@123456`

---

### 1.2 Login (Vendor)

**POST** `/auth/vendor-login`

**Request Body:**

```json
{
  "email": "vendor@techsupply.com",
  "password": "Vendor@123456"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "vendorId": 1,
    "companyName": "Tech Supply Co",
    "email": "vendor@techsupply.com",
    "status": "APPROVED"
  }
}
```

---

### 1.3 Request Password Reset OTP

**POST** `/auth/forgot-password`

**Request Body:**

```json
{
  "email": "admin@svpms.com"
}
```

**Response:**

```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "otp": "123456",
    "message": "OTP sent to console and returned for browser testing"
  }
}
```

**Note:** OTP is printed in backend console and returned in response for testing.

---

### 1.4 Verify OTP

**POST** `/auth/verify-otp`

**Request Body:**

```json
{
  "email": "admin@svpms.com",
  "otp": "123456"
}
```

**Response:**

```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": "OTP is valid. Proceed to reset password."
}
```

---

### 1.5 Reset Password

**POST** `/auth/reset-password`

**Request Body:**

```json
{
  "email": "admin@svpms.com",
  "otp": "123456",
  "newPassword": "NewPassword@123"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Password reset successful",
  "data": "Password has been reset successfully"
}
```

---

## 2. VENDOR REGISTRATION & MANAGEMENT

### 2.1 Register Vendor

**POST** `/vendors/register`

**Request Body:**

```json
{
  "companyName": "New Vendor Corp",
  "gstNumber": "29ABCDE1234F1Z5",
  "registrationNumber": "REG123456",
  "email": "newvendor@example.com",
  "password": "Vendor@123456",
  "phone": "9876543210",
  "address": "123 Business Street, City",
  "website": "https://newvendor.com",
  "businessType": "MANUFACTURER",
  "yearEstablished": 2020
}
```

**Response:**

```json
{
  "success": true,
  "message": "Vendor registered successfully",
  "data": {
    "id": 2,
    "companyName": "New Vendor Corp",
    "email": "newvendor@example.com",
    "status": "PENDING_APPROVAL",
    "message": "Email verification link sent to console"
  }
}
```

---

### 2.2 Get All Vendors (Admin/Manager)

**GET** `/vendors`

**Headers:**

```
Authorization: Bearer <admin_or_manager_token>
```

**Query Parameters:**

- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)
- `search` (optional): Search by company name or email
- `status` (optional): Filter by status (PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED)

**Example:**

```
GET /vendors?page=0&size=10&status=APPROVED
```

**Response:**

```json
{
  "success": true,
  "message": "Vendors fetched",
  "data": {
    "content": [
      {
        "id": 1,
        "companyName": "Tech Supply Co",
        "email": "vendor@techsupply.com",
        "gstNumber": "29ABCDE1234F1Z5",
        "status": "APPROVED",
        "totalRfqsParticipated": 5,
        "totalRfqsWon": 2
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0
  }
}
```

---

### 2.3 Get Vendor by ID

**GET** `/vendors/{id}`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /vendors/1
```

---

### 2.4 Approve Vendor (Admin)

**PATCH** `/vendors/{id}/approve`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Request Body:**

```json
{
  "approvalComment": "All documents verified. Approved for procurement."
}
```

**Response:**

```json
{
  "success": true,
  "message": "Vendor approved",
  "data": {
    "id": 1,
    "companyName": "Tech Supply Co",
    "status": "APPROVED",
    "approvalComment": "All documents verified. Approved for procurement."
  }
}
```

---

### 2.5 Reject Vendor (Admin)

**PATCH** `/vendors/{id}/reject`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Request Body:**

```json
{
  "rejectionReason": "Incomplete documentation. GST certificate not valid."
}
```

---

### 2.6 Suspend Vendor (Admin)

**PATCH** `/vendors/{id}/suspend`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Request Body:**

```json
{
  "suspensionReason": "Multiple complaints received. Under investigation."
}
```

---

### 2.7 Get Vendor Approval History (Admin)

**GET** `/vendors/{id}/approval-history`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Example:**

```
GET /vendors/1/approval-history
```

---

## 3. RFQ MANAGEMENT

### 3.1 Create RFQ (Procurement Manager)

**POST** `/rfqs`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Request Body:**

```json
{
  "title": "Office Furniture Procurement",
  "description": "Procurement of office furniture for new branch",
  "terms": "Payment within 30 days. Delivery required within 15 days.",
  "deadline": "2026-06-30T23:59:59",
  "invitedVendorIds": [1, 2, 3],
  "items": [
    {
      "itemName": "Office Desk",
      "description": "Executive office desk with drawers",
      "quantity": 50,
      "unit": "pieces",
      "specifications": "Wood finish, 5ft x 3ft"
    },
    {
      "itemName": "Office Chair",
      "description": "Ergonomic office chair",
      "quantity": 50,
      "unit": "pieces",
      "specifications": "Adjustable height, lumbar support"
    }
  ]
}
```

**Response:**

```json
{
  "success": true,
  "message": "RFQ created",
  "data": {
    "id": 1,
    "rfqNumber": "RFQ-202605-1001",
    "title": "Office Furniture Procurement",
    "status": "OPEN",
    "revisionNumber": 1,
    "deadline": "2026-06-30T23:59:59",
    "items": [...],
    "invitedVendors": [...]
  }
}
```

---

### 3.2 Get All RFQs

**GET** `/rfqs`

**Headers:**

```
Authorization: Bearer <token>
```

**Query Parameters:**

- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)
- `search` (optional): Search by title or RFQ number
- `status` (optional): Filter by status (OPEN, CLOSED, AWARDED, ARCHIVED)

**Example:**

```
GET /rfqs?page=0&size=10&status=OPEN
```

---

### 3.3 Get RFQ by ID

**GET** `/rfqs/{id}`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /rfqs/1
```

---

### 3.4 Update RFQ (Procurement Manager)

**PUT** `/rfqs/{id}`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Request Body:** (Same as Create RFQ)

**Note:**

- Only OPEN RFQs can be updated
- Cannot edit after award
- Cannot edit after deadline
- Revision number auto-incremented
- Vendors notified of changes

---

### 3.5 Upload RFQ Attachment

**POST** `/rfqs/{id}/attachments`

**Headers:**

```
Authorization: Bearer <manager_token>
Content-Type: multipart/form-data
```

**Form Data:**

- `file`: (file upload)

**Example using Postman:**

1. Select POST method
2. Enter URL: `http://localhost:8081/api/rfqs/1/attachments`
3. Go to "Body" tab
4. Select "form-data"
5. Add key "file" with type "File"
6. Choose file to upload

---

### 3.6 Get RFQ Attachments

**GET** `/rfqs/{id}/attachments`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /rfqs/1/attachments
```

**Response:**

```json
{
  "success": true,
  "message": "Attachments fetched",
  "data": [
    {
      "id": 1,
      "fileName": "specifications.pdf",
      "fileSize": 245678,
      "fileType": "application/pdf",
      "revisionNumber": 1,
      "uploadedAt": "2026-05-06T10:30:00"
    }
  ]
}
```

---

### 3.7 Get RFQ Revision History

**GET** `/rfqs/{id}/revisions`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /rfqs/1/revisions
```

---

### 3.8 Get Specific RFQ Revision

**GET** `/rfqs/{id}/revisions/{revisionNumber}`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /rfqs/1/revisions/2
```

---

### 3.9 Export RFQ Revision

**GET** `/rfqs/{id}/revisions/{revisionNumber}/export`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Example:**

```
GET /rfqs/1/revisions/1/export
```

**Response:** Text file download

---

### 3.10 Close RFQ

**PATCH** `/rfqs/{id}/close`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Example:**

```
PATCH /rfqs/1/close
```

---

### 3.11 Award RFQ

**PATCH** `/rfqs/{id}/award`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Request Body:**

```json
{
  "quotationId": 5,
  "awardReason": "Best value for money with excellent delivery terms"
}
```

---

### 3.12 Get RFQs for Vendor

**GET** `/rfqs/vendor/{vendorId}`

**Headers:**

```
Authorization: Bearer <vendor_token>
```

**Query Parameters:**

- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)

**Example:**

```
GET /rfqs/vendor/1?page=0&size=10
```

---

## 4. QUOTATION MANAGEMENT

### 4.1 Submit Quotation (Vendor)

**POST** `/quotations`

**Headers:**

```
Authorization: Bearer <vendor_token>
```

**Request Body:**

```json
{
  "rfqId": 1,
  "totalAmount": 125000.0,
  "taxPercentage": 18.0,
  "currency": "INR",
  "deliveryDays": 15,
  "notes": "We can deliver within 15 days. Warranty included.",
  "items": [
    {
      "rfqItemId": 1,
      "unitPrice": 2500.0
    },
    {
      "rfqItemId": 2,
      "unitPrice": 2500.0
    }
  ]
}
```

**Response:**

```json
{
  "success": true,
  "message": "Quotation submitted",
  "data": {
    "id": 1,
    "rfqId": 1,
    "rfqNumber": "RFQ-202605-1001",
    "vendorId": 1,
    "vendorName": "Tech Supply Co",
    "totalAmount": 125000.0,
    "taxAmount": 22500.0,
    "grandTotal": 147500.0,
    "currency": "INR",
    "status": "SUBMITTED",
    "submittedAt": "2026-05-06T15:30:00"
  }
}
```

---

### 4.2 Resubmit/Update Quotation (Vendor)

**PUT** `/quotations/{id}`

**Headers:**

```
Authorization: Bearer <vendor_token>
```

**Request Body:** (Same as Submit Quotation)

**Note:** Can only resubmit before deadline

---

### 4.3 Upload Quotation Document (Vendor)

**POST** `/quotations/{id}/documents`

**Headers:**

```
Authorization: Bearer <vendor_token>
Content-Type: multipart/form-data
```

**Form Data:**

- `file`: (file upload)

**Example:**

```
POST /quotations/1/documents
```

**Response:**

```json
{
  "success": true,
  "message": "Document uploaded successfully with integrity verification",
  "data": "Document uploaded successfully with integrity verification"
}
```

---

### 4.4 Get Quotation Documents

**GET** `/quotations/{id}/documents`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /quotations/1/documents
```

**Response:**

```json
{
  "success": true,
  "message": "Documents fetched",
  "data": [
    {
      "id": 1,
      "fileName": "technical_specs.pdf",
      "fileSize": 156789,
      "fileType": "application/pdf",
      "checksum": "a3b5c7d9e1f2...",
      "uploadedAt": "2026-05-06T16:00:00"
    }
  ]
}
```

---

### 4.5 Download Quotation Document

**GET** `/quotations/documents/{documentId}/download`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /quotations/documents/1/download
```

**Response:** File download (inline preview)

---

### 4.6 Get Quotation by ID

**GET** `/quotations/{id}`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /quotations/1
```

---

### 4.7 Get Quotations by RFQ (Manager)

**GET** `/quotations/rfq/{rfqId}`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Example:**

```
GET /quotations/rfq/1
```

**Response:** List of quotations sorted by amount (ascending)

---

### 4.8 Compare Quotations (Manager)

**GET** `/quotations/rfq/{rfqId}/compare`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Example:**

```
GET /quotations/rfq/1/compare
```

**Response:**

```json
{
  "success": true,
  "message": "Comparison ready",
  "data": [
    {
      "id": 1,
      "vendorName": "Tech Supply Co",
      "totalAmount": 125000.0,
      "grandTotal": 147500.0,
      "deliveryDays": 15,
      "weightedScore": 85.5,
      "isLowestBidder": true,
      "documentCount": 2,
      "status": "UNDER_EVALUATION"
    },
    {
      "id": 2,
      "vendorName": "Office Solutions Ltd",
      "totalAmount": 130000.0,
      "grandTotal": 153400.0,
      "deliveryDays": 20,
      "weightedScore": 78.0,
      "isLowestBidder": false,
      "documentCount": 1,
      "status": "SUBMITTED"
    }
  ]
}
```

---

### 4.9 Export Comparison to PDF (Manager)

**GET** `/quotations/rfq/{rfqId}/export-pdf`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Example:**

```
GET /quotations/rfq/1/export-pdf
```

**Response:** PDF file download

---

### 4.10 Evaluate Quotation (Manager)

**POST** `/quotations/{id}/evaluate`

**Headers:**

```
Authorization: Bearer <manager_token>
```

**Request Body:**

```json
{
  "score": 85.5,
  "comment": "Excellent pricing and delivery terms. Technical specifications meet all requirements. Vendor has good track record.",
  "award": false
}
```

**Response:**

```json
{
  "success": true,
  "message": "Quotation evaluated",
  "data": {
    "id": 1,
    "vendorName": "Tech Supply Co",
    "weightedScore": 85.5,
    "evaluationComment": "Excellent pricing and delivery terms...",
    "evaluatedBy": "Manager User",
    "evaluatedAt": "2026-05-06T17:00:00",
    "status": "UNDER_EVALUATION"
  }
}
```

**Note:**

- Comment is mandatory (min 10 characters)
- Cannot evaluate while RFQ is still open
- Score range: 0.0 to 100.0

---

### 4.11 Get Quotations by Vendor

**GET** `/quotations/vendor/{vendorId}`

**Headers:**

```
Authorization: Bearer <token>
```

**Example:**

```
GET /quotations/vendor/1
```

---

## 5. USER MANAGEMENT

### 5.1 Create User (Admin)

**POST** `/users`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Request Body:**

```json
{
  "name": "New Manager",
  "email": "newmanager@svpms.com",
  "password": "Manager@123456",
  "role": "PROCUREMENT_MANAGER"
}
```

**Roles:**

- `ADMIN`
- `PROCUREMENT_MANAGER`
- `COMPLIANCE_OFFICER`

---

### 5.2 Get All Users (Admin)

**GET** `/users`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Query Parameters:**

- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)

---

### 5.3 Get User by ID (Admin)

**GET** `/users/{id}`

**Headers:**

```
Authorization: Bearer <admin_token>
```

---

### 5.4 Update User (Admin)

**PUT** `/users/{id}`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Request Body:**

```json
{
  "name": "Updated Name",
  "email": "updated@svpms.com",
  "role": "PROCUREMENT_MANAGER",
  "isActive": true
}
```

---

### 5.5 Delete User (Admin)

**DELETE** `/users/{id}`

**Headers:**

```
Authorization: Bearer <admin_token>
```

---

## 6. COMPLIANCE DOCUMENTS

### 6.1 Upload Compliance Document (Vendor)

**POST** `/compliance/upload`

**Headers:**

```
Authorization: Bearer <vendor_token>
Content-Type: multipart/form-data
```

**Form Data:**

- `file`: (file upload)
- `documentType`: GST_CERTIFICATE, PAN_CARD, REGISTRATION_CERTIFICATE, etc.
- `vendorId`: Vendor ID

---

### 6.2 Get Vendor Compliance Documents

**GET** `/compliance/vendor/{vendorId}`

**Headers:**

```
Authorization: Bearer <token>
```

---

### 6.3 Download Compliance Document

**GET** `/compliance/{documentId}/download`

**Headers:**

```
Authorization: Bearer <token>
```

---

## 7. NOTIFICATIONS

### 7.1 Get My Notifications

**GET** `/notifications/my`

**Headers:**

```
Authorization: Bearer <token>
```

**Query Parameters:**

- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)

---

### 7.2 Get Unread Count

**GET** `/notifications/unread-count`

**Headers:**

```
Authorization: Bearer <token>
```

---

### 7.3 Mark Notification as Read

**PATCH** `/notifications/{id}/read`

**Headers:**

```
Authorization: Bearer <token>
```

---

### 7.4 Mark All as Read

**PATCH** `/notifications/mark-all-read`

**Headers:**

```
Authorization: Bearer <token>
```

---

## 8. DASHBOARD & REPORTS

### 8.1 Get Dashboard Stats

**GET** `/dashboard/stats`

**Headers:**

```
Authorization: Bearer <token>
```

**Response:**

```json
{
  "success": true,
  "message": "Dashboard stats fetched",
  "data": {
    "totalRfqs": 25,
    "openRfqs": 5,
    "totalVendors": 15,
    "approvedVendors": 12,
    "pendingApprovals": 3,
    "totalQuotations": 45,
    "averageQuotationsPerRfq": 3.2
  }
}
```

---

### 8.2 Get Audit Logs (Admin)

**GET** `/audit/logs`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Query Parameters:**

- `page` (optional): Page number
- `size` (optional): Page size
- `actorId` (optional): Filter by actor ID
- `action` (optional): Filter by action
- `entityType` (optional): Filter by entity type
- `from` (optional): From date (ISO format)
- `to` (optional): To date (ISO format)

---

### 8.3 Export Audit Logs (Admin)

**GET** `/audit/export`

**Headers:**

```
Authorization: Bearer <admin_token>
```

**Query Parameters:**

- `entityType` (optional): Filter by entity type

**Response:** CSV file download

---

## POSTMAN COLLECTION SETUP

### Step 1: Create Environment

1. Click "Environments" in Postman
2. Create new environment "SVPMS Local"
3. Add variables:
   - `base_url`: `http://localhost:8081/api`
   - `admin_token`: (will be set after login)
   - `manager_token`: (will be set after login)
   - `vendor_token`: (will be set after login)

### Step 2: Login and Save Token

1. Send login request
2. Copy the token from response
3. Go to Environment
4. Paste token in appropriate variable

### Step 3: Use Token in Requests

In Authorization tab:

- Type: Bearer Token
- Token: `{{admin_token}}` or `{{manager_token}}` or `{{vendor_token}}`

---

## COMMON ERROR RESPONSES

### 400 Bad Request

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ]
}
```

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Unauthorized",
  "error": "Invalid or expired token"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Access denied",
  "error": "You do not have permission to access this resource"
}
```

### 404 Not Found

```json
{
  "success": false,
  "message": "Resource not found",
  "error": "RFQ with id 999 not found"
}
```

### 409 Conflict

```json
{
  "success": false,
  "message": "Duplicate entry",
  "error": "Vendor with this email already exists"
}
```

### 500 Internal Server Error

```json
{
  "success": false,
  "message": "Internal server error",
  "error": "An unexpected error occurred"
}
```

---

## TESTING WORKFLOW

### Complete Test Flow:

1. **Admin Login**

   ```
   POST /auth/login
   Body: {"email": "admin@svpms.com", "password": "Admin@123456"}
   Save token to {{admin_token}}
   ```

2. **Register New Vendor**

   ```
   POST /vendors/register
   Body: {vendor details}
   ```

3. **Approve Vendor (Admin)**

   ```
   PATCH /vendors/{id}/approve
   Headers: Authorization: Bearer {{admin_token}}
   ```

4. **Vendor Login**

   ```
   POST /auth/vendor-login
   Body: {vendor credentials}
   Save token to {{vendor_token}}
   ```

5. **Manager Login**

   ```
   POST /auth/login
   Body: {"email": "manager@svpms.com", "password": "Manager@123456"}
   Save token to {{manager_token}}
   ```

6. **Create RFQ (Manager)**

   ```
   POST /rfqs
   Headers: Authorization: Bearer {{manager_token}}
   Body: {RFQ details with items}
   ```

7. **Upload RFQ Attachment (Manager)**

   ```
   POST /rfqs/{id}/attachments
   Headers: Authorization: Bearer {{manager_token}}
   Form-data: file
   ```

8. **Submit Quotation (Vendor)**

   ```
   POST /quotations
   Headers: Authorization: Bearer {{vendor_token}}
   Body: {quotation details}
   ```

9. **Upload Quotation Document (Vendor)**

   ```
   POST /quotations/{id}/documents
   Headers: Authorization: Bearer {{vendor_token}}
   Form-data: file
   ```

10. **Compare Quotations (Manager)**

    ```
    GET /quotations/rfq/{rfqId}/compare
    Headers: Authorization: Bearer {{manager_token}}
    ```

11. **Evaluate Quotation (Manager)**

    ```
    POST /quotations/{id}/evaluate
    Headers: Authorization: Bearer {{manager_token}}
    Body: {score, comment}
    ```

12. **Export Comparison PDF (Manager)**

    ```
    GET /quotations/rfq/{rfqId}/export-pdf
    Headers: Authorization: Bearer {{manager_token}}
    ```

13. **Award RFQ (Manager)**
    ```
    PATCH /rfqs/{id}/award
    Headers: Authorization: Bearer {{manager_token}}
    Body: {quotationId, awardReason}
    ```

---

## NOTES

- All timestamps are in ISO 8601 format
- File uploads use `multipart/form-data`
- All monetary values are in decimal format
- Pagination starts from page 0
- Default page size is 10
- JWT tokens expire after 24 hours
- OTP expires after 10 minutes
- File size limit: 10MB per file
- Supported file types: PDF, DOC, DOCX, XLS, XLSX, JPG, PNG

---

## SUPPORT

For issues or questions:

- Check backend console for detailed error logs
- Verify JWT token is valid and not expired
- Ensure correct role permissions for endpoints
- Check database connection status

---

**Last Updated:** May 6, 2026
**API Version:** 1.0.0
**Backend Port:** 8081
**Frontend Port:** 3000
