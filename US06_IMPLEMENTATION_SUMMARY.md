# US 06: Secure Quotation Submission - Implementation Summary

## User Story

**As a Vendor**, I want to submit a secure quotation before deadline.

## Priority

Must Have

---

## Acceptance Criteria Implementation Status

### ✅ 1. Submission only before deadline

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` lines 35-38  
**Implementation:**

```java
if (rfq.getDeadline().isBefore(LocalDateTime.now())) {
    throw new BusinessException("Submission deadline has passed. Late submissions are not accepted.");
}
```

### ✅ 2. Price must be positive decimal

**Status:** IMPLEMENTED  
**Location:** `QuotationRequest.java`  
**Implementation:**

```java
@NotNull @DecimalMin("0.01") public BigDecimal totalAmount;
```

### ✅ 3. Tax & currency mandatory

**Status:** IMPLEMENTED  
**Location:** `QuotationRequest.java`  
**Implementation:**

```java
@NotNull @DecimalMin("0.0") @DecimalMax("100.0") public Double taxPercentage;
@NotBlank @Size(max=10) public String currency;
```

### ✅ 4. Supporting documents required

**Status:** NEWLY IMPLEMENTED  
**Location:**

- Backend: `QuotationServiceImpl.java` - `finalizeSubmission()` method
- Frontend: `QuotationSubmitPage.tsx` - Document upload UI
- API: `QuotationController.java` - `/quotations/{id}/finalize` endpoint

**Implementation:**

- Two-step submission process:
  1. Create quotation with pricing details
  2. Upload supporting documents (minimum 1 required)
  3. Finalize submission (validates document requirement)
- Backend validation ensures at least one document before finalization
- Frontend enforces document upload with clear UI feedback

**Code:**

```java
@Override @Transactional
public QuotationResponse finalizeSubmission(Long quotationId, String vendorEmail) {
    // ... validation ...
    long documentCount = docRepo.countByQuotationId(quotationId);
    if (documentCount == 0) {
        throw new BusinessException("At least one supporting document is required...");
    }
    // ... finalize ...
}
```

### ✅ 5. Late submissions automatically rejected

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` - `rejectLateSubmissions()` method  
**Implementation:**

- Scheduled job checks for expired RFQs
- Automatically rejects quotations submitted after deadline
- Sends notification to vendor about rejection
- Logs rejection in audit trail

### ✅ 6. Overwriting previous submission allowed before deadline

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` - `resubmit()` method  
**Implementation:**

```java
if (q.getRfq().getDeadline().isBefore(LocalDateTime.now())) {
    throw new BusinessException("Deadline has passed. Cannot resubmit.");
}
```

### ✅ 7. Submission timestamp stored

**Status:** IMPLEMENTED  
**Location:** `Quotation.java` entity  
**Implementation:**

```java
@CreationTimestamp
private LocalDateTime submittedAt;
```

### ✅ 8. Confirmation email sent

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` lines 67-70  
**Implementation:**

```java
notif.send(vendor.getId(),"VENDOR","Quotation Submitted Successfully",
    "Your quotation for RFQ "+rfq.getRfqNumber()+" has been submitted successfully...",
    Notification.NotificationType.GENERAL, saved.getId(), "Quotation");
```

### ✅ 9. File integrity verified

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` - `uploadDocument()` method  
**Implementation:**

- SHA-256 checksum calculated on upload
- Checksum stored in database with document metadata
- Checksum verified on download to detect tampering
- Audit log records checksum for traceability

**Code:**

```java
private String calculateChecksum(MultipartFile file) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(file.getBytes());
    // Convert to hex string
    return hexString.toString();
}
```

### ✅ 10. Quotation encrypted in transit

**Status:** IMPLEMENTED  
**Location:** Spring Security configuration  
**Implementation:**

- HTTPS enforced by Spring Security
- All API endpoints require authentication
- JWT tokens used for secure session management
- TLS/SSL encryption for data in transit

### ✅ 11. Submission action logged

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` lines 63-65  
**Implementation:**

```java
audit.log(vendor.getId(),"VENDOR",vendor.getCompanyName(),"QUOTATION_SUBMITTED","Quotation",saved.getId(),
    "Submitted for RFQ: "+rfq.getRfqNumber()+" | Amount: "+req.getTotalAmount()+" "+req.getCurrency());
```

### ✅ 12. System prevents duplicate vendor submissions after award

