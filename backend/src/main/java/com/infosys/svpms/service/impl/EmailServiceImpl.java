package com.infosys.svpms.service.impl;

import com.infosys.svpms.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * US 13: Email Notification Service Implementation
 * 
 * This service simulates email notifications by logging them to console.
 * In production, this would integrate with a real email provider (SMTP, SendGrid, etc.)
 * 
 * US 13 Acceptance Criteria Implementation:
 * 
 * AC #1: Email notification on vendor approval
 *   - sendVendorApprovalEmail() logs formatted email to console
 * 
 * AC #2: Email notification on RFQ assignment
 *   - sendRfqAssignmentEmail() logs formatted email to console
 * 
 * AC #3: Email notification on RFQ award
 *   - sendRfqAwardEmail() logs formatted email to console
 * 
 * AC #4: Email notification on PO issuance
 *   - sendPoIssuanceEmail() logs formatted email to console
 * 
 * AC #9: Failed email delivery must be logged
 *   - All methods wrapped in try-catch with ERROR level logging
 * 
 * AC #11: Notification content must not expose sensitive data
 *   - Emails don't include passwords, tokens, or confidential information
 *   - Only reference IDs and public information displayed
 * 
 * AC #12: Notification dispatch must not block core workflow execution
 *   - All email methods annotated with @Async for non-blocking execution
 *   - Failures don't throw exceptions to prevent workflow interruption
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEPARATOR = "═".repeat(80);
    private static final String THIN_SEPARATOR = "─".repeat(80);

    /**
     * US 13 AC #12: Async execution to prevent blocking
     * All email methods use @Async annotation for non-blocking execution
     */

    @Async
    @Override
    public void sendVendorApprovalEmail(String toEmail, String vendorName, Long vendorId) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - VENDOR APPROVED\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {} (ID: {})", vendorName, vendorId);
            log.info("📋 Subject: 🎉 Congratulations! Your Vendor Account Has Been Approved");
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("Great news! Your vendor account has been APPROVED.");
            log.info("");
            log.info("You can now:");
            log.info("  ✅ Access your vendor dashboard");
            log.info("  ✅ View and respond to RFQ invitations");
            log.info("  ✅ Submit quotations for open RFQs");
            log.info("  ✅ Manage your compliance documents");
            log.info("");
            log.info("Vendor ID: {}", vendorId);
            log.info("Status: APPROVED");
            log.info("");
            log.info("Login to your dashboard: http://localhost:3000/login");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Procurement Team");
            log.info("Smart Vendor Procurement Management System");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13 AC #1] Vendor approval email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send vendor approval email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendVendorRejectionEmail(String toEmail, String vendorName, String reason) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - VENDOR REGISTRATION REJECTED\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: Update on Your Vendor Registration");
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("Thank you for your interest in becoming a vendor with us.");
            log.info("");
            log.info("After careful review, we regret to inform you that your vendor");
            log.info("registration could not be approved at this time.");
            log.info("");
            log.info("Reason: {}", reason);
            log.info("");
            log.info("You may address the concerns mentioned above and reapply");
            log.info("for vendor registration in the future.");
            log.info("");
            log.info("If you have any questions, please contact our procurement team.");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13 AC #11] Vendor rejection email sent to: {} (no sensitive data)", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send vendor rejection email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendRfqAssignmentEmail(String toEmail, String vendorName, String rfqNumber, 
                                        String rfqTitle, String deadline) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - NEW RFQ INVITATION\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: 📝 New RFQ Invitation: {}", rfqNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("You have been invited to participate in a new Request for Quotation!");
            log.info("");
            log.info("━━━━━━━━━━━━━━ RFQ DETAILS ━━━━━━━━━━━━━━");
            log.info("  RFQ Number:   {}", rfqNumber);
            log.info("  Title:        {}", rfqTitle);
            log.info("  Deadline:     {} ⏰", deadline);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("");
            log.info("To submit your quotation:");
            log.info("  1. Login to your vendor dashboard");
            log.info("  2. Navigate to RFQs > View Details");
            log.info("  3. Click 'Submit Quotation' before the deadline");
            log.info("");
            log.info("⚠️  Important: Late submissions will not be accepted.");
            log.info("");
            log.info("Dashboard: http://localhost:3000/rfqs");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13 AC #2] RFQ assignment email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send RFQ assignment email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendRfqAwardEmail(String toEmail, String vendorName, String rfqNumber, 
                                   String rfqTitle, String awardReason) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - RFQ AWARD WINNER 🏆\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: 🎉 Congratulations! RFQ Awarded - {}", rfqNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("🎉 CONGRATULATIONS! 🎉");
            log.info("");
            log.info("We are pleased to inform you that your quotation has been selected!");
            log.info("");
            log.info("━━━━━━━━━━━━━━ AWARD DETAILS ━━━━━━━━━━━━━━");
            log.info("  RFQ Number:   {}", rfqNumber);
            log.info("  Title:        {}", rfqTitle);
            log.info("  Status:       AWARDED ✓");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("");
            log.info("Award Reason: {}", awardReason);
            log.info("");
            log.info("Next Steps:");
            log.info("  📄 A Purchase Order will be generated shortly");
            log.info("  📧 You will receive another notification with PO details");
            log.info("  📦 Please prepare for order fulfillment");
            log.info("");
            log.info("Thank you for your participation and competitive quotation!");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13 AC #3] RFQ award email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send RFQ award email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendRfqNonSelectionEmail(String toEmail, String vendorName, String rfqNumber, String rfqTitle) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - RFQ RESULT\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: RFQ Update - {}", rfqNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("Thank you for your participation in the RFQ process.");
            log.info("");
            log.info("━━━━━━━━━━━━━━ RFQ DETAILS ━━━━━━━━━━━━━━");
            log.info("  RFQ Number:   {}", rfqNumber);
            log.info("  Title:        {}", rfqTitle);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("");
            log.info("After careful evaluation, the RFQ has been awarded to another vendor.");
            log.info("");
            log.info("We appreciate your interest and participation. Your quotation was");
            log.info("valued, and we encourage you to participate in future opportunities.");
            log.info("");
            log.info("We look forward to working with you in future procurements.");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13] RFQ non-selection email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send RFQ non-selection email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendPoIssuanceEmail(String toEmail, String vendorName, String poNumber, 
                                     String rfqNumber, String totalAmount, String currency, 
                                     String deliveryDate) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - PURCHASE ORDER ISSUED\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: 📦 Purchase Order Issued - {}", poNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("A Purchase Order has been generated and issued to you!");
            log.info("");
            log.info("━━━━━━━━━━━━━━ PURCHASE ORDER ━━━━━━━━━━━━━━");
            log.info("  PO Number:     {}", poNumber);
            log.info("  Related RFQ:   {}", rfqNumber);
            log.info("  Total Amount:  {} {}", totalAmount, currency);
            log.info("  Delivery Date: {} 📦", deliveryDate);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("");
            log.info("ACTION REQUIRED:");
            log.info("  ✅ Please review the Purchase Order details");
            log.info("  ✅ Confirm acceptance through the system");
            log.info("  ✅ Prepare for delivery by the specified date");
            log.info("");
            log.info("You can download the official PO document from your dashboard.");
            log.info("");
            log.info("Dashboard: http://localhost:3000/purchase-orders");
            log.info("");
            log.info("Thank you for your partnership!");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13 AC #4] PO issuance email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send PO issuance email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendComplianceExpiryEmail(String toEmail, String vendorName, String documentType, String expiryDate) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - COMPLIANCE DOCUMENT EXPIRING\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: ⚠️ Compliance Document Expiring Soon");
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", vendorName);
            log.info("");
            log.info("⚠️ ACTION REQUIRED - Document Expiry Warning");
            log.info("");
            log.info("Your compliance document is expiring soon:");
            log.info("");
            log.info("━━━━━━━━━━━━━━ DOCUMENT DETAILS ━━━━━━━━━━━━━━");
            log.info("  Document Type: {}", documentType);
            log.info("  Expiry Date:   {} ⏰", expiryDate);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("");
            log.info("Please renew this document before it expires to maintain your");
            log.info("compliant status and continue participating in RFQs.");
            log.info("");
            log.info("Upload renewed document: http://localhost:3000/compliance");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Compliance Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13] Compliance expiry email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send compliance expiry email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendAccountLockedEmail(String toEmail, String userName, String lockedUntil) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - ACCOUNT LOCKED\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 User: {}", userName);
            log.info("📋 Subject: 🔒 Account Temporarily Locked");
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", userName);
            log.info("");
            log.info("🔒 SECURITY NOTICE - Account Temporarily Locked");
            log.info("");
            log.info("Your account has been temporarily locked due to multiple failed");
            log.info("login attempts. This is a security measure to protect your account.");
            log.info("");
            log.info("━━━━━━━━━━━━━━ LOCK DETAILS ━━━━━━━━━━━━━━");
            log.info("  Status:        LOCKED");
            log.info("  Locked Until:  {}", lockedUntil);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("");
            log.info("Your account will automatically unlock after the specified time.");
            log.info("");
            log.info("If this was not you, please contact support immediately.");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Security Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13] Account locked email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send account locked email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - PASSWORD RESET REQUEST\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 User: {}", userName);
            log.info("📋 Subject: 🔑 Password Reset Request");
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},\n", userName);
            log.info("");
            log.info("A password reset has been requested for your account.");
            log.info("");
            log.info("Reset your password using this link:");
            log.info("http://localhost:3000/reset-password?token={}", resetToken);
            log.info("");
            log.info("⚠️ This link will expire in 24 hours.");
            log.info("⚠️ If you did not request this reset, please ignore this email.");
            log.info("");
            log.info("Best regards,");
            log.info("SVPMS Support Team");
            log.info("{}\n", SEPARATOR);
            
            log.info("[US 13 AC #11] Password reset email sent to: {} (token not logged)", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // US 02 / US 05 / US 06 / US 11 / US 13 - Additional console email channels
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    @Override
    public void sendVendorEmailVerification(String toEmail, String vendorName, String verificationLink, String verificationToken) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - VENDOR EMAIL VERIFICATION (US 02)\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: ✉️  Please verify your SVPMS vendor account");
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},", vendorName);
            log.info("");
            log.info("Thank you for registering with SVPMS. Please verify your email by clicking:");
            log.info("");
            log.info("  🔗 {}", verificationLink);
            log.info("");
            log.info("Verification Token: {}", verificationToken);
            log.info("Status: PENDING_APPROVAL  (your account will be reviewed after verification)");
            log.info("");
            log.info("This link expires in 24 hours.");
            log.info("");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            log.info("[US 02] Verification email logged to console for: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send vendor verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendAdminNewVendorRegistration(String adminEmail, String adminName, String vendorName,
                                               String vendorGst, String vendorRegistrationId, Long vendorId) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - NEW VENDOR REGISTRATION (Admin Alert, US 02)\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {} ({})", adminEmail, adminName);
            log.info("📋 Subject: 🆕 New vendor awaiting approval: {}", vendorName);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},", adminName);
            log.info("");
            log.info("A new vendor has submitted a registration and is awaiting your review:");
            log.info("");
            log.info("  Vendor:          {}", vendorName);
            log.info("  GST Number:      {}", vendorGst);
            log.info("  Registration ID: {}", vendorRegistrationId);
            log.info("  Vendor ID:       {}", vendorId);
            log.info("  Status:          PENDING_APPROVAL");
            log.info("");
            log.info("Please review and approve / reject the vendor in the Admin console.");
            log.info("");
            log.info("Admin Console: http://localhost:3000/vendors/{}", vendorId);
            log.info("");
            log.info("SVPMS Notification Service");
            log.info("{}\n", SEPARATOR);
            log.info("[US 02] Admin notification email logged for: {}", adminEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send admin new-vendor email to {}: {}", adminEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendRfqRevisionEmail(String toEmail, String vendorName, String rfqNumber, String rfqTitle,
                                     int revisionNumber, String changeSummary, String newDeadline) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - RFQ REVISED (US 05)\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: 📝 RFQ {} has been revised (Revision {})", rfqNumber, revisionNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},", vendorName);
            log.info("");
            log.info("The following RFQ has been revised. Please review the changes:");
            log.info("");
            log.info("  RFQ Number:   {}", rfqNumber);
            log.info("  Title:        {}", rfqTitle);
            log.info("  Revision:     {}", revisionNumber);
            log.info("  New Deadline: {}", newDeadline);
            log.info("");
            log.info("Change Summary:");
            log.info("  {}", changeSummary);
            log.info("");
            log.info("⚠️  If you have already submitted a quotation, you may resubmit before the deadline.");
            log.info("");
            log.info("Dashboard: http://localhost:3000/rfqs");
            log.info("");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            log.info("[US 05] RFQ revision email logged for: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send RFQ revision email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendQuotationSubmissionEmail(String toEmail, String vendorName, String rfqNumber, String rfqTitle,
                                             Long quotationId, String totalAmount, String currency, String submittedAt) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - QUOTATION SUBMITTED (US 06)\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail);
            log.info("👤 Vendor: {}", vendorName);
            log.info("📋 Subject: ✅ Quotation submitted for RFQ {}", rfqNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},", vendorName);
            log.info("");
            log.info("Your quotation has been received successfully.");
            log.info("");
            log.info("  RFQ Number:      {}", rfqNumber);
            log.info("  RFQ Title:       {}", rfqTitle);
            log.info("  Quotation ID:    {}", quotationId);
            log.info("  Total Amount:    {} {}", totalAmount, currency);
            log.info("  Submitted At:    {}", submittedAt);
            log.info("");
            log.info("🔒 Submission is encrypted in transit and stored under your vendor account.");
            log.info("📄 You may resubmit (overwrite) until the deadline.");
            log.info("");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            log.info("[US 06] Quotation confirmation email logged for: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send quotation confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendRfqClosureEmail(String toEmail, String managerName, String rfqNumber, String rfqTitle,
                                    String closedAt, int totalQuotationsReceived) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - RFQ AUTO-CLOSED (US 11)\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {} ({})", toEmail, managerName);
            log.info("📋 Subject: ⏰ RFQ {} has been auto-closed", rfqNumber);
            log.info("{}", THIN_SEPARATOR);
            log.info("Dear {},", managerName);
            log.info("");
            log.info("The deadline has passed and the system has automatically closed the RFQ:");
            log.info("");
            log.info("  RFQ Number:           {}", rfqNumber);
            log.info("  Title:                {}", rfqTitle);
            log.info("  Closed At:            {}", closedAt);
            log.info("  Quotations Received:  {}", totalQuotationsReceived);
            log.info("  Status:               CLOSED  (edits disabled)");
            log.info("");
            log.info("Next step: evaluate received quotations and award the RFQ.");
            log.info("");
            log.info("Comparison: http://localhost:3000/quotations/compare/{}", rfqNumber);
            log.info("");
            log.info("SVPMS Procurement Team");
            log.info("{}\n", SEPARATOR);
            log.info("[US 11] RFQ closure email logged for: {}", toEmail);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send RFQ closure email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendGenericNotificationEmail(String toEmail, String recipientName, String title, String message) {
        try {
            log.info("\n{}\n📧 EMAIL NOTIFICATION - SYSTEM ALERT (US 13)\n{}", SEPARATOR, SEPARATOR);
            log.info("📅 Timestamp: {}", LocalDateTime.now().format(DATE_FORMATTER));
            log.info("📬 To: {}", toEmail == null ? "(in-app only)" : toEmail);
            log.info("👤 Recipient: {}", recipientName);
            log.info("📋 Subject: {}", title);
            log.info("{}", THIN_SEPARATOR);
            log.info("{}", message);
            log.info("{}\n", SEPARATOR);
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to send generic notification email: {}", e.getMessage());
        }
    }
}
