# US 06: Secure Quotation Submission - Workflow Diagram

## Complete Submission Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         VENDOR QUOTATION SUBMISSION                      │
│                              (US 06 Flow)                                │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│   Vendor     │
│  Dashboard   │
└──────┬───────┘
       │
       │ Clicks "Submit Quotation"
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 1: Create Quotation (Pricing & Items)                             │
├──────────────────────────────────────────────────────────────────────────┤
│  ✓ Enter total amount (must be > 0.01)                                  │
│  ✓ Enter tax percentage (0-100%)                                        │
│  ✓ Select currency (INR/USD/EUR/GBP)                                    │
│  ✓ Enter delivery days (min 1)                                          │
│  ✓ Enter unit prices for each RFQ item                                  │
│  ✓ Add optional notes                                                   │
│                                                                          │
│  Backend Validations:                                                   │
│  • Deadline not passed ✓                                                │
│  • RFQ status is OPEN ✓                                                 │
│  • Vendor is invited ✓                                                  │
│  • No duplicate submission ✓                                            │
│  • Price validations ✓                                                  │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ POST /quotations
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Backend: QuotationServiceImpl.submit()                                 │
├──────────────────────────────────────────────────────────────────────────┤
│  1. Validate vendor status (APPROVED)                                   │
│  2. Validate RFQ status (OPEN, not AWARDED)                             │
│  3. Check deadline (must be in future)                                  │
│  4. Validate vendor invitation                                          │
│  5. Check for duplicate submission                                      │
│  6. Create Quotation entity (status: SUBMITTED)                         │
│  7. Create QuotationItem entities                                       │
│  8. Save to database                                                    │
│  9. Mark RFQ invite as responded                                        │
│  10. Update vendor statistics                                           │
│  11. Log audit: QUOTATION_SUBMITTED                                     │
│  12. Send notification to vendor                                        │
│  13. Send notification to procurement managers                          │
│  14. Return quotation ID                                                │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ Returns: { id: 123, status: "SUBMITTED", ... }
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 2: Upload Supporting Documents (REQUIRED)                         │
├──────────────────────────────────────────────────────────────────────────┤
│  UI Changes:                                                             │
│  • Form fields become disabled (read-only)                               │
│  • Document upload panel appears                                         │
│  • "Finalize & Submit" button appears (disabled)                         │
│                                                                          │
│  Document Upload:                                                        │
│  ✓ Select file (PDF, DOC, DOCX, XLS, XLSX, JPG, PNG)                   │
│  ✓ Max size: 10MB                                                       │
│  ✓ Multiple documents allowed                                           │
│  ✓ Each upload shows integrity verification                             │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ POST /quotations/{id}/documents (for each file)
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Backend: QuotationServiceImpl.uploadDocument()                         │
├──────────────────────────────────────────────────────────────────────────┤
│  1. Validate quotation ownership                                        │
│  2. Check deadline (must not be passed)                                 │
│  3. Calculate SHA-256 checksum                                          │
│  4. Generate unique filename (UUID + original name)                     │
│  5. Create upload directory if needed                                   │
│  6. Save file to disk                                                   │
│  7. Create QuotationDocument entity with checksum                       │
│  8. Save document metadata to database                                  │
│  9. Log audit: QUOTATION_DOCUMENT_UPLOADED (with checksum)             │
│  10. Return success                                                     │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ Returns: Success + document metadata
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Frontend: Document List Updates                                        │
├──────────────────────────────────────────────────────────────────────────┤
│  • Shows uploaded document with:                                        │
│    - Filename                                                           │
│    - File size                                                          │
│    - Upload timestamp                                                   │
│    - ✓ Integrity verified (SHA-256) badge                              │
│  • "Finalize & Submit" button becomes ENABLED                           │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ Vendor clicks "Finalize & Submit"
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 3: Finalize Submission                                            │
├──────────────────────────────────────────────────────────────────────────┤
│  Frontend Validation:                                                   │
│  • Check documents.length > 0                                           │
│  • Show error if no documents                                           │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ POST /quotations/{id}/finalize
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Backend: QuotationServiceImpl.finalizeSubmission()                     │
├──────────────────────────────────────────────────────────────────────────┤
│  1. Validate quotation ownership                                        │
│  2. Count documents: docRepo.countByQuotationId()                       │
│  3. REQUIRE at least 1 document (throw error if 0)                      │
│  4. Check deadline again (must not be passed)                           │
│  5. Check RFQ status (must not be AWARDED)                              │
│  6. Confirm quotation status as SUBMITTED                               │
│  7. Save quotation                                                      │
│  8. Log audit: QUOTATION_FINALIZED (with document count)               │
│  9. Send notification: "Quotation Finalized"                            │
│  10. Return success                                                     │
└──────────────┬───────────────────────────────────────────────────────────┘
               │
               │ Returns: Success
               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Success!                                                                │
