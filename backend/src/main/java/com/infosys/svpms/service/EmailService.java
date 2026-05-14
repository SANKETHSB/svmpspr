package com.infosys.svpms.service;

/**
 * US 13: Email Notification Service
 * 
 * This service handles email notifications for procurement-related activities.
 * For demo/testing purposes, emails are logged to console instead of being sent.
 * 
 * US 13 Acceptance Criteria:
 * - AC #1: Email notification on vendor approval
 * - AC #2: Email notification on RFQ assignment
 * - AC #3: Email notification on RFQ award
 * - AC #4: Email notification on PO issuance
 * - AC #9: Failed email delivery must be logged
 * - AC #11: Notification content must not expose sensitive data
 * - AC #12: Notification dispatch must not block core workflow execution
 */
public interface EmailService {
    
    /**
     * Send vendor approval notification email
     * US 13 AC #1: Email notification on vendor approval
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param vendorId Vendor's ID for reference
     */
    void sendVendorApprovalEmail(String toEmail, String vendorName, Long vendorId);
    
    /**
     * Send vendor rejection notification email
     * US 13 AC #11: No sensitive data exposed
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param reason Rejection reason
     */
    void sendVendorRejectionEmail(String toEmail, String vendorName, String reason);
    
    /**
     * Send RFQ assignment notification email
     * US 13 AC #2: Email notification on RFQ assignment
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param rfqNumber RFQ number for reference
     * @param rfqTitle RFQ title
     * @param deadline Submission deadline
     */
    void sendRfqAssignmentEmail(String toEmail, String vendorName, String rfqNumber, 
                                 String rfqTitle, String deadline);
    
    /**
     * Send RFQ award notification email
     * US 13 AC #3: Email notification on RFQ award
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param rfqNumber RFQ number
     * @param rfqTitle RFQ title
     * @param awardReason Reason for award
     */
    void sendRfqAwardEmail(String toEmail, String vendorName, String rfqNumber, 
                           String rfqTitle, String awardReason);
    
    /**
     * Send RFQ non-selection notification email
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param rfqNumber RFQ number
     * @param rfqTitle RFQ title
     */
    void sendRfqNonSelectionEmail(String toEmail, String vendorName, String rfqNumber, String rfqTitle);
    
    /**
     * Send PO issuance notification email
     * US 13 AC #4: Email notification on PO issuance
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param poNumber Purchase Order number
     * @param rfqNumber Related RFQ number
     * @param totalAmount PO total amount
     * @param currency Currency
     * @param deliveryDate Expected delivery date
     */
    void sendPoIssuanceEmail(String toEmail, String vendorName, String poNumber, 
                             String rfqNumber, String totalAmount, String currency, 
                             String deliveryDate);
    
    /**
     * Send compliance document expiry warning email
     * 
     * @param toEmail Vendor's email address
     * @param vendorName Vendor's company name
     * @param documentType Type of document
     * @param expiryDate Expiry date
     */
    void sendComplianceExpiryEmail(String toEmail, String vendorName, String documentType, String expiryDate);
    
    /**
     * Send account locked notification email
     * 
     * @param toEmail User's email address
     * @param userName User's name
     * @param lockedUntil Account lockout end time
     */
    void sendAccountLockedEmail(String toEmail, String userName, String lockedUntil);
    
    /**
     * Send password reset email
     * 
     * @param toEmail User's email address
     * @param userName User's name
     * @param resetToken Password reset token
     */
    void sendPasswordResetEmail(String toEmail, String userName, String resetToken);

    /**
     * US 02: Send vendor email verification link upon self-registration.
     */
    void sendVendorEmailVerification(String toEmail, String vendorName, String verificationLink, String verificationToken);

    /**
     * US 02: Notify admin(s) that a new vendor has submitted a registration awaiting approval.
     */
    void sendAdminNewVendorRegistration(String adminEmail, String adminName, String vendorName,
                                        String vendorGst, String vendorRegistrationId, Long vendorId);

    /**
     * US 05: Notify invited vendors that the RFQ has been revised.
     */
    void sendRfqRevisionEmail(String toEmail, String vendorName, String rfqNumber, String rfqTitle,
                              int revisionNumber, String changeSummary, String newDeadline);

    /**
     * US 06: Confirmation email to vendor upon successful quotation submission.
     */
    void sendQuotationSubmissionEmail(String toEmail, String vendorName, String rfqNumber, String rfqTitle,
                                      Long quotationId, String totalAmount, String currency, String submittedAt);

    /**
     * US 11: Email notification to the Procurement Manager upon RFQ auto-closure.
     */
    void sendRfqClosureEmail(String toEmail, String managerName, String rfqNumber, String rfqTitle,
                             String closedAt, int totalQuotationsReceived);

    /**
     * US 13 (transparency): Generic console mirror for any in-app notification.
     */
    void sendGenericNotificationEmail(String toEmail, String recipientName, String title, String message);
}
