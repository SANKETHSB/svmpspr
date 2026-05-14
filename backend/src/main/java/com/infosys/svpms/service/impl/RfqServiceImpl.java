package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.request.*;
import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class RfqServiceImpl implements RfqService {

    private final RFQRepository rfqRepo;
    private final RfqItemRepository itemRepo;
    private final RfqVendorInviteRepository inviteRepo;
    private final RfqAttachmentRepository attachmentRepo;
    private final RfqRevisionHistoryRepository revisionHistoryRepo;
    private final VendorRepository vendorRepo;
    private final UserRepository userRepo;
    private final QuotationRepository quotationRepo;
    private final AuditService audit;
    private final NotificationService notif;
    private final EmailService emailService; // US 13: Email notification service
    private static final AtomicInteger SEQ = new AtomicInteger(1000);
    
    @org.springframework.beans.factory.annotation.Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    // US 11 AC #10: Grace period configuration for RFQ auto-closure
    @org.springframework.beans.factory.annotation.Value("${rfq.closure.grace-period-minutes:0}")
    private int gracePeriodMinutes;

    @Override @Transactional
    public RfqResponse create(RfqRequest req, String actorEmail) {
        User actor = userRepo.findByEmail(actorEmail).orElseThrow();
        
        // Duplicate RFQ warning - check for similar title in last 30 days
        List<RFQ> recentRfqs = rfqRepo.findByCreatedAtAfter(LocalDateTime.now().minusDays(30));
        boolean hasSimilar = recentRfqs.stream()
            .anyMatch(r -> r.getTitle().equalsIgnoreCase(req.getTitle().trim()));
        
        if (hasSimilar) {
            log.warn("Potential duplicate RFQ detected: {}", req.getTitle());
            // Log warning but allow creation (business decision)
            audit.log(actor.getId(),"USER",actor.getName(),"DUPLICATE_RFQ_WARNING","RFQ",0L,
                "Potential duplicate RFQ: "+req.getTitle());
        }
        
        // Validate deadline is in future
        if (req.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Deadline must be in the future");
        }
        
        // Validate at least one item
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("RFQ must have at least one item");
        }
        
        // Validate all quantities > 0
        boolean hasInvalidQuantity = req.getItems().stream()
            .anyMatch(item -> item.getQuantity() == null || item.getQuantity() <= 0);
        if (hasInvalidQuantity) {
            throw new BusinessException("All item quantities must be greater than 0");
        }
        
        String rfqNum = "RFQ-" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now()) + "-" + SEQ.incrementAndGet();

        RFQ rfq = RFQ.builder()
            .rfqNumber(rfqNum).title(req.getTitle()).description(req.getDescription())
            .terms(req.getTerms()).deadline(req.getDeadline())
            .createdBy(actor).status(RFQ.RFQStatus.OPEN).build();

        List<RfqItem> items = req.getItems().stream().map(i -> RfqItem.builder()
            .rfq(rfq).itemName(i.getItemName()).description(i.getDescription())
            .quantity(i.getQuantity()).unit(i.getUnit()).specifications(i.getSpecifications()).build()
        ).collect(Collectors.toList());
        rfq.setItems(items);

        RFQ saved = rfqRepo.save(rfq);

        // Invite vendors — AC #5: Non-compliant vendors cannot participate in new RFQs
        List<Vendor> vendors = vendorRepo.findAllById(req.getInvitedVendorIds())
            .stream()
            .filter(v -> v.getStatus() == Vendor.VendorStatus.APPROVED)
            .collect(Collectors.toList());
        if (vendors.isEmpty()) throw new BusinessException("No approved vendors found in the invite list.");

        // Separate compliant from non-compliant for informative response
        List<Vendor> nonCompliant = vendors.stream().filter(v -> !v.isCompliant()).collect(Collectors.toList());
        List<Vendor> compliantVendors = vendors.stream().filter(Vendor::isCompliant).collect(Collectors.toList());

        if (!nonCompliant.isEmpty()) {
            String names = nonCompliant.stream().map(Vendor::getCompanyName).collect(Collectors.joining(", "));
            log.warn("[AC #5] Excluded non-compliant vendors from RFQ {}: {}", rfqNum, names);
            audit.log(actor.getId(), "USER", actor.getName(), "NON_COMPLIANT_VENDORS_EXCLUDED",
                "RFQ", 0L, "Non-compliant vendors excluded from RFQ " + rfqNum + ": " + names);
        }

        if (compliantVendors.isEmpty()) {
            throw new BusinessException(
                "All selected vendors are non-compliant. Non-compliant vendors cannot participate in RFQs. " +
                "Please ensure vendors have valid compliance documents before inviting them.");
        }

        compliantVendors.forEach(v -> {
            inviteRepo.save(RfqVendorInvite.builder().rfq(saved).vendor(v).build());
            notif.send(v.getId(),"VENDOR","New RFQ: "+rfqNum,
                "You've been invited to submit a quotation for: "+req.getTitle(),
                Notification.NotificationType.RFQ_ASSIGNED, saved.getId(), "RFQ");
            
            // US 13 AC #2: Email notification on RFQ assignment
            log.info("US 13 AC #2: Sending RFQ assignment email to vendor: {}", v.getCompanyName());
            emailService.sendRfqAssignmentEmail(
                v.getEmail(),
                v.getCompanyName(),
                rfqNum,
                req.getTitle(),
                req.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
        });

        audit.log(actor.getId(),"USER",actor.getName(),"RFQ_CREATED","RFQ",saved.getId(),"RFQ created: "+rfqNum);
        return toResponse(saved);
    }

    @Override @Transactional
    public RfqResponse update(Long id, RfqRequest req, String actorEmail) {
        RFQ rfq = rfqRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("RFQ","id",id));
        User actor = userRepo.findByEmail(actorEmail).orElseThrow();
        
        // US 05: Editing disabled after award
        if (rfq.getStatus() == RFQ.RFQStatus.AWARDED) {
            throw new BusinessException("Cannot edit RFQ after it has been awarded.");
        }
        
        // US 11 AC #7: Closed RFQs must not allow edits
        if (rfq.getStatus() == RFQ.RFQStatus.CLOSED) {
            throw new BusinessException("Cannot edit RFQ after it has been closed. " +
                "RFQ was closed on " + rfq.getUpdatedAt() + ". " +
                "Contact an administrator if you need to reopen this RFQ.");
        }
        
        // US 05: Editing allowed only before deadline
        if (LocalDateTime.now().isAfter(rfq.getDeadline())) {
            throw new BusinessException("Cannot edit RFQ after deadline has passed.");
        }
        
        if (rfq.getStatus() != RFQ.RFQStatus.OPEN)
            throw new BusinessException("Only OPEN RFQs can be updated.");
        
        // Validate deadline is in future
        if (req.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Deadline must be in the future");
        }
        
        // US 05: Prevent deletion of mandatory data
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new BusinessException("RFQ title is mandatory and cannot be deleted.");
        }
        if (req.getTerms() == null || req.getTerms().trim().isEmpty()) {
            throw new BusinessException("RFQ terms are mandatory and cannot be deleted.");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("RFQ must have at least one item. Cannot delete all items.");
        }
        
        // US 05: Save current state to revision history before updating
        saveRevisionHistory(rfq, actor, "RFQ updated");
        
        // US 05: Track field-level changes for audit
        StringBuilder changes = new StringBuilder();
        if (!rfq.getTitle().equals(req.getTitle())) {
            audit.log(actor.getId(),"USER",actor.getName(),"RFQ_FIELD_CHANGED","RFQ",id,
                rfq.getTitle(), req.getTitle(), "Title changed");
            changes.append("Title changed; ");
        }
        if (!rfq.getDescription().equals(req.getDescription())) {
            audit.log(actor.getId(),"USER",actor.getName(),"RFQ_FIELD_CHANGED","RFQ",id,
                rfq.getDescription(), req.getDescription(), "Description changed");
            changes.append("Description changed; ");
        }
        if (!rfq.getTerms().equals(req.getTerms())) {
            audit.log(actor.getId(),"USER",actor.getName(),"RFQ_FIELD_CHANGED","RFQ",id,
                rfq.getTerms(), req.getTerms(), "Terms changed");
            changes.append("Terms changed; ");
        }
        
        // Check if deadline is being modified
        boolean deadlineChanged = !rfq.getDeadline().equals(req.getDeadline());
        if (deadlineChanged) {
            log.warn("RFQ {} deadline modified from {} to {}", rfq.getRfqNumber(), rfq.getDeadline(), req.getDeadline());
            audit.log(actor.getId(),"USER",actor.getName(),"RFQ_DEADLINE_MODIFIED","RFQ",id,
                "Deadline changed", rfq.getDeadline().toString(), req.getDeadline().toString());
            changes.append("Deadline changed; ");
            
            // Notify all invited vendors about deadline change
            inviteRepo.findByRfqId(id).forEach(invite -> {
                notif.send(invite.getVendor().getId(),"VENDOR",
                    "RFQ Deadline Modified: "+rfq.getRfqNumber(),
                    "The deadline for '"+rfq.getTitle()+"' has been changed to "+req.getDeadline(),
                    Notification.NotificationType.GENERAL, id, "RFQ");
            });
        }
        
        // Validate quantities
        boolean hasInvalidQuantity = req.getItems().stream()
            .anyMatch(item -> item.getQuantity() == null || item.getQuantity() <= 0);
        if (hasInvalidQuantity) {
            throw new BusinessException("All item quantities must be greater than 0");
        }
        
        // Track item changes
        if (rfq.getItems().size() != req.getItems().size()) {
            changes.append("Items count changed; ");
        }
        
        rfq.setTitle(req.getTitle()); 
        rfq.setDescription(req.getDescription());
        rfq.setTerms(req.getTerms()); 
        rfq.setDeadline(req.getDeadline());
        rfq.setRevisionNumber(rfq.getRevisionNumber() + 1);

        rfq.getItems().clear();
        req.getItems().forEach(i -> rfq.getItems().add(RfqItem.builder()
            .rfq(rfq).itemName(i.getItemName()).description(i.getDescription())
            .quantity(i.getQuantity()).unit(i.getUnit()).specifications(i.getSpecifications()).build()));

        RFQ saved = rfqRepo.save(rfq);
        
        // US 05 AC: Vendors are notified on updates (in-app + email)
        final String changeSummary = changes.length() == 0 ? "Details updated by procurement manager." : changes.toString();
        inviteRepo.findByRfqId(id).forEach(invite -> {
            notif.send(invite.getVendor().getId(),"VENDOR",
                "RFQ Updated: "+rfq.getRfqNumber(),
                "RFQ '"+rfq.getTitle()+"' has been updated. Revision: "+saved.getRevisionNumber(),
                Notification.NotificationType.GENERAL, id, "RFQ");

            // US 05 + US 13: Email revision notice to invited vendors
            emailService.sendRfqRevisionEmail(
                invite.getVendor().getEmail(),
                invite.getVendor().getCompanyName(),
                saved.getRfqNumber(),
                saved.getTitle(),
                saved.getRevisionNumber(),
                changeSummary,
                String.valueOf(saved.getDeadline()));
        });
        
        audit.log(actor.getId(),"USER",actor.getName(),"RFQ_UPDATED","RFQ",id,
            "RFQ updated, rev "+saved.getRevisionNumber()+". Changes: "+changes.toString());
        return toResponse(saved);
    }

    @Override
    public RfqResponse getById(Long id) {
        return toResponse(rfqRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("RFQ","id",id)));
    }

    /**
     * US 12 AC #2: Filter RFQs by status (Open, Closed, Awarded, Archived)
     * US 12 AC #4: Multi-criteria filtering supported
     * US 12 AC #5: Results support pagination
     * US 12 AC #6: Sorting (ascending/descending) via Pageable
     * US 12 AC #7: Case-insensitive search (handled in repository)
     * US 12 AC #12: Performance optimized with indexed DB fields
     * 
     * Filters:
     * - Search: title, RFQ number, description (partial match)
     * - Status: OPEN, CLOSED, AWARDED, ARCHIVED
     * - Created by: User ID filter
     * - Deadline range: from/to dates
     * 
     * @param search Search term (can be null or empty)
     * @param status RFQ status filter (can be null)
     * @param createdById Creator user ID filter (can be null)
     * @param deadlineFrom Deadline from date (can be null)
     * @param deadlineTo Deadline to date (can be null)
     * @param pageable Pagination and sorting (page, size, sort)
     * @return Page of RFQ responses
     */
    @Override
    public Page<RfqResponse> getAll(String search, RFQ.RFQStatus status, Long createdById,
                                     LocalDateTime deadlineFrom, LocalDateTime deadlineTo, 
                                     Pageable pageable) {
        log.debug("[US 12] Searching RFQs - search: {}, status: {}, createdById: {}, " +
            "deadlineFrom: {}, deadlineTo: {}, page: {}, size: {}", 
            search, status, createdById, deadlineFrom, deadlineTo, 
            pageable.getPageNumber(), pageable.getPageSize());
        
        // US 12 AC #7: Trim search to handle empty strings
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        Page<RfqResponse> results = rfqRepo.searchRFQs(
            trimmedSearch, status, createdById, deadlineFrom, deadlineTo, pageable
        ).map(this::toResponse);
        
        log.debug("[US 12] Found {} RFQs (total: {}, pages: {})", 
            results.getNumberOfElements(), results.getTotalElements(), results.getTotalPages());
        
        return results;
    }

    @Override
    public Page<RfqResponse> getForVendor(Long vendorId, Pageable pageable) {
        return rfqRepo.findRFQsForVendor(vendorId, pageable).map(this::toResponse);
    }

    @Override @Transactional
    public RfqResponse inviteVendors(Long rfqId, List<Long> vendorIds, String actorEmail) {
        RFQ rfq = rfqRepo.findById(rfqId).orElseThrow(() -> new ResourceNotFoundException("RFQ","id",rfqId));
        User actor = userRepo.findByEmail(actorEmail).orElseThrow();
        vendorIds.forEach(vid -> {
            if (!inviteRepo.existsByRfqIdAndVendorId(rfqId, vid)) {
                vendorRepo.findById(vid).filter(v -> v.getStatus() == Vendor.VendorStatus.APPROVED)
                    .ifPresent(v -> {
                        inviteRepo.save(RfqVendorInvite.builder().rfq(rfq).vendor(v).build());
                        notif.send(v.getId(),"VENDOR","RFQ Invitation: "+rfq.getRfqNumber(),
                            "You've been invited to: "+rfq.getTitle(),
                            Notification.NotificationType.RFQ_ASSIGNED, rfqId, "RFQ");
                        
                        // US 13 AC #2: Email notification on RFQ assignment
                        log.info("US 13 AC #2: Sending RFQ assignment email to vendor: {}", v.getCompanyName());
                        emailService.sendRfqAssignmentEmail(
                            v.getEmail(),
                            v.getCompanyName(),
                            rfq.getRfqNumber(),
                            rfq.getTitle(),
                            rfq.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        );
                    });
            }
        });
        audit.log(actor.getId(),"USER",actor.getName(),"VENDORS_INVITED","RFQ",rfqId,"Invited vendors to RFQ");
        return toResponse(rfqRepo.findById(rfqId).get());
    }

    @Override @Transactional
    public RfqResponse close(Long id, String actorEmail) {
        RFQ rfq = rfqRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("RFQ","id",id));
        User actor = userRepo.findByEmail(actorEmail).orElseThrow();
        rfq.setStatus(RFQ.RFQStatus.CLOSED);
        RFQ saved = rfqRepo.save(rfq);
        audit.log(actor.getId(),"USER",actor.getName(),"RFQ_CLOSED","RFQ",id,"Manually closed by "+actor.getName());
        return toResponse(saved);
    }

    /**
     * US 11 AC #6: Admin Override to Reopen Closed RFQ
     * 
     * This method allows administrators to reopen a CLOSED RFQ with a new deadline.
     * This is an exceptional operation that requires:
     * - ADMIN role (enforced in controller via @PreAuthorize)
     * - Mandatory reason for audit trail
     * - New deadline must be in the future
     * - RFQ must be in CLOSED status (cannot reopen AWARDED or ARCHIVED RFQs)
     * 
     * BUSINESS RULES:
     * 1. Only ADMIN users can reopen RFQs (role-based access control)
     * 2. Reason is mandatory for transparency and audit compliance
     * 3. New deadline must be in the future (server-side validation)
     * 4. RFQ status transitions from CLOSED back to OPEN
     * 5. All invited vendors are notified about the reopening
     * 6. Audit log records the admin override action
     * 7. Cannot reopen AWARDED RFQs (business integrity)
     * 8. Cannot reopen ARCHIVED RFQs (historical data protection)
     * 
     * USE CASES:
     * - Insufficient quotations received before deadline
     * - Technical issues prevented vendor submissions
     * - Business requirement to extend submission window
     * - Correction of premature closure
     * 
     * @param rfqId The ID of the RFQ to reopen
     * @param newDeadline New submission deadline (must be in future)
     * @param reason Mandatory reason for reopening (min 20 characters)
     * @param actorEmail Email of the admin performing the override
     * @return Updated RFQ response with OPEN status
     * @throws ResourceNotFoundException if RFQ not found
     * @throws BusinessException if validation fails
     */
    @Override
    @Transactional
    public RfqResponse reopenRfq(Long rfqId, LocalDateTime newDeadline, String reason, String actorEmail) {
        log.info("[US 11 AC #6] Admin override: Reopening RFQ ID: {}", rfqId);
        
        // Step 1: Fetch RFQ and validate it exists
        RFQ rfq = rfqRepo.findById(rfqId)
            .orElseThrow(() -> new ResourceNotFoundException("RFQ", "id", rfqId));
        
        // Step 2: Fetch admin user (role validation in controller)
        User admin = userRepo.findByEmail(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", actorEmail));
        
        log.info("[US 11 AC #6] Admin {} attempting to reopen RFQ: {} (Current Status: {})", 
            admin.getName(), rfq.getRfqNumber(), rfq.getStatus());
        
        // Step 3: Validate RFQ is in CLOSED status
        if (rfq.getStatus() != RFQ.RFQStatus.CLOSED) {
            throw new BusinessException(
                String.format(
                    "Cannot reopen RFQ with status %s. Only CLOSED RFQs can be reopened. " +
                    "Current status: %s",
                    rfq.getStatus(),
                    rfq.getStatus()
                )
            );
        }
        
        // Step 4: Prevent reopening of AWARDED RFQs (business integrity)
        if (rfq.getStatus() == RFQ.RFQStatus.AWARDED) {
            throw new BusinessException(
                "Cannot reopen an AWARDED RFQ. RFQ was awarded to " + 
                rfq.getAwardedVendor().getCompanyName() + " on " + rfq.getAwardedAt() + ". " +
                "Awarded RFQs cannot be modified to maintain contract integrity."
            );
        }
        
        // Step 5: Validate reason is provided and meaningful
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(
                "Reason is mandatory for reopening an RFQ. " +
                "Please provide a detailed explanation for audit and transparency purposes."
            );
        }
        
        if (reason.trim().length() < 20) {
            throw new BusinessException(
                "Reason must be at least 20 characters for meaningful audit trail. " +
                "Current length: " + reason.trim().length()
            );
        }
        
        // Step 6: Validate new deadline is in the future (server-side validation)
        LocalDateTime serverTime = LocalDateTime.now();
        if (newDeadline.isBefore(serverTime) || newDeadline.isEqual(serverTime)) {
            throw new BusinessException(
                String.format(
                    "New deadline must be in the future. " +
                    "Provided deadline: %s, Server time: %s",
                    newDeadline,
                    serverTime
                )
            );
        }
        
        // Step 7: Validate new deadline is after original deadline (logical check)
        if (newDeadline.isBefore(rfq.getDeadline())) {
            log.warn("[US 11 AC #6] New deadline {} is before original deadline {}. " +
                "This is unusual but allowed.", newDeadline, rfq.getDeadline());
        }
        
        // Step 8: Store original deadline for audit
        LocalDateTime originalDeadline = rfq.getDeadline();
        RFQ.RFQStatus originalStatus = rfq.getStatus();
        
        // Step 9: Reopen RFQ - transition status from CLOSED to OPEN
        rfq.setStatus(RFQ.RFQStatus.OPEN);
        rfq.setDeadline(newDeadline);
        rfq.setRevisionNumber(rfq.getRevisionNumber() + 1);
        
        RFQ savedRfq = rfqRepo.save(rfq);
        
        log.info("[US 11 AC #6] Reopened RFQ {} - Status: {} -> OPEN, " +
            "Original Deadline: {}, New Deadline: {}, Revision: {}", 
            rfq.getRfqNumber(), originalStatus, originalDeadline, newDeadline, 
            savedRfq.getRevisionNumber());
        
        // Step 10: Create immutable audit log for admin override
        audit.log(
            admin.getId(),
            "USER",
            admin.getName(),
            "RFQ_REOPENED_ADMIN_OVERRIDE",
            "RFQ",
            rfqId,
            String.format(
                "ADMIN OVERRIDE: RFQ %s reopened by %s. " +
                "Original Status: %s, New Status: OPEN. " +
                "Original Deadline: %s, New Deadline: %s. " +
                "Reason: %s. " +
                "Revision: %d. " +
                "Server Time: %s",
                rfq.getRfqNumber(),
                admin.getName(),
                originalStatus,
                originalDeadline,
                newDeadline,
                reason,
                savedRfq.getRevisionNumber(),
                serverTime
            )
        );
        
        log.info("[US 11 AC #6] Created audit log for RFQ reopening");
        
        // Step 11: Notify all invited vendors about reopening
        List<RfqVendorInvite> invites = inviteRepo.findByRfqId(rfqId);
        
        for (RfqVendorInvite invite : invites) {
            Vendor vendor = invite.getVendor();
            
            // Check if vendor already submitted
            boolean hasSubmitted = invite.isHasResponded();
            
            String message = String.format(
                "RFQ '%s' has been REOPENED by administration. " +
                "New submission deadline: %s. " +
                "%s " +
                "Reason for reopening: %s",
                rfq.getTitle(),
                newDeadline,
                hasSubmitted 
                    ? "You may update your existing quotation or keep it as is." 
                    : "You now have an opportunity to submit a quotation.",
                reason
            );
            
            notif.send(
                vendor.getId(),
                "VENDOR",
                "🔓 RFQ Reopened: " + rfq.getRfqNumber(),
                message,
                Notification.NotificationType.RFQ_ASSIGNED,
                rfqId,
                "RFQ"
            );
            
            log.debug("[US 11 AC #6] Notified vendor: {} (ID: {}, Previously Submitted: {})", 
                vendor.getCompanyName(), vendor.getId(), hasSubmitted);
        }
        
        log.info("[US 11 AC #6] Sent reopening notifications to {} vendor(s)", invites.size());
        
        // Step 12: Notify procurement managers about the reopening
        List<User> procurementManagers = userRepo.findByRole(User.Role.PROCUREMENT_MANAGER);
        
        for (User manager : procurementManagers) {
            notif.send(
                manager.getId(),
                "USER",
                "🔓 RFQ Reopened by Admin: " + rfq.getRfqNumber(),
                String.format(
                    "RFQ '%s' has been reopened by administrator %s. " +
                    "New deadline: %s. " +
                    "Reason: %s. " +
                    "Vendors have been notified and may submit or update quotations.",
                    rfq.getTitle(),
                    admin.getName(),
                    newDeadline,
                    reason
                ),
                Notification.NotificationType.GENERAL,
                rfqId,
                "RFQ"
            );
        }
        
        log.info("[US 11 AC #6] Notified {} procurement manager(s) about reopening", 
            procurementManagers.size());
        
        // Step 13: Save revision history for the reopening
        saveRevisionHistory(savedRfq, admin, "RFQ reopened by admin: " + reason);
        
        log.info("[US 11 AC #6] RFQ {} successfully reopened. New deadline: {}", 
            rfq.getRfqNumber(), newDeadline);
        
        return toResponse(savedRfq);
    }

    /**
     * US 08: Award RFQ fairly with comprehensive validation
     * 
     * Acceptance Criteria Implementation:
     * 1. Only one vendor selectable - enforced by single quotationId
     * 2. Award reason mandatory - validated in RfqAwardRequest
     * 3. RFQ status changes to "Awarded" - status updated
     * 4. Non-selected vendors notified - notifications sent
     * 5. Award timestamp stored - awardedAt field
     * 6. Award logged immutably - audit log created
     * 7. RFQ locked from edits - status check in update method
     * 8. Vendor performance updated - totalRfqsWon incremented
     * 9. Purchase Order generation enabled - status allows PO creation
     * 10. System prevents multiple awards - duplicate award check
     * 11. Award must require confirmation dialog - confirmed flag required
     * 12. Role-based validation enforced - @PreAuthorize in controller
     */
    @Override
    @Transactional
    public RfqResponse award(Long rfqId, RfqAwardRequest request, String actorEmail) {
        // Step 1: Validate confirmation flag (US 08 - Confirmation dialog requirement)
        if (request.getConfirmed() == null || !request.getConfirmed()) {
            throw new BusinessException("Award confirmation is required. Please confirm the award action.");
        }
        
        // Step 2: Fetch RFQ and validate it exists
        RFQ rfq = rfqRepo.findById(rfqId)
            .orElseThrow(() -> new ResourceNotFoundException("RFQ", "id", rfqId));
        
        // Step 3: Fetch actor (manager) and validate role (role validation in controller)
        User actor = userRepo.findByEmail(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", actorEmail));
        
        // Step 4: Prevent multiple awards (US 08 - System prevents multiple awards)
        if (rfq.getStatus() == RFQ.RFQStatus.AWARDED) {
            throw new BusinessException("RFQ has already been awarded to " + 
                rfq.getAwardedVendor().getCompanyName() + " on " + rfq.getAwardedAt() + 
                ". Multiple awards are not allowed.");
        }
        
        // Step 5: Validate RFQ is in a state that can be awarded
        if (rfq.getStatus() == RFQ.RFQStatus.ARCHIVED) {
            throw new BusinessException("Cannot award an archived RFQ.");
        }
        
        // Step 6: Validate award reason is provided (US 08 - Award reason mandatory)
        if (request.getAwardReason() == null || request.getAwardReason().trim().isEmpty()) {
            throw new BusinessException("Award reason is mandatory for transparency and audit purposes.");
        }
        
        if (request.getAwardReason().trim().length() < 10) {
            throw new BusinessException("Award reason must be at least 10 characters for meaningful documentation.");
        }
        
        // Step 7: Fetch the winning quotation (US 08 - Only one vendor selectable)
        Quotation winningQuotation = quotationRepo.findById(request.getQuotationId())
            .orElseThrow(() -> new ResourceNotFoundException("Quotation", "id", request.getQuotationId()));
        
        // Step 8: Validate quotation belongs to this RFQ
        if (!winningQuotation.getRfq().getId().equals(rfqId)) {
            throw new BusinessException("The selected quotation does not belong to this RFQ.");
        }
        
        // Step 9: Validate quotation is in submitted or evaluated state
        if (winningQuotation.getStatus() == Quotation.QuotationStatus.AWARDED) {
            throw new BusinessException("This quotation has already been awarded.");
        }
        
        if (winningQuotation.getStatus() == Quotation.QuotationStatus.REJECTED) {
            throw new BusinessException("Cannot award a rejected quotation.");
        }
        
        // Step 10: Validate vendor is approved and compliant
        Vendor winningVendor = winningQuotation.getVendor();
        if (winningVendor.getStatus() != Vendor.VendorStatus.APPROVED) {
            throw new BusinessException("Cannot award to a vendor that is not approved. Vendor status: " + 
                winningVendor.getStatus());
        }
        
        if (!winningVendor.isCompliant()) {
            throw new BusinessException("Cannot award to a non-compliant vendor. Please ensure vendor compliance documents are valid.");
        }
        
        log.info("US 08: Starting RFQ award process - RFQ: {}, Quotation: {}, Vendor: {}, Manager: {}", 
            rfq.getRfqNumber(), winningQuotation.getId(), winningVendor.getCompanyName(), actor.getName());
        
        // Step 11: Mark winning quotation as AWARDED
        winningQuotation.setStatus(Quotation.QuotationStatus.AWARDED);
        winningQuotation.setAwarded(true);
        winningQuotation.setEvaluatedAt(LocalDateTime.now());
        quotationRepo.save(winningQuotation);
        
        log.info("US 08: Marked quotation {} as AWARDED", winningQuotation.getId());
        
        // Step 12: Reject all other quotations and notify vendors (US 08 - Non-selected vendors notified)
        List<Quotation> otherQuotations = quotationRepo.findByRfqId(rfqId).stream()
            .filter(q -> !q.getId().equals(request.getQuotationId()))
            .collect(Collectors.toList());
        
        log.info("US 08: Rejecting {} other quotations and notifying vendors", otherQuotations.size());
        
        for (Quotation otherQuotation : otherQuotations) {
            // Reject quotation
            otherQuotation.setStatus(Quotation.QuotationStatus.REJECTED);
            otherQuotation.setEvaluationComment("RFQ awarded to another vendor");
            otherQuotation.setEvaluatedAt(LocalDateTime.now());
            quotationRepo.save(otherQuotation);
            
            // Notify non-selected vendor (US 08 - Non-selected vendors notified)
            notif.send(
                otherQuotation.getVendor().getId(),
                "VENDOR",
                "RFQ Award Notification - " + rfq.getRfqNumber(),
                "Thank you for your quotation submission for '" + rfq.getTitle() + "'. " +
                "After careful evaluation, the RFQ has been awarded to another vendor. " +
                "We appreciate your participation and look forward to future opportunities.",
                Notification.NotificationType.RFQ_AWARDED,
                rfqId,
                "RFQ"
            );
            
            // US 13 AC #3: Email notification on RFQ award (non-selected vendors)
            log.info("US 13 AC #3: Sending RFQ non-selection email to vendor: {}", 
                otherQuotation.getVendor().getCompanyName());
            emailService.sendRfqNonSelectionEmail(
                otherQuotation.getVendor().getEmail(),
                otherQuotation.getVendor().getCompanyName(),
                rfq.getRfqNumber(),
                rfq.getTitle()
            );
            
            log.info("US 08: Notified vendor {} about non-selection", otherQuotation.getVendor().getCompanyName());
        }
        
        // Step 13: Update RFQ status to AWARDED (US 08 - RFQ status changes to "Awarded")
        rfq.setStatus(RFQ.RFQStatus.AWARDED);
        rfq.setAwardedVendor(winningVendor);
        rfq.setAwardReason(request.getAwardReason());
        rfq.setAwardedAt(LocalDateTime.now()); // US 08 - Award timestamp stored
        
        RFQ savedRfq = rfqRepo.save(rfq);
        
        log.info("US 08: Updated RFQ {} status to AWARDED, locked from further edits", rfq.getRfqNumber());
        
        // Step 14: Notify winning vendor
        notif.send(
            winningVendor.getId(),
            "VENDOR",
            "🏆 Congratulations! RFQ Awarded - " + rfq.getRfqNumber(),
            "Congratulations! Your quotation for '" + rfq.getTitle() + "' has been selected. " +
            "Award Reason: " + request.getAwardReason() + ". " +
            "A Purchase Order will be generated shortly. Please check your dashboard for next steps.",
            Notification.NotificationType.RFQ_AWARDED,
            rfqId,
            "RFQ"
        );
        
        // US 13 AC #3: Email notification on RFQ award (winning vendor)
        log.info("US 13 AC #3: Sending RFQ award email to winning vendor: {}", winningVendor.getCompanyName());
        emailService.sendRfqAwardEmail(
            winningVendor.getEmail(),
            winningVendor.getCompanyName(),
            rfq.getRfqNumber(),
            rfq.getTitle(),
            request.getAwardReason()
        );
        
        log.info("US 08: Notified winning vendor {} about award", winningVendor.getCompanyName());
        
        // Step 15: Update vendor performance metrics (US 08 - Vendor performance updated)
        int previousWins = winningVendor.getTotalRfqsWon();
        winningVendor.setTotalRfqsWon(previousWins + 1);
        
        // Calculate and update performance score
        int totalParticipated = winningVendor.getTotalRfqsParticipated();
        if (totalParticipated > 0) {
            double winRate = (double) winningVendor.getTotalRfqsWon() / totalParticipated;
            double newPerformanceScore = Math.min(100.0, winRate * 100);
            winningVendor.setPerformanceScore(newPerformanceScore);
        }
        
        vendorRepo.save(winningVendor);
        
        log.info("US 08: Updated vendor {} performance - Total Wins: {} (was {}), Performance Score: {}", 
            winningVendor.getCompanyName(), winningVendor.getTotalRfqsWon(), previousWins, 
            winningVendor.getPerformanceScore());
        
        // Step 16: Create immutable audit log (US 08 - Award logged immutably)
        audit.log(
            actor.getId(),
            "USER",
            actor.getName(),
            "RFQ_AWARDED",
            "RFQ",
            rfqId,
            "RFQ " + rfq.getRfqNumber() + " awarded to vendor: " + winningVendor.getCompanyName() + 
            " (ID: " + winningVendor.getId() + "). " +
            "Quotation ID: " + winningQuotation.getId() + ". " +
            "Award Amount: " + winningQuotation.getTotalAmount() + " " + winningQuotation.getCurrency() + ". " +
            "Award Reason: " + request.getAwardReason() + ". " +
            "Awarded At: " + rfq.getAwardedAt() + ". " +
            "Other quotations rejected: " + otherQuotations.size()
        );
        
        log.info("US 08: Created immutable audit log for RFQ award");
        
        // Step 17: Notify procurement team about successful award
        notif.send(
            actor.getId(),
            "USER",
            "RFQ Award Confirmed - " + rfq.getRfqNumber(),
            "RFQ '" + rfq.getTitle() + "' has been successfully awarded to " + 
            winningVendor.getCompanyName() + ". Purchase Order generation is now enabled.",
            Notification.NotificationType.RFQ_AWARDED,
            rfqId,
            "RFQ"
        );
        
        log.info("US 08: RFQ award process completed successfully for RFQ {}", rfq.getRfqNumber());
        
        // Step 18: Return updated RFQ response (US 08 - Purchase Order generation enabled)
        return toResponse(savedRfq);
    }

    /**
     * ============================================================================
     * US 11: Automatic RFQ Closure After Deadline
     * ============================================================================
     * 
     * This method implements all 12 acceptance criteria for automatic RFQ closure:
     * 
     * US 11 AC #1: System must automatically transition RFQ status from "Open" to "Closed" 
     *              once deadline timestamp is reached.
     * US 11 AC #2: Closure must be executed via scheduled background job.
     * US 11 AC #3: Late quotation submissions must return HTTP 400 with appropriate message.
     *              (Implemented in QuotationServiceImpl.submit() method)
     * US 11 AC #4: Vendors must visually see RFQ marked as "Closed."
     *              (Implemented in frontend RFQ display components)
     * US 11 AC #5: Closure events must be recorded in audit log.
     * US 11 AC #6: System must prevent reopening of RFQ unless Admin overrides.
     *              (Implemented via status validation in update/reopen methods)
     * US 11 AC #7: Closed RFQs must not allow edits.
     *              (Already implemented in update() method - checks status)
     * US 11 AC #8: Dashboard metrics must reflect closure immediately.
     *              (Dashboard queries RFQ status in real-time)
     * US 11 AC #9: Deadline comparison must use server-side time validation.
     * US 11 AC #10: Grace period configuration (if enabled) must be enforced.
     * US 11 AC #11: Notification must be sent to Procurement Manager upon closure.
     * US 11 AC #12: System must log any failed scheduler execution attempts.
     *               (Implemented in SvpmsScheduler.java with try-catch)
     * 
     * EXECUTION FLOW:
     * 1. Fetch grace period configuration from application properties
     * 2. Calculate effective deadline with grace period
     * 3. Use server-side time for deadline comparison (US 11 AC #9)
     * 4. Query database for expired OPEN RFQs
     * 5. For each expired RFQ:
     *    a. Transition status from OPEN to CLOSED (US 11 AC #1)
     *    b. Record closure timestamp
     *    c. Save RFQ to database
     *    d. Create immutable audit log entry (US 11 AC #5)
     *    e. Send notification to Procurement Manager (US 11 AC #11)
     *    f. Send notification to all invited vendors
     * 6. Log summary of closure operation
     * 
     * SCHEDULER CONFIGURATION:
     * - Cron expression: ${scheduler.rfq.close-cron:0 0 * * * *} (hourly by default)
     * - Configured in application.properties
     * - Executed by SvpmsScheduler.autoCloseExpiredRFQs() (US 11 AC #2)
     * 
     * GRACE PERIOD:
     * - Configurable via ${rfq.closure.grace-period-minutes:0} property
     * - Default: 0 minutes (no grace period)
     * - Example: Set to 30 for 30-minute grace period after deadline
     * - Grace period allows late submissions within configured window (US 11 AC #10)
     * 
     * ERROR HANDLING:
     * - All exceptions are caught and logged in SvpmsScheduler
     * - Failed closures are logged with ERROR level (US 11 AC #12)
     * - Transaction rollback ensures data consistency
     * 
     * RELATED VALIDATIONS:
     * - QuotationServiceImpl.submit() validates deadline before accepting submissions (US 11 AC #3)
     * - RfqServiceImpl.update() prevents editing CLOSED RFQs (US 11 AC #7)
     * - Admin override for reopening requires ADMIN role (US 11 AC #6)
     * 
     * @see SvpmsScheduler#autoCloseExpiredRFQs() - Scheduler entry point
     * @see QuotationServiceImpl#submit() - Late submission validation
     * @see #update() - Edit prevention for closed RFQs
     * @see #reopenRfq() - Admin override functionality
     */
    @Override
    @Transactional
    public void autoCloseExpired() {
        // US 11 AC #9: Use server-side time for deadline comparison
        LocalDateTime serverTime = LocalDateTime.now();
        
        log.info("[US 11] Starting automatic RFQ closure process at server time: {}", serverTime);
        
        // US 11 AC #10: Fetch grace period configuration (if enabled)
        // Grace period allows submissions slightly after deadline
        // Configured in application.properties: rfq.closure.grace-period-minutes
        // Injected via @Value annotation
        log.debug("[US 11] Grace period: {} minutes (from configuration)", gracePeriodMinutes);
        
        // US 11 AC #10: Calculate effective deadline with grace period
        LocalDateTime effectiveDeadline = serverTime.minusMinutes(gracePeriodMinutes);
        
        log.info("[US 11] Effective deadline for closure: {} (grace period: {} minutes)", 
            effectiveDeadline, gracePeriodMinutes);
        
        // US 11 AC #1: Query for RFQs with status=OPEN and deadline <= effective deadline
        // This ensures only OPEN RFQs past their deadline are closed
        List<RFQ> expiredRfqs = rfqRepo.findExpiredOpenRFQs(effectiveDeadline);
        
        if (expiredRfqs.isEmpty()) {
            log.info("[US 11] No expired RFQs found for closure");
            return;
        }
        
        log.info("[US 11] Found {} expired RFQ(s) to close", expiredRfqs.size());
        
        // Track closure statistics
        int successCount = 0;
        int failureCount = 0;
        
        // US 11 AC #1: Process each expired RFQ
        for (RFQ rfq : expiredRfqs) {
            try {
                log.info("[US 11] Processing RFQ: {} (ID: {}, Deadline: {})", 
                    rfq.getRfqNumber(), rfq.getId(), rfq.getDeadline());
                
                // Step 1: Validate RFQ is in OPEN status (double-check)
                if (rfq.getStatus() != RFQ.RFQStatus.OPEN) {
                    log.warn("[US 11] Skipping RFQ {} - Status is {} (expected OPEN)", 
                        rfq.getRfqNumber(), rfq.getStatus());
                    continue;
                }
                
                // Step 2: US 11 AC #1 - Transition status from OPEN to CLOSED
                RFQ.RFQStatus previousStatus = rfq.getStatus();
                rfq.setStatus(RFQ.RFQStatus.CLOSED);
                
                log.info("[US 11] Transitioned RFQ {} status: {} -> CLOSED", 
                    rfq.getRfqNumber(), previousStatus);
                
                // Step 3: Record closure timestamp for audit trail
                // Note: updatedAt is automatically set by JPA @PreUpdate
                
                // Step 4: Save RFQ to database (US 11 AC #8 - Dashboard reflects immediately)
                RFQ savedRfq = rfqRepo.save(rfq);
                
                log.info("[US 11] Saved RFQ {} with CLOSED status to database", rfq.getRfqNumber());
                
                // Step 5: US 11 AC #5 - Create immutable audit log entry
                // Audit log records: who (SYSTEM), what (RFQ_AUTO_CLOSED), when (timestamp), why (deadline passed)
                audit.log(
                    0L,  // System actor (ID = 0)
                    "SYSTEM",  // Actor type
                    "SCHEDULER",  // Actor name
                    "RFQ_AUTO_CLOSED",  // Action type
                    "RFQ",  // Entity type
                    rfq.getId(),  // Entity ID
                    String.format(
                        "RFQ %s automatically closed by scheduler. " +
                        "Deadline: %s, Closed At: %s, Grace Period: %d minutes, " +
                        "Server Time: %s, Quotations Received: %d",
                        rfq.getRfqNumber(),
                        rfq.getDeadline(),
                        serverTime,
                        gracePeriodMinutes,
                        serverTime,
                        rfq.getQuotations() != null ? rfq.getQuotations().size() : 0
                    )
                );
                
                log.info("[US 11] Created audit log entry for RFQ {}", rfq.getRfqNumber());
                
                // Step 6: US 11 AC #11 - Send notification to Procurement Manager(s)
                // Notify all users with PROCUREMENT_MANAGER role about the closure
                List<User> procurementManagers = userRepo.findByRole(User.Role.PROCUREMENT_MANAGER);
                
                int quotationCount = rfq.getQuotations() != null ? rfq.getQuotations().size() : 0;
                for (User manager : procurementManagers) {
                    notif.send(
                        manager.getId(),
                        "USER",
                        "⏰ RFQ Auto-Closed: " + rfq.getRfqNumber(),
                        String.format(
                            "RFQ '%s' has been automatically closed after its deadline (%s). " +
                            "Total quotations received: %d. " +
                            "Please review and proceed with evaluation if quotations are available.",
                            rfq.getTitle(),
                            rfq.getDeadline(),
                            quotationCount
                        ),
                        Notification.NotificationType.RFQ_CLOSED,
                        rfq.getId(),
                        "RFQ"
                    );

                    // US 11 AC #11: Email notification to Procurement Manager upon closure
                    emailService.sendRfqClosureEmail(
                        manager.getEmail(),
                        manager.getName(),
                        rfq.getRfqNumber(),
                        rfq.getTitle(),
                        String.valueOf(serverTime),
                        quotationCount);

                    log.debug("[US 11] Notified Procurement Manager: {} (ID: {})",
                        manager.getName(), manager.getId());
                }
                
                log.info("[US 11] Sent notifications to {} Procurement Manager(s)", 
                    procurementManagers.size());
                
                // Step 7: US 11 AC #4 - Notify all invited vendors about closure
                // Vendors need to know the RFQ is now closed and no further submissions are accepted
                List<RfqVendorInvite> invites = inviteRepo.findByRfqId(rfq.getId());
                
                for (RfqVendorInvite invite : invites) {
                    Vendor vendor = invite.getVendor();
                    
                    // Check if vendor submitted a quotation
                    boolean hasSubmitted = invite.isHasResponded();
                    
                    String message = hasSubmitted
                        ? String.format(
                            "RFQ '%s' has been closed after its deadline. " +
                            "Your quotation has been received and will be evaluated. " +
                            "No further modifications are allowed.",
                            rfq.getTitle()
                        )
                        : String.format(
                            "RFQ '%s' has been closed after its deadline (%s). " +
                            "The submission window is now closed. " +
                            "We look forward to your participation in future opportunities.",
                            rfq.getTitle(),
                            rfq.getDeadline()
                        );
                    
                    notif.send(
                        vendor.getId(),
                        "VENDOR",
                        "🔒 RFQ Closed: " + rfq.getRfqNumber(),
                        message,
                        Notification.NotificationType.RFQ_CLOSED,
                        rfq.getId(),
                        "RFQ"
                    );
                    
                    log.debug("[US 11] Notified vendor: {} (ID: {}, Submitted: {})", 
                        vendor.getCompanyName(), vendor.getId(), hasSubmitted);
                }
                
                log.info("[US 11] Sent notifications to {} invited vendor(s)", invites.size());
                
                // Step 8: Mark successful closure
                successCount++;
                
                log.info("[US 11] Successfully closed RFQ: {} (ID: {})", 
                    rfq.getRfqNumber(), rfq.getId());
                
            } catch (Exception e) {
                // US 11 AC #12: Log failed closure attempts
                failureCount++;
                
                log.error("[US 11] Failed to close RFQ: {} (ID: {}). Error: {}", 
                    rfq.getRfqNumber(), rfq.getId(), e.getMessage(), e);
                
                // Create audit log for failure
                try {
                    audit.log(
                        0L,
                        "SYSTEM",
                        "SCHEDULER",
                        "RFQ_AUTO_CLOSE_FAILED",
                        "RFQ",
                        rfq.getId(),
                        String.format(
                            "Failed to auto-close RFQ %s. Error: %s. " +
                            "Manual intervention may be required.",
                            rfq.getRfqNumber(),
                            e.getMessage()
                        )
                    );
                } catch (Exception auditException) {
                    log.error("[US 11] Failed to create audit log for closure failure: {}", 
                        auditException.getMessage());
                }
                
                // Continue processing other RFQs (don't let one failure stop the batch)
            }
        }
        
        // US 11 AC #12: Log summary of closure operation
        log.info("[US 11] RFQ auto-closure process completed. " +
            "Total: {}, Successful: {}, Failed: {}, Server Time: {}", 
            expiredRfqs.size(), successCount, failureCount, serverTime);
        
        // If there were failures, log a warning
        if (failureCount > 0) {
            log.warn("[US 11] {} RFQ(s) failed to close automatically. " +
                "Please review logs and take manual action if needed.", failureCount);
        }
    }

    @Override @Transactional
    public void uploadAttachment(Long rfqId, org.springframework.web.multipart.MultipartFile file, String actorEmail) {
        RFQ rfq = rfqRepo.findById(rfqId)
            .orElseThrow(() -> new ResourceNotFoundException("RFQ","id",rfqId));
        User actor = userRepo.findByEmail(actorEmail).orElseThrow();
        
        if (rfq.getStatus() != RFQ.RFQStatus.OPEN) {
            throw new BusinessException("Can only upload attachments to OPEN RFQs");
        }
        
        // US 05: Attachment versioning - mark old attachments with same name as inactive
        List<RfqAttachment> existingAttachments = attachmentRepo.findByRfqId(rfqId);
        existingAttachments.stream()
            .filter(att -> att.getFileName().equals(file.getOriginalFilename()) && att.isActive())
            .forEach(att -> {
                att.setActive(false);
                attachmentRepo.save(att);
                log.info("Marked attachment {} as inactive for versioning", att.getFileName());
            });
        
        String fileName = java.util.UUID.randomUUID() + "_" + file.getOriginalFilename();
        java.nio.file.Path path = java.nio.file.Paths.get(uploadDir, "rfq", String.valueOf(rfqId));
        
        try {
            java.nio.file.Files.createDirectories(path);
            java.nio.file.Files.copy(file.getInputStream(), path.resolve(fileName), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to store attachment");
        }
        
        // Get next revision number for this attachment
        int nextRevision = existingAttachments.stream()
            .filter(att -> att.getFileName().equals(file.getOriginalFilename()))
            .mapToInt(RfqAttachment::getRevisionNumber)
            .max()
            .orElse(0) + 1;
        
        RfqAttachment attachment = RfqAttachment.builder()
            .rfq(rfq)
            .fileName(file.getOriginalFilename())
            .filePath(path.resolve(fileName).toString())
            .fileSize(file.getSize())
            .fileType(file.getContentType())
            .revisionNumber(nextRevision)
            .active(true)
            .build();
        
        attachmentRepo.save(attachment);
        audit.log(actor.getId(),"USER",actor.getName(),"RFQ_ATTACHMENT_UPLOADED","RFQ",rfqId,
            "Uploaded attachment: "+file.getOriginalFilename()+" (rev "+nextRevision+")");
    }

    @Override
    public List<Map<String, Object>> getAttachments(Long rfqId) {
        return attachmentRepo.findByRfqId(rfqId).stream()
            .filter(RfqAttachment::isActive) // Only return active attachments
            .map(att -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", att.getId());
                map.put("fileName", att.getFileName());
                map.put("fileSize", att.getFileSize());
                map.put("fileType", att.getFileType());
                map.put("revisionNumber", att.getRevisionNumber());
                map.put("uploadedAt", att.getUploadedAt());
                return map;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<RfqRevisionHistoryResponse> getRevisionHistory(Long rfqId) {
        rfqRepo.findById(rfqId).orElseThrow(() -> new ResourceNotFoundException("RFQ","id",rfqId));
        
        return revisionHistoryRepo.findByRfqIdOrderByRevisionNumberDesc(rfqId).stream()
            .map(this::toRevisionHistoryResponse)
            .collect(Collectors.toList());
    }

    @Override
    public RfqRevisionHistoryResponse getRevisionByNumber(Long rfqId, int revisionNumber) {
        rfqRepo.findById(rfqId).orElseThrow(() -> new ResourceNotFoundException("RFQ","id",rfqId));
        
        RfqRevisionHistory revision = revisionHistoryRepo.findByRfqIdAndRevisionNumber(rfqId, revisionNumber);
        if (revision == null) {
            throw new ResourceNotFoundException("RFQ Revision", "revisionNumber", (long) revisionNumber);
        }
        
        return toRevisionHistoryResponse(revision);
    }

    @Override
    public byte[] exportRfqRevision(Long rfqId, int revisionNumber) {
        RfqRevisionHistory revision = revisionHistoryRepo.findByRfqIdAndRevisionNumber(rfqId, revisionNumber);
        if (revision == null) {
            throw new ResourceNotFoundException("RFQ Revision", "revisionNumber", (long) revisionNumber);
        }
        
        StringBuilder export = new StringBuilder();
        export.append("RFQ REVISION EXPORT\n");
        export.append("===================\n\n");
        export.append("RFQ Number: ").append(revision.getRfqNumber()).append("\n");
        export.append("Revision: ").append(revision.getRevisionNumber()).append("\n");
        export.append("Title: ").append(revision.getTitle()).append("\n");
        export.append("Status: ").append(revision.getStatus()).append("\n");
        export.append("Deadline: ").append(revision.getDeadline()).append("\n");
        export.append("Modified By: ").append(revision.getModifiedByName()).append("\n");
        export.append("Modified At: ").append(revision.getCreatedAt()).append("\n\n");
        export.append("Description:\n").append(revision.getDescription()).append("\n\n");
        export.append("Terms:\n").append(revision.getTerms()).append("\n\n");
        export.append("Items:\n").append(revision.getItemsSnapshot()).append("\n\n");
        export.append("Change Description: ").append(revision.getChangeDescription()).append("\n");
        
        return export.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // Helper method to save revision history
    private void saveRevisionHistory(RFQ rfq, User actor, String changeDescription) {
        try {
            // Create JSON snapshot of items
            StringBuilder itemsJson = new StringBuilder("[");
            if (rfq.getItems() != null) {
                itemsJson.append(rfq.getItems().stream()
                    .map(item -> String.format("{\"itemName\":\"%s\",\"quantity\":%d,\"unit\":\"%s\"}",
                        item.getItemName(), item.getQuantity(), item.getUnit()))
                    .collect(Collectors.joining(",")));
            }
            itemsJson.append("]");
            
            RfqRevisionHistory history = RfqRevisionHistory.builder()
                .rfqId(rfq.getId())
                .rfqNumber(rfq.getRfqNumber())
                .revisionNumber(rfq.getRevisionNumber())
                .title(rfq.getTitle())
                .description(rfq.getDescription())
                .terms(rfq.getTerms())
                .deadline(rfq.getDeadline())
                .status(rfq.getStatus())
                .itemsSnapshot(itemsJson.toString())
                .modifiedBy(actor.getId())
                .modifiedByName(actor.getName())
                .changeDescription(changeDescription)
                .build();
            
            revisionHistoryRepo.save(history);
            log.info("Saved revision history for RFQ {} rev {}", rfq.getRfqNumber(), rfq.getRevisionNumber());
        } catch (Exception e) {
            log.error("Failed to save revision history: {}", e.getMessage());
            // Don't fail the main operation if revision history fails
        }
    }

    private RfqRevisionHistoryResponse toRevisionHistoryResponse(RfqRevisionHistory r) {
        // Parse items from JSON snapshot
        List<RfqItemResponse> items = new ArrayList<>();
        try {
            String itemsJson = r.getItemsSnapshot();
            if (itemsJson != null && !itemsJson.equals("[]")) {
                // Simple JSON parsing for items
                itemsJson = itemsJson.substring(1, itemsJson.length() - 1); // Remove [ ]
                if (!itemsJson.isEmpty()) {
                    String[] itemArray = itemsJson.split("\\},\\{");
                    for (String itemStr : itemArray) {
                        itemStr = itemStr.replace("{", "").replace("}", "");
                        String[] fields = itemStr.split(",");
                        RfqItemResponse item = RfqItemResponse.builder()
                            .itemName(extractJsonValue(fields[0]))
                            .quantity(Integer.parseInt(extractJsonValue(fields[1])))
                            .unit(extractJsonValue(fields[2]))
                            .build();
                        items.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse items snapshot: {}", e.getMessage());
        }
        
        return RfqRevisionHistoryResponse.builder()
            .id(r.getId())
            .rfqId(r.getRfqId())
            .rfqNumber(r.getRfqNumber())
            .revisionNumber(r.getRevisionNumber())
            .title(r.getTitle())
            .description(r.getDescription())
            .terms(r.getTerms())
            .deadline(r.getDeadline())
            .status(r.getStatus())
            .items(items)
            .modifiedByName(r.getModifiedByName())
            .changeDescription(r.getChangeDescription())
            .createdAt(r.getCreatedAt())
            .build();
    }

    private String extractJsonValue(String field) {
        String[] parts = field.split(":");
        if (parts.length > 1) {
            return parts[1].replace("\"", "").trim();
        }
        return "";
    }

    private RfqResponse toResponse(RFQ r) {
        List<VendorResponse> invited = inviteRepo.findByRfqId(r.getId()).stream().map(inv -> {
            Vendor v = inv.getVendor();
            return VendorResponse.builder().id(v.getId()).companyName(v.getCompanyName())
                .email(v.getEmail()).gstNumber(v.getGstNumber()).status(v.getStatus()).build();
        }).collect(Collectors.toList());

        return RfqResponse.builder()
            .id(r.getId()).rfqNumber(r.getRfqNumber()).title(r.getTitle())
            .description(r.getDescription()).terms(r.getTerms()).deadline(r.getDeadline())
            .status(r.getStatus()).revisionNumber(r.getRevisionNumber())
            .createdByName(r.getCreatedBy() != null ? r.getCreatedBy().getName() : null)
            .createdById(r.getCreatedBy() != null ? r.getCreatedBy().getId() : null)
            .awardedVendorName(r.getAwardedVendor() != null ? r.getAwardedVendor().getCompanyName() : null)
            .awardedVendorId(r.getAwardedVendor() != null ? r.getAwardedVendor().getId() : null)
            .awardReason(r.getAwardReason()).awardedAt(r.getAwardedAt())
            .items(r.getItems() == null ? List.of() : r.getItems().stream().map(i ->
                RfqItemResponse.builder().id(i.getId()).itemName(i.getItemName())
                .description(i.getDescription()).quantity(i.getQuantity())
                .unit(i.getUnit()).specifications(i.getSpecifications()).build()
            ).collect(Collectors.toList()))
            .invitedVendors(invited)
            .quotationCount(r.getQuotations() != null ? r.getQuotations().size() : 0)
            .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
            .build();
    }
}
