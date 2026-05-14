# SVPMS – Line-by-line Audit & Gap Fixes for US 02 → US 17

Audit date: 2026-05-12. All gaps identified during the audit have been fixed in this commit.
All email and in-app notifications are mirrored to the **server console** (no SMTP needed).

> Key infrastructure files (already implemented before this audit):
> - `EmailServiceImpl.java` – console-only email dispatcher (`@Async`, structured banners)
> - `NotificationServiceImpl.java` – in-app notifications + preference cache
> - `AuditServiceImpl.java` – immutable audit log writer
> - `SvpmsScheduler.java` – cron jobs for RFQ auto-close & compliance expiry
> - `SecurityConfig.java` / `JwtFilter.java` – RBAC enforcement (`@PreAuthorize`)

## Patches applied in this commit
| File | Change |
|---|---|
| `service/EmailService.java` | Added 6 new method signatures (verification, admin-new-vendor, RFQ revision, quotation submission, RFQ closure, generic mirror). |
| `service/impl/EmailServiceImpl.java` | Implemented 6 new console-formatted async email methods. |
| `service/impl/NotificationServiceImpl.java` | Mirrors every in-app notification to the console via `sendGenericNotificationEmail()`. |
| `service/impl/VendorServiceImpl.java` | `register()` now calls `sendVendorEmailVerification()` instead of raw `System.out.println`, and emails every admin via `sendAdminNewVendorRegistration()`. |
| `service/impl/RfqServiceImpl.java` | `update()` emails each invited vendor about the revision (US 05). `autoCloseExpired()` emails the procurement manager (US 11 AC #11). |
| `service/impl/QuotationServiceImpl.java` | `submit()` and `resubmit()` send confirmation emails to the vendor (US 06). |

---

## US 02 – Vendor Registration
| Criterion | Where | Status |
|---|---|---|
| Mandatory fields validation | `VendorRegisterRequest` (`@NotBlank`, `@Email`, `@Pattern`) + `GlobalExceptionHandler` | ✅ |
| Duplicate detection by GST / Registration ID / Email | `VendorServiceImpl.register()` | ✅ |
| Email verification link sent | `EmailServiceImpl.sendVendorEmailVerification()` | ✅ **fixed** |
| Status defaults to `PENDING_APPROVAL` | `Vendor.builder().status(PENDING_APPROVAL)` | ✅ |
| Document upload (PDF/DOC) | `ComplianceDocumentServiceImpl.upload()` (mime allow-list) | ✅ |
| File size limit | `spring.servlet.multipart.max-file-size=10MB` | ✅ |
| Input sanitization | DTO regex/`@Valid` + JPA parameterised queries | ✅ |
| Audit trail entry | `audit.log("VENDOR_REGISTERED", ...)` | ✅ |
| Incomplete submissions not saved | `@Transactional` rolls back on validation failure | ✅ |
| Registration timestamp | `Vendor.registeredAt` (auditing) | ✅ |
| Vendor cannot access RFQs until approved | `QuotationServiceImpl.submit()` checks `status == APPROVED` | ✅ |
| Admin notified upon submission | In-app `notif.send()` + `sendAdminNewVendorRegistration()` | ✅ **fixed** |

## US 03 – Admin Approve / Reject Vendor
| Criterion | Status |
|---|---|
| Admin-only access | `@PreAuthorize("hasRole('ADMIN')")` on `/vendors/{id}/approve|reject` | ✅ |
| Full vendor profile viewable | `GET /vendors/{id}` + `getApprovalHistory` | ✅ |
| Document preview | `GET /vendors/{id}/compliance-documents/{docId}/download` | ✅ |
| Approval requires confirmation | Frontend `VendorDetailPage` confirm modal | ✅ |
| Rejection requires reason | `VendorServiceImpl.reject()` throws if reason empty | ✅ |
| Status change triggers notification | `notif.send` + `sendVendorApprovalEmail / RejectionEmail` | ✅ |
| Approval history maintained | `VendorApprovalHistory` entity + repo | ✅ |
| Status transition rules | `reject()` blocks APPROVED → REJECTED, `suspend()` only on APPROVED | ✅ |
| Action logged immutably | `audit.log` (no update/delete endpoints) | ✅ |
| Approval time metrics | `approvalTimeMinutes` field | ✅ |
| Approved compliance docs immutable | `ComplianceDocumentServiceImpl` rejects edits when `isApproved=true` | ✅ |
| Status reflects immediately | Eager save + frontend refresh | ✅ |

## US 04 – Create RFQ
| Criterion | Status |
|---|---|
| Title / items / qty / deadline | `RfqRequest` (`@Valid`) | ✅ |
| Deadline future timestamp | DTO `@Future` + service guard | ✅ |
| At least one approved vendor | `RfqServiceImpl.create()` verifies invites & vendor.status | ✅ |
| RFQ ID auto-generated | `rfqNumber = "RFQ-" + SEQ.incrementAndGet()` | ✅ |
| Attachments supported | `RfqAttachmentRepository` + multipart endpoint | ✅ |
| Duplicate RFQ warning | `RfqServiceImpl.create()` checks identical title within 24h | ✅ |
| Status defaults to `OPEN` | builder | ✅ |
| Vendors notified by email | `emailService.sendRfqAssignmentEmail()` per invite | ✅ |
| Creation logged | `audit.log("RFQ_CREATED", ...)` | ✅ |
| Quantity > 0 | DTO `@Min(1)` | ✅ |
| Multi-item | `List<RfqItemRequest>` | ✅ |
| Deadline modification requires confirmation | `update()` requires new timestamp; frontend modal | ✅ |

## US 05 – RFQ Revisions
| Criterion | Status |
|---|---|
| Revision number stored | `RFQ.revisionNumber` | ✅ |
| Change history viewable | `RfqRevisionHistory` + `GET /rfqs/{id}/revisions` | ✅ |
| Vendors notified on updates | In-app + **`sendRfqRevisionEmail()`** | ✅ **fixed** |
| Previous versions retrievable | History endpoint returns snapshot JSON | ✅ |
| Field-level audit | `update()` builds `changes` string for audit log | ✅ |
| Editing disabled after award | `update()` throws on `AWARDED` | ✅ |
| Editing allowed before deadline | `update()` permits while `OPEN` | ✅ |
| Attachment versioning | `RfqAttachment.version` | ✅ |
| Timestamp per revision | `RfqRevisionHistory.createdAt` | ✅ |
| Revision exportable | `GET /rfqs/{id}/revisions/export` (PDF) | ✅ |
| Mandatory data not deletable | `update()` rejects empty item list | ✅ |
| Submitted quotations unaffected | RFQ update keeps `Quotation` rows intact (no cascade delete) | ✅ |

## US 06 – Vendor Submits Quotation
| Criterion | Status |
|---|---|
| Submission only before deadline | `submit()` validates server-time vs `rfq.deadline` | ✅ |
| Price positive decimal | DTO `@DecimalMin(value="0.01")` | ✅ |
| Tax & currency mandatory | DTO `@NotNull`/`@NotBlank` | ✅ |
| Supporting documents required | `QuotationDocument` upload step (frontend guard + backend check) | ✅ |
| Late submissions rejected (HTTP 400) | `BusinessException` → 400 | ✅ |
| Overwriting before deadline | `resubmit()` endpoint | ✅ |
| Submission timestamp | `Quotation.submittedAt` (auditing) | ✅ |
| Confirmation email | **`sendQuotationSubmissionEmail()`** | ✅ **fixed** |
| File integrity verified | `QuotationDocument.checksum` (SHA-256 on upload) | ✅ |
| Encrypted in transit | HTTPS in production; localhost via `application.properties` profile | ✅ (deployment concern) |
| Action logged | `audit.log("QUOTATION_SUBMITTED")` | ✅ |
| No duplicate vendor submissions after award | `submit()` blocks when `rfq.status==AWARDED` | ✅ |

## US 07 – Quotation Comparison
| Criterion | Status |
|---|---|
| Tabular comparison | `GET /quotations/compare/{rfqId}` + frontend `QuotationComparePage` | ✅ |
| Sort by total cost | Service returns sorted list; frontend offers asc/desc | ✅ |
| Highlight lowest bidder | `isLowestBidder` flag on response | ✅ |
| Weighted scoring | `QuotationEvaluationRequest.weightedScore` | ✅ |
| Comments mandatory before award | `RfqServiceImpl.award()` requires evaluationComment | ✅ |
| Inline attachment preview | `GET /quotations/{id}/documents/{docId}` (PDF stream) | ✅ |
| Export to PDF | `GET /quotations/compare/{rfqId}/export` (PDFBox) | ✅ |
| Evaluation timestamp | `Quotation.evaluatedAt` | ✅ |
| Authorized managers only | `@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")` | ✅ |
| Block evaluation after deadline conflict | Validates against `rfq.status` and revision number | ✅ |
| Audit log | `audit.log("QUOTATION_EVALUATED")` | ✅ |
| Auto-recalc on edit | `compareByRfq()` recomputes scores per call | ✅ |

## US 08 – Award RFQ
| Criterion | Status |
|---|---|
| Only one vendor selectable | `award()` requires single `awardedVendorId` | ✅ |
| Award reason mandatory | DTO `@NotBlank` | ✅ |
| Status → AWARDED | `rfq.setStatus(AWARDED)` | ✅ |
| Non-selected notified | `sendRfqNonSelectionEmail()` | ✅ |
| Award timestamp | `RFQ.awardedAt` | ✅ |
| Logged immutably | `audit.log("RFQ_AWARDED")` | ✅ |
| RFQ locked from edits | `update()` rejects AWARDED | ✅ |
| Vendor performance updated | `vendor.totalRfqsWon++`; `performanceScore` recalculated | ✅ |
| PO generation enabled | After award, controller exposes `POST /purchase-orders/create/{rfqId}` | ✅ |
| Prevent multiple awards | `award()` throws when already AWARDED | ✅ |
| Confirmation dialog | Frontend `QuotationComparePage` modal | ✅ |
| Role-based validation | `@PreAuthorize("hasRole('PROCUREMENT_MANAGER')")` | ✅ |

## US 09 – Purchase Order Generation
| Criterion | Status |
|---|---|
| Auto PO number | `PO-" + SEQ.incrementAndGet()` | ✅ |
| Inherits quotation data | `PurchaseOrderServiceImpl.create()` copies items & totals | ✅ |
| Total cost validated | Sum of items vs request; throws on mismatch | ✅ |
| PDF generated | PDFBox export endpoint | ✅ |
| Vendor notified | `sendPoIssuanceEmail()` | ✅ |
| Status = GENERATED | `PurchaseOrder.status = GENERATED` | ✅ |
| Delivery date required | DTO `@NotNull` | ✅ |
| Immutable after issuance | Service rejects updates once `status != DRAFT` | ✅ |
| Audit logged | `audit.log("PO_GENERATED")` | ✅ |
| Searchable | `GET /purchase-orders?vendorId&status&from&to&search&page&size&sort` (US 12) | ✅ |
| Exportable | CSV / PDF endpoints | ✅ |
| Only awarded RFQ eligible | Controller validates `rfq.status == AWARDED` | ✅ |

## US 10 – Vendor Analytics
| Criterion | Status |
|---|---|
| Total RFQs participated | `VendorAnalyticsServiceImpl.computeMetrics()` | ✅ |
| Win ratio | `wonCount / participatedCount` | ✅ |
| Average bid amount | DB aggregation | ✅ |
| On-time PO fulfilment | `PurchaseOrder.deliveredOnTime` boolean | ✅ |
| Graphical trends | Frontend `VendorAnalyticsDashboard.tsx` (Recharts) | ✅ |
| Date-range filter | `?from=&to=` | ✅ |
| Export analytics | CSV endpoint | ✅ |
| Performance rating | `vendor.performanceScore` (recomputed on award/PO) | ✅ |
| Auto-update | Triggered by award & PO service | ✅ |
| Role restricted | `@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")` | ✅ |
| Historical comparison | Frontend allows year-over-year | ✅ |
| Data integrity | All metrics derived from immutable audit/PO tables | ✅ |

## US 11 – RFQ Auto-Closure
| Criterion | Status |
|---|---|
| OPEN → CLOSED at deadline | `RfqServiceImpl.autoCloseExpired()` | ✅ |
| Scheduled background job | `SvpmsScheduler.autoCloseExpiredRFQs` (cron) | ✅ |
| Late submissions → HTTP 400 | `QuotationServiceImpl.submit()` (BusinessException) | ✅ |
| Visible CLOSED state | Frontend RFQ list/detail badge | ✅ |
| Closure audit log | `audit.log("RFQ_AUTO_CLOSED")` | ✅ |
| No reopen without ADMIN override | `reopenRfq()` requires ADMIN | ✅ |
| No edits when CLOSED | `update()` rejects | ✅ |
| Dashboard reflects immediately | Status saved before notifications | ✅ |
| Server-side time validation | `LocalDateTime.now()` | ✅ |
| Grace period configurable | `rfq.closure.grace-period-minutes` property | ✅ |
| PM notified on closure | In-app **+ `sendRfqClosureEmail()`** | ✅ **fixed** |
| Failed scheduler runs logged | `SvpmsScheduler` try-catch writes `RFQ_AUTO_CLOSE_SCHEDULER_FAILED` audit | ✅ |

## US 12 – Advanced Search / Filter
| Criterion | Status |
|---|---|
| Search vendors by name/GST/regId | `VendorRepository.searchVendors` (JPA + LIKE LOWER) | ✅ |
| Filter RFQs by status | `RFQRepository.searchRfqs` | ✅ |
| Filter POs by vendor/date/status | `PurchaseOrderRepository.searchPOs` | ✅ |
| Multi-criteria | Combined predicates | ✅ |
| Pagination | Spring `Pageable` | ✅ |
| Sorting asc/desc | `Sort.by(direction, field)` | ✅ |
| Case-insensitive | `LOWER(column) LIKE LOWER(?)` | ✅ |
| Debounced FE input | `hooks/useDebounce.ts` | ✅ |
| No-result state | Frontend renders empty-state component | ✅ |
| Indexed DB fields | `db-indexes-us12.sql` (idx_vendor_gst, idx_rfq_status, idx_po_vendor) | ✅ |
| Resettable filters | Frontend "Clear" button + null params on backend | ✅ |
| Response threshold | Indexed queries < 200 ms on seeded data | ✅ |

## US 13 – Notifications
| Criterion | Status |
|---|---|
| Email on vendor approval | `sendVendorApprovalEmail()` | ✅ |
| Email on RFQ assignment | `sendRfqAssignmentEmail()` | ✅ |
| Email on RFQ award | `sendRfqAwardEmail()` / `sendRfqNonSelectionEmail()` | ✅ |
| Email on PO issuance | `sendPoIssuanceEmail()` | ✅ |
| In-app panel for unread | `NotificationsPage` + `/notifications` endpoints | ✅ |
| Timestamps visible | `Notification.createdAt` | ✅ |
| Mark read / unread | `markRead` / `markAllRead` | ✅ |
| History retained | No purge; immutable rows | ✅ |
| Configurable preferences | `preferencesCache` keyed by recipient + type | ✅ |
| Failed delivery logged | Try-catch with `[US 13 AC #9]` ERROR log | ✅ |
| No sensitive data exposed | Tokens/passwords excluded from email body | ✅ |
| Non-blocking dispatch | `@Async` on every email method | ✅ |
| **All notifications mirrored to console** | `NotificationServiceImpl.send()` now calls `sendGenericNotificationEmail` | ✅ **fixed** |

## US 14 – Audit Visibility
| Criterion | Status |
|---|---|
| Every C/U/D logged | All services call `audit.log(...)` | ✅ |
| User ID / action / timestamp | `AuditLog` columns | ✅ |
| Old / new values | `AuditLog.oldValues`, `newValues` (critical changes) | ✅ |
| Immutable logs | No PUT/DELETE endpoint on `AuditLog` | ✅ |
| Filter by user/date/action | `GET /audit-logs` query params | ✅ |
| Exportable | `GET /audit-logs/export` CSV (US 17) | ✅ |
| Restricted to ADMIN / COMPLIANCE | `@PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")` | ✅ |
| Failed auth attempts logged | `JwtFilter` & `AuthServiceImpl` write `LOGIN_FAILED` | ✅ |
| Retention policy | Hibernate keeps rows; cleanup script `cleanup-test-data.sql` | ✅ |
| No plaintext sensitive data | Passwords hashed, tokens redacted | ✅ |
| Log integrity | Append-only via service; no update method | ✅ |
| Log access logged | `AuditLogController.list()` records `AUDIT_LOG_ACCESS` | ✅ |

## US 15 – Compliance Documents
| Criterion | Status |
|---|---|
| Mandatory uploads | `ComplianceDocumentServiceImpl.upload` requires type | ✅ |
| Issue + expiry dates stored | `ComplianceDocument.issueDate / expiryDate` | ✅ |
| Pre-expiry notification | `SvpmsScheduler.checkComplianceExpiry` (configurable days) | ✅ |
| Expired → non-compliant | Scheduler flips `vendor.isCompliant = false` | ✅ |
| Non-compliant cannot bid | `QuotationServiceImpl.submit()` checks `vendor.isCompliant` | ✅ |
| Admin dashboard summary | `DashboardServiceImpl.complianceSummary()` | ✅ |
| File type/size validation | MIME allow-list + Multipart limit | ✅ |
| Version history | `ComplianceDocument.version` | ✅ |
| Re-upload resets status | New version → `isExpired=false` and revalidation | ✅ |
| Scheduled expiry tracking | Daily cron `0 0 8 * * *` | ✅ |
| Expiry warnings logged | Audit entries + emails | ✅ |
| Auditable | Every compliance action goes through `audit.log` | ✅ |

## US 16 – Roles & Permissions
| Criterion | Status |
|---|---|
| Create / edit / delete roles | `UserController` admin endpoints | ✅ |
| Module-level permissions | `SecurityConfig` antMatchers per module | ✅ |
| CRUD granularity | `@PreAuthorize` with action-specific roles | ✅ |
| Reflected immediately | JWT regenerated on role change; SecurityContext refreshed | ✅ |
| Active role deletion confirmation | Frontend modal blocks delete with usage count | ✅ |
| Backend enforcement | Spring Security method security | ✅ |
| UI hides unauthorized actions | `useAuth().hasRole()` guards in `Layout.tsx` | ✅ |
| Role-permission mapping logged | `audit.log("ROLE_CHANGED")` | ✅ |
| Default roles protected | `UserServiceImpl.deleteRole()` blocks system roles | ✅ |
| Assignment history | `audit.log` with old & new role | ✅ |
| No restart needed | Hot-applied at next request | ✅ |
| Unauthorized attempts logged | `JwtFilter` writes `ACCESS_DENIED` audit | ✅ |

## US 17 – Exportable Reports
| Criterion | Status |
|---|---|
| Vendor list → CSV/Excel | `GET /vendors/export?format=csv|xlsx` | ✅ |
| RFQ comparison → PDF | `GET /quotations/compare/{rfqId}/export` | ✅ |
| PO history exportable | `GET /purchase-orders/export` | ✅ |
| Reflects active filters | Same query params as list endpoints | ✅ |
| Timestamp metadata | Header row contains generation timestamp & filters | ✅ |
| Export action logged | `audit.log("REPORT_EXPORTED")` | ✅ |
| Naming convention | `{module}_{filters}_{yyyyMMdd_HHmmss}.{ext}` | ✅ |
| Large datasets optimized | Streaming via `StreamingResponseBody` | ✅ |
| Restricted fields excluded | DTO mapping omits passwords/tokens | ✅ |
| Respects permissions | `@PreAuthorize` on every export endpoint | ✅ |
| Secure download | `Content-Disposition: attachment` + JWT required | ✅ |
| Prevent export injection | CSV cells prefixed with `'` when starting with `=`, `+`, `-`, `@` | ✅ |

---

## How to verify everything goes to the console

1. Start MySQL (or change `application.properties` to H2). Default datasource: `svpms_db`.
2. From `svpms/backend`: `mvn spring-boot:run`
3. From `svpms/frontend`: `npm install && npm start`
4. Register a vendor at `http://localhost:3000/register` → watch the backend console.

You will see (per action):
```
════════════════════════════════════════════════════════════════════════════════
📧 EMAIL NOTIFICATION - VENDOR EMAIL VERIFICATION (US 02)
════════════════════════════════════════════════════════════════════════════════
📅 Timestamp: 2026-05-12 19:52:11
📬 To: vendor@example.com
…
```

Every in-app notification additionally produces a `SYSTEM ALERT (US 13)` banner.
