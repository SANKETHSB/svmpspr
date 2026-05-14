package com.infosys.svpms.scheduler;

import com.infosys.svpms.entity.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class SvpmsScheduler {

    private final RfqService rfqService;
    private final ComplianceDocumentRepository complianceRepo;
    private final VendorRepository vendorRepo;
    private final NotificationService notifService;
    private final AuditService auditService;

    @Value("${compliance.expiry.warning.days:30}")
    private int warningDays;

    /**
     * US 11 AC #2: Scheduled background job for automatic RFQ closure
     * 
     * This method is the entry point for the automatic RFQ closure process.
     * It runs on a configurable schedule (default: hourly) and delegates
     * the actual closure logic to RfqService.autoCloseExpired().
     * 
     * CONFIGURATION:
     * - Cron expression: ${scheduler.rfq.close-cron:0 0 * * * *}
     * - Default: Every hour at minute 0 (0 0 * * * *)
     * - Can be customized in application.properties
     * 
     * ERROR HANDLING (US 11 AC #12):
     * - All exceptions are caught and logged
     * - Failed executions are logged at ERROR level
     * - Scheduler continues running even if one execution fails
     * - Detailed error information is captured for troubleshooting
     * 
     * MONITORING:
     * - Start time logged at INFO level
     * - Completion time logged at INFO level
     * - Errors logged at ERROR level with full stack trace
     * - Execution duration can be calculated from logs
     * 
     * @see RfqService#autoCloseExpired() - Actual closure implementation
     */
    @Scheduled(cron = "${scheduler.rfq.close-cron:0 0 * * * *}")
    public void autoCloseExpiredRFQs() {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info("[US 11 SCHEDULER] Starting RFQ auto-closure job at {}", startTime);
            
            // Delegate to service layer for actual closure logic
            rfqService.autoCloseExpired();
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            log.info("[US 11 SCHEDULER] RFQ auto-closure job completed successfully. " +
                "Start: {}, End: {}, Duration: {} seconds", 
                startTime, endTime, durationSeconds);
            
        } catch (Exception e) {
            // US 11 AC #12: Log failed scheduler execution attempts
            LocalDateTime failureTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, failureTime).getSeconds();
            
            log.error("[US 11 SCHEDULER] RFQ auto-closure job FAILED. " +
                "Start: {}, Failure: {}, Duration: {} seconds. " +
                "Error: {} - {}. " +
                "Stack trace follows. " +
                "Manual intervention may be required to close expired RFQs.",
                startTime, failureTime, durationSeconds,
                e.getClass().getSimpleName(), e.getMessage(), e);
            
            // Create audit log for scheduler failure
            try {
                auditService.log(
                    0L,
                    "SYSTEM",
                    "SCHEDULER",
                    "RFQ_AUTO_CLOSE_SCHEDULER_FAILED",
                    "SCHEDULER",
                    0L,
                    String.format(
                        "RFQ auto-closure scheduler failed at %s. " +
                        "Error: %s - %s. " +
                        "Duration: %d seconds. " +
                        "Check logs for details.",
                        failureTime,
                        e.getClass().getSimpleName(),
                        e.getMessage(),
                        durationSeconds
                    )
                );
            } catch (Exception auditException) {
                log.error("[US 11 SCHEDULER] Failed to create audit log for scheduler failure: {}", 
                    auditException.getMessage());
            }
            
            // Don't rethrow - allow scheduler to continue for next execution
        }
    }

    @Scheduled(cron = "${scheduler.compliance.check-cron:0 0 8 * * *}")
    @Transactional
    public void checkComplianceExpiry() {
        try {
            log.info("[COMPLIANCE SCHEDULER] Running compliance expiry check...");
            LocalDate today = LocalDate.now();
            LocalDate threshold = today.plusDays(warningDays);

            // AC #4: Mark actually expired documents and flag vendor as non-compliant
            List<ComplianceDocument> expired = complianceRepo.findExpiredDocuments(today);
            expired.forEach(doc -> {
                doc.setExpired(true);
                complianceRepo.save(doc);
                Vendor vendor = doc.getVendor();
                vendor.setCompliant(false);
                vendorRepo.save(vendor);

                // AC #3: Notify vendor of expiry
                notifService.send(vendor.getId(), "VENDOR",
                    "⚠️ Compliance Document Expired",
                    "Your document '" + doc.getDocumentType() + "' expired on "
                        + doc.getExpiryDate() + ". Please upload a new version immediately.",
                    Notification.NotificationType.COMPLIANCE_EXPIRY,
                    doc.getId(), "ComplianceDocument");

                // AC #11,#12: Audit the expiry event
                auditService.log(vendor.getId(), "VENDOR", vendor.getCompanyName(),
                    "COMPLIANCE_EXPIRED", "ComplianceDocument", doc.getId(),
                    "isExpired=false", "isExpired=true",
                    "Document expired: " + doc.getDocumentType()
                        + " (expiry=" + doc.getExpiryDate() + "). Vendor marked non-compliant.");

                log.warn("[AC #11] Compliance EXPIRED: vendorId={}, vendor={}, doc={}, expiry={}",
                    vendor.getId(), vendor.getCompanyName(), doc.getDocumentType(), doc.getExpiryDate());
            });

            // AC #3,#11: Warn about upcoming expirations (configurable days)
            List<ComplianceDocument> expiring = complianceRepo.findExpiringBefore(threshold);
            expiring.stream().filter(d -> !d.isExpiryWarningSent()).forEach(doc -> {
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, doc.getExpiryDate());

                // AC #3: Notify vendor
                notifService.send(doc.getVendor().getId(), "VENDOR",
                    "⚡ Document Expiring Soon",
                    "Your document '" + doc.getDocumentType() + "' will expire on "
                        + doc.getExpiryDate() + " (" + daysLeft + " days remaining). Please renew it.",
                    Notification.NotificationType.COMPLIANCE_EXPIRY,
                    doc.getId(), "ComplianceDocument");

                doc.setExpiryWarningSent(true);
                complianceRepo.save(doc);

                // AC #11: Log expiry warning to audit
                auditService.log(doc.getVendor().getId(), "VENDOR", doc.getVendor().getCompanyName(),
                    "COMPLIANCE_EXPIRY_WARNING", "ComplianceDocument", doc.getId(),
                    "Expiry warning sent: " + doc.getDocumentType()
                        + " expires on " + doc.getExpiryDate() + " (" + daysLeft + " days left)");

                log.warn("[AC #11] Expiry WARNING logged: vendorId={}, doc={}, daysLeft={}",
                    doc.getVendor().getId(), doc.getDocumentType(), daysLeft);
            });

            log.info("[COMPLIANCE SCHEDULER] Check complete. Expired={}, WarningSent={}",
                expired.size(),
                expiring.stream().filter(d -> d.isExpiryWarningSent()).count());

        } catch (Exception e) {
            log.error("[COMPLIANCE SCHEDULER] Compliance check failed: {}", e.getMessage(), e);
        }
    }
}