├──────────────────────────────────────────────────────────────────────────┤
│  • Toast: "Quotation finalized and submitted successfully!"             │
│  • Navigate to: /quotations (vendor's quotation list)                   │
│  • Vendor receives notification                                         │
│  • Procurement managers notified                                        │
│  • Audit trail complete                                                 │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Security Checkpoints

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SECURITY VALIDATIONS                             │
└─────────────────────────────────────────────────────────────────────────┘

At Every Step:
├─ Authentication: JWT token required
├─ Authorization: Role-based access (VENDOR only)
└─ Audit Logging: All actions recorded

Step 1 (Create Quotation):
├─ Deadline validation (before submission)
├─ Price validation (positive decimal)
├─ Tax validation (0-100%)
├─ Currency validation (mandatory)
├─ RFQ status check (OPEN, not AWARDED)
├─ Vendor invitation check
└─ Duplicate submission prevention

Step 2 (Upload Documents):
├─ File size validation (max 10MB)
├─ File type validation (whitelist)
├─ SHA-256 checksum calculation
├─ Secure file storage (UUID naming)
├─ Deadline check (before upload)
└─ Ownership validation

Step 3 (Finalize):
├─ Document count validation (min 1)
├─ Deadline check (before finalization)
├─ RFQ status check (not AWARDED)
├─ Ownership validation
└─ Final audit log

Data in Transit:
├─ HTTPS/TLS encryption
├─ JWT token authentication
└─ Secure headers

Data at Rest:
├─ File integrity (SHA-256)
├─ Database encryption (if configured)
└─ Secure file permissions
```

---

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ERROR SCENARIOS                                  │
└─────────────────────────────────────────────────────────────────────────┘

Deadline Passed:
├─ Error: "Submission deadline has passed. Late submissions are not accepted."
├─ HTTP: 400 Bad Request
└─ Action: Prevent submission, show error message

RFQ Already Awarded:
├─ Error: "Cannot submit quotation. RFQ has already been awarded."
├─ HTTP: 400 Bad Request
└─ Action: Prevent submission, show error message

No Documents Uploaded:
├─ Error: "At least one supporting document is required before finalizing..."
├─ HTTP: 400 Bad Request
└─ Action: Disable finalize button, show warning

File Too Large:
├─ Error: "File size must be less than 10MB"
├─ Frontend validation
└─ Action: Reject file, show error toast

Invalid Price:
├─ Error: "Valid amount required" (must be > 0.01)
├─ Bean validation
└─ Action: Show field error, prevent submission

File Integrity Failed:
├─ Error: "File integrity check failed. Document may have been tampered with."
├─ HTTP: 400 Bad Request
└─ Action: Prevent download, log security incident
```

---

## Database Schema

```
┌─────────────────────────────────────────────────────────────────────────┐
│  quotations                                                              │
├─────────────────────────────────────────────────────────────────────────┤
│  id (PK)                                                                 │
│  rfq_id (FK) → rfqs.id                                                  │
│  vendor_id (FK) → vendors.id                                            │
│  total_amount (DECIMAL)                                                 │
│  tax_percentage (DOUBLE)                                                │
│  currency (VARCHAR)                                                     │
│  delivery_days (INT)                                                    │
│  notes (TEXT)                                                           │
│  status (ENUM: SUBMITTED, UNDER_EVALUATION, AWARDED, REJECTED)         │
│  weighted_score (DOUBLE)                                                │
│  evaluation_comment (TEXT)                                              │
│  evaluated_by (VARCHAR)                                                 │
│  evaluated_at (TIMESTAMP)                                               │
│  is_awarded (BOOLEAN)                                                   │
│  submitted_at (TIMESTAMP) ← @CreationTimestamp                          │
│  updated_at (TIMESTAMP) ← @UpdateTimestamp                              │
│  UNIQUE(rfq_id, vendor_id)                                              │
└─────────────────────────────────────────────────────────────────────────┘
                              │
                              │ 1:N
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  quotation_documents                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  id (PK)                                                                 │
│  quotation_id (FK) → quotations.id                                      │
│  file_name (VARCHAR)                                                    │
│  file_path (VARCHAR)                                                    │
│  file_size (BIGINT)                                                     │
│  file_type (VARCHAR)                                                    │
│  checksum (VARCHAR) ← SHA-256 hash                                      │
│  uploaded_at (TIMESTAMP) ← @CreationTimestamp                           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints Summary

```
POST   /quotations                    Create quotation (Step 1)
POST   /quotations/{id}/documents     Upload document (Step 2)
GET    /quotations/{id}/documents     List documents
POST   /quotations/{id}/finalize      Finalize submission (Step 3)
PUT    /quotations/{id}               Resubmit (before deadline)
GET    /quotations/{id}               Get quotation details
GET    /quotations/rfq/{rfqId}        List quotations for RFQ
GET    /quotations/vendor/{vendorId}  List vendor's quotations
POST   /quotations/{id}/evaluate      Evaluate quotation (Manager)
GET    /quotations/rfq/{rfqId}/compare Compare quotations
GET    /quotations/rfq/{rfqId}/export-pdf Export to PDF
GET    /quotations/documents/{docId}/download Download document
```

All endpoints require authentication and appropriate role-based authorization.