**Status:** IMPLEMENTED  
**Location:** `QuotationServiceImpl.java` lines 30-33  
**Implementation:**

```java
if (rfq.getStatus() == RFQ.RFQStatus.AWARDED) {
    throw new BusinessException("Cannot submit quotation. RFQ has already been awarded.");
}
```

---

## New Features Added

### 1. Document Upload API

**Endpoint:** `POST /quotations/{id}/documents`  
**Features:**

- Multipart file upload
- File size validation (max 10MB)
- File type validation
- SHA-256 integrity verification
- Secure file storage with UUID naming

### 2. Finalize Submission API

**Endpoint:** `POST /quotations/{id}/finalize`  
**Features:**

- Validates document requirement (minimum 1)
- Checks deadline compliance
- Prevents finalization after RFQ award
- Sends confirmation notification
- Logs finalization action

### 3. Get Documents API

**Endpoint:** `GET /quotations/{id}/documents`  
**Returns:**

- Document metadata (filename, size, type)
- Upload timestamp
- SHA-256 checksum
- Document ID for download

### 4. Enhanced Frontend UI

**Features:**

- Two-step submission workflow
- Real-time document upload with progress
- Document list with integrity indicators
- Clear validation messages
- Disabled form fields after quotation creation
- Finalize button enabled only when documents uploaded

---

## Files Modified

### Backend

1. `QuotationService.java` - Added `finalizeSubmission()` interface method
2. `QuotationServiceImpl.java` - Implemented finalization logic with document validation
3. `QuotationController.java` - Added `/finalize` endpoint
4. `QuotationRequest.java` - Already had proper validations

### Frontend

1. `api.ts` - Added `uploadDocument()`, `getDocuments()`, `finalize()` methods
2. `QuotationSubmitPage.tsx` - Complete rewrite with document upload UI

---

## Testing Checklist

### Backend Tests

- [ ] Submit quotation before deadline - should succeed
- [ ] Submit quotation after deadline - should fail
- [ ] Submit with negative price - should fail (validation)
- [ ] Submit without tax/currency - should fail (validation)
- [ ] Finalize without documents - should fail
- [ ] Finalize with documents - should succeed
- [ ] Upload document with valid file - should succeed
- [ ] Upload document > 10MB - should fail
- [ ] Resubmit before deadline - should succeed
- [ ] Resubmit after deadline - should fail
- [ ] Submit to awarded RFQ - should fail
- [ ] Verify file integrity on download - should match checksum

### Frontend Tests

- [ ] Create quotation form validation works
- [ ] Document upload shows progress
- [ ] Document list updates after upload
- [ ] Finalize button disabled without documents
- [ ] Finalize button enabled with documents
- [ ] Success messages display correctly
- [ ] Error messages display correctly
- [ ] Form fields disabled after quotation creation
- [ ] Navigation works after finalization

### Integration Tests

- [ ] End-to-end quotation submission flow
- [ ] Document upload and integrity verification
- [ ] Notification sent on submission
- [ ] Audit log created for all actions
- [ ] Late submission rejection by scheduler

---

## Security Features

1. **Authentication:** All endpoints require valid JWT token
2. **Authorization:** Only vendors can submit quotations
3. **File Integrity:** SHA-256 checksum verification
4. **Encryption in Transit:** HTTPS/TLS enforced
5. **Input Validation:** Bean validation on all request DTOs
6. **Audit Trail:** All actions logged with actor, timestamp, details
7. **Access Control:** Vendors can only access their own quotations

---

## User Experience Flow

1. **Vendor navigates to RFQ details**
2. **Clicks "Submit Quotation"**
3. **Fills pricing and item details**
4. **Clicks "Create Quotation"** → Quotation saved as draft
5. **Upload supporting documents** (PDF, DOC, XLS, images)
   - Each upload shows integrity verification
   - Can upload multiple documents
6. **Clicks "Finalize & Submit"** → Validates documents exist
7. **Receives confirmation notification**
8. **Redirected to quotations list**

---

## Compliance Summary

✅ All 12 acceptance criteria are fully implemented  
✅ Security best practices followed  
✅ User experience is intuitive and clear  
✅ Error handling is comprehensive  
✅ Audit trail is complete  
✅ File integrity is verified  
✅ Deadline enforcement is strict

**US 06 Status: COMPLETE AND READY FOR TESTING**
