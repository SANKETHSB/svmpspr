package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.request.VendorApprovalRequest;
import com.infosys.svpms.dto.request.VendorRegisterRequest;
import com.infosys.svpms.dto.response.VendorApprovalHistoryResponse;
import com.infosys.svpms.dto.response.VendorResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorRepository repo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final NotificationService notif;
    private final EmailService emailService;
    private final VendorApprovalHistoryRepository historyRepo;

    @Override @Transactional
    public VendorResponse register(VendorRegisterRequest req) {
        // Duplicate detection
        if (repo.existsByEmail(req.getEmail())) 
            throw new DuplicateException("Email already registered: " + req.getEmail());
        if (repo.existsByGstNumber(req.getGstNumber())) 
            throw new DuplicateException("GST already registered: " + req.getGstNumber());
        if (repo.existsByRegistrationId(req.getRegistrationId())) 
            throw new DuplicateException("Registration ID already used.");
        
        // Create vendor
        Vendor v = Vendor.builder()
            .companyName(req.getCompanyName()).email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .gstNumber(req.getGstNumber()).registrationId(req.getRegistrationId())
            .phone(req.getPhone()).address(req.getAddress())
            .contactPerson(req.getContactPerson())
            .status(Vendor.VendorStatus.PENDING_APPROVAL)
            .emailVerified(false)
            .build();
        Vendor saved = repo.save(v);

        // US 02 AC: Email verification link must be sent
        String verificationToken = java.util.UUID.randomUUID().toString();
        String verificationLink = "http://localhost:3000/verify-email?token=" + verificationToken + "&email=" + saved.getEmail();
        emailService.sendVendorEmailVerification(saved.getEmail(), saved.getCompanyName(), verificationLink, verificationToken);

        // US 02 AC: All submissions logged in audit trail
        audit.log(saved.getId(),"VENDOR",saved.getCompanyName(),"VENDOR_REGISTERED","Vendor",saved.getId(),
            "New vendor registered - awaiting approval (token=" + verificationToken + ")");

        // US 02 AC: Admin notified upon submission (in-app + email)
        notifyAdminsOfNewVendor(saved);

        return toResponse(saved);
    }

    private void notifyAdminsOfNewVendor(Vendor vendor) {
        List<User> admins = userRepo.findByRole(User.Role.ADMIN);
        for (User admin : admins) {
            // In-app notification (also mirrored to console by NotificationServiceImpl)
            notif.send(admin.getId(), "USER",
                "New Vendor Registration",
                "New vendor '" + vendor.getCompanyName() + "' has registered and is awaiting approval.",
                Notification.NotificationType.GENERAL,
                vendor.getId(), "Vendor");

            // US 02 AC: Email to admin
            emailService.sendAdminNewVendorRegistration(
                admin.getEmail(), admin.getName(),
                vendor.getCompanyName(), vendor.getGstNumber(),
                vendor.getRegistrationId(), vendor.getId());
        }
    }

    @Override @Transactional
    public VendorResponse approve(Long id, String actorEmail) {
        Vendor v = getVendorOrThrow(id);
        
        // Status transition rules
        if (v.getStatus() != Vendor.VendorStatus.PENDING_APPROVAL && 
            v.getStatus() != Vendor.VendorStatus.REJECTED)
            throw new BusinessException("Vendor can only be approved from PENDING_APPROVAL or REJECTED status. Current status: " + v.getStatus());
        
        User actor = userRepo.findByEmail(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", actorEmail));
        
        Vendor.VendorStatus previousStatus = v.getStatus();
        
        // Calculate approval time metrics
        long approvalTimeMinutes = java.time.Duration.between(v.getRegisteredAt(), LocalDateTime.now()).toMinutes();
        
        // Update vendor status
        v.setStatus(Vendor.VendorStatus.APPROVED);
        v.setApprovedAt(LocalDateTime.now());
        v.setApprovedBy(actor);
        v.setRejectionReason(null); // Clear rejection reason if re-approving
        Vendor saved = repo.save(v);
        
        // Record approval history
        VendorApprovalHistory history = VendorApprovalHistory.builder()
            .vendor(saved)
            .actor(actor)
            .previousStatus(previousStatus)
            .newStatus(Vendor.VendorStatus.APPROVED)
            .comments("Vendor approved after compliance verification")
            .approvalTimeMinutes(approvalTimeMinutes)
            .build();
        historyRepo.save(history);
        
        // Send notification
        notif.send(saved.getId(),"VENDOR","Vendor Registration Approved",
            "Congratulations! Your vendor registration has been approved. You can now participate in RFQs.",
            Notification.NotificationType.VENDOR_APPROVED, id, "Vendor");
        
        // US 13 AC #1: Email notification on vendor approval
        emailService.sendVendorApprovalEmail(saved.getEmail(), saved.getCompanyName(), saved.getId());
        
        // Audit log
        audit.log(actor.getId(),"USER",actor.getName(),"VENDOR_APPROVED","Vendor",id,
            "Vendor approved by "+actor.getName()+" (Approval time: "+approvalTimeMinutes+" minutes)");
        
        log.info("[US 13 AC #1] Vendor approval email triggered for: {}", saved.getEmail());
        
        return toResponse(saved);
    }

    @Override @Transactional
    public VendorResponse reject(Long id, String reason, String actorEmail) {
        Vendor v = getVendorOrThrow(id);
        
        // Validation: Reason is mandatory
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Rejection reason is mandatory");
        }
        
        // Status transition rules
        if (v.getStatus() == Vendor.VendorStatus.APPROVED) {
            throw new BusinessException("Cannot reject an APPROVED vendor. Use suspend instead.");
        }
        
        User actor = userRepo.findByEmail(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", actorEmail));
        
        Vendor.VendorStatus previousStatus = v.getStatus();
        
        // Calculate time metrics
        long processingTimeMinutes = java.time.Duration.between(v.getRegisteredAt(), LocalDateTime.now()).toMinutes();
        
        // Update vendor status
        v.setStatus(Vendor.VendorStatus.REJECTED);
        v.setRejectionReason(reason);
        Vendor saved = repo.save(v);
        
        // Record rejection history
        VendorApprovalHistory history = VendorApprovalHistory.builder()
            .vendor(saved)
            .actor(actor)
            .previousStatus(previousStatus)
            .newStatus(Vendor.VendorStatus.REJECTED)
            .reason(reason)
            .comments("Vendor rejected after review")
            .approvalTimeMinutes(processingTimeMinutes)
            .build();
        historyRepo.save(history);
        
        // Send notification
        notif.send(saved.getId(),"VENDOR","Vendor Registration Rejected",
            "Your registration was rejected. Reason: " + reason,
            Notification.NotificationType.VENDOR_REJECTED, id, "Vendor");
        
        // US 13: Email notification on vendor rejection
        emailService.sendVendorRejectionEmail(saved.getEmail(), saved.getCompanyName(), reason);
        
        // Audit log
        audit.log(actor.getId(),"USER",actor.getName(),"VENDOR_REJECTED","Vendor",id,"Rejected: "+reason);
        
        log.info("[US 13] Vendor rejection email triggered for: {}", saved.getEmail());
        
        return toResponse(saved);
    }

    @Override @Transactional
    public VendorResponse suspend(Long id, String reason, String actorEmail) {
        Vendor v = getVendorOrThrow(id);
        
        // Validation: Reason is mandatory
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Suspension reason is mandatory");
        }
        
        // Status transition rules: Can only suspend APPROVED vendors
        if (v.getStatus() != Vendor.VendorStatus.APPROVED) {
            throw new BusinessException("Can only suspend APPROVED vendors. Current status: " + v.getStatus());
        }
        
        User actor = userRepo.findByEmail(actorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", actorEmail));
        
        Vendor.VendorStatus previousStatus = v.getStatus();
        
        // Update vendor status
        v.setStatus(Vendor.VendorStatus.SUSPENDED);
        v.setRejectionReason(reason);
        Vendor saved = repo.save(v);
        
        // Record suspension history
        VendorApprovalHistory history = VendorApprovalHistory.builder()
            .vendor(saved)
            .actor(actor)
            .previousStatus(previousStatus)
            .newStatus(Vendor.VendorStatus.SUSPENDED)
            .reason(reason)
            .comments("Vendor suspended")
            .approvalTimeMinutes(0L)
            .build();
        historyRepo.save(history);
        
        // Send notification
        notif.send(saved.getId(),"VENDOR","Vendor Account Suspended",
            "Your vendor account has been suspended. Reason: " + reason,
            Notification.NotificationType.GENERAL, id, "Vendor");
        
        // Audit log
        audit.log(actor.getId(),"USER",actor.getName(),"VENDOR_SUSPENDED","Vendor",id,"Suspended: "+reason);
        
        return toResponse(saved);
    }

    @Override
    public VendorResponse getById(Long id) { return toResponse(getVendorOrThrow(id)); }

    /**
     * US 12 AC #1: Search vendors by name, GST, registration ID
     * US 12 AC #4: Multi-criteria filtering supported
     * US 12 AC #5: Results support pagination
     * US 12 AC #6: Sorting (ascending/descending) via Pageable
     * US 12 AC #7: Case-insensitive search (handled in repository)
     * US 12 AC #12: Performance optimized with indexed DB fields
     * 
     * Filters:
     * - Search: company name, GST number, registration ID (partial match)
     * - Status: PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED
     * - Compliant: true/false/null (compliance document status)
     * 
     * @param search Search term (can be null or empty)
     * @param status Vendor status filter (can be null)
     * @param compliant Compliance status filter (can be null)
     * @param pageable Pagination and sorting (page, size, sort)
     * @return Page of vendor responses
     */
    @Override
    public Page<VendorResponse> getAll(String search, Vendor.VendorStatus status, Boolean compliant, Pageable pageable) {
        log.debug("[US 12] Searching vendors - search: {}, status: {}, compliant: {}, page: {}, size: {}", 
            search, status, compliant, pageable.getPageNumber(), pageable.getPageSize());
        
        // US 12 AC #7: Trim search to handle empty strings
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        Page<VendorResponse> results = repo.searchVendors(trimmedSearch, status, compliant, pageable)
            .map(this::toResponse);
        
        log.debug("[US 12] Found {} vendors (total: {}, pages: {})", 
            results.getNumberOfElements(), results.getTotalElements(), results.getTotalPages());
        
        return results;
    }

    @Override
    public List<VendorResponse> getApproved() {
        return repo.findByStatus(Vendor.VendorStatus.APPROVED).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public VendorResponse getMyProfile(String email) {
        return toResponse(repo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Vendor","email",email)));
    }

    @Override
    public Vendor findByEmail(String email) {
        return repo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Vendor","email",email));
    }

    @Override
    public List<VendorApprovalHistoryResponse> getApprovalHistory(Long vendorId) {
        return historyRepo.findByVendorIdOrderByActionTimestampDesc(vendorId)
            .stream()
            .map(this::toHistoryResponse)
            .collect(Collectors.toList());
    }

    private Vendor getVendorOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor","id",id));
    }

    private VendorResponse toResponse(Vendor v) {
        return VendorResponse.builder()
            .id(v.getId()).companyName(v.getCompanyName()).email(v.getEmail())
            .gstNumber(v.getGstNumber()).registrationId(v.getRegistrationId())
            .phone(v.getPhone()).address(v.getAddress()).contactPerson(v.getContactPerson())
            .status(v.getStatus()).emailVerified(v.isEmailVerified()).compliant(v.isCompliant())
            .performanceScore(v.getPerformanceScore()).totalRfqsWon(v.getTotalRfqsWon())
            .totalRfqsParticipated(v.getTotalRfqsParticipated())
            .rejectionReason(v.getRejectionReason())
            .approvedByName(v.getApprovedBy() != null ? v.getApprovedBy().getName() : null)
            .approvedAt(v.getApprovedAt()).registeredAt(v.getRegisteredAt())
            .build();
    }

    private VendorApprovalHistoryResponse toHistoryResponse(VendorApprovalHistory h) {
        return VendorApprovalHistoryResponse.builder()
            .id(h.getId())
            .vendorId(h.getVendor().getId())
            .vendorName(h.getVendor().getCompanyName())
            .actorId(h.getActor().getId())
            .actorName(h.getActor().getName())
            .previousStatus(h.getPreviousStatus())
            .newStatus(h.getNewStatus())
            .reason(h.getReason())
            .comments(h.getComments())
            .actionTimestamp(h.getActionTimestamp())
            .approvalTimeMinutes(h.getApprovalTimeMinutes())
            .build();
    }
}
