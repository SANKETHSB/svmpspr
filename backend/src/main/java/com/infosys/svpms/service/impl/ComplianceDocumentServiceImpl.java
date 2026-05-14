package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.response.ComplianceDocResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compliance Document Service — implements all acceptance criteria for
 * "Monitor Vendor Compliance Documents" user story.
 *
 * AC #1  – Vendors must upload mandatory compliance documents.
 * AC #2  – Each document stores issue date and expiry date.
 * AC #3  – System notifies vendors before expiry (handled in SvpmsScheduler).
 * AC #4  – Expired documents flag vendor as non-compliant.
 * AC #5  – Non-compliant vendors cannot participate in new RFQs (enforced in RfqServiceImpl).
 * AC #6  – Admin dashboard shows compliance summary.
 * AC #7  – Upload validates file type (PDF/PNG/JPG/JPEG) and size (≤10 MB).
 * AC #8  – Version history maintained; re-upload creates new version.
 * AC #9  – Re-upload resets compliance status (isExpired=false, vendor.isCompliant=true).
 * AC #10 – Expiry tracking runs via scheduled job (SvpmsScheduler).
 * AC #11 – Expiry warnings logged (SvpmsScheduler + audit).
 * AC #12 – All compliance actions are auditable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceDocumentServiceImpl implements ComplianceDocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf", "image/png", "image/jpeg", "image/jpg"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "png", "jpg", "jpeg"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

    private final ComplianceDocumentRepository repo;
    private final VendorRepository vendorRepo;
    private final UserRepository userRepo;
    private final AuditService audit;
    private final NotificationService notifService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // -------------------------------------------------------------------------
    // AC #1, #2, #7, #8, #9, #12
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public ComplianceDocResponse upload(Long vendorId, String documentType,
                                         String issueDate, String expiryDate,
                                         MultipartFile file, String actorEmail) {

        Vendor vendor = vendorRepo.findById(vendorId)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", vendorId));

        // AC #7: Validate file type
        validateFile(file);

        // AC #2: Parse and validate dates
        LocalDate issue  = parseDate(issueDate,  "issueDate");
        LocalDate expiry = parseDate(expiryDate, "expiryDate");
        if (!expiry.isAfter(issue)) {
            throw new BusinessException("Expiry date must be after issue date.");
        }
        if (expiry.isBefore(LocalDate.now())) {
            throw new BusinessException("Expiry date cannot be in the past.");
        }

        // Persist file to disk
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String storedName   = UUID.randomUUID() + "_" + originalName;
        Path dir  = Paths.get(uploadDir, "compliance", String.valueOf(vendorId));
        Path dest = dir.resolve(storedName);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("File storage failed: {}", e.getMessage());
            throw new RuntimeException("Failed to store compliance document.");
        }

        // AC #8: Determine next version number and mark previous as not-latest
        int nextVersion = 1;
        Optional<ComplianceDocument> previous =
            repo.findByVendorIdAndDocumentTypeAndLatestTrue(vendorId, documentType);
        if (previous.isPresent()) {
            nextVersion = previous.get().getVersion() + 1;
            // Mark all previous versions of this type as not-latest
            repo.markPreviousVersionsNotLatest(vendorId, documentType);
        }

        // Detect MIME type
        String detectedType = file.getContentType();

        ComplianceDocument doc = ComplianceDocument.builder()
            .vendor(vendor)
            .documentType(documentType)
            .fileName(originalName)
            .filePath(dest.toString())
            .fileSize(file.getSize())
            .fileType(detectedType)
            .issueDate(issue)
            .expiryDate(expiry)
            .expired(false)
            .expiryWarningSent(false)
            .version(nextVersion)
            .latest(true)
            .build();

        ComplianceDocument saved = repo.save(doc);

        // AC #9: Re-upload resets compliance status
        if (nextVersion > 1) {
            vendor.setCompliant(true);
            vendorRepo.save(vendor);
            log.info("[AC #9] Compliance reset for vendor {} after re-upload of '{}'",
                vendorId, documentType);
        }

        // AC #12: Audit the upload
        Long actorId = resolveActorId(actorEmail);
        String actorType = vendorRepo.findByEmail(actorEmail).isPresent() ? "VENDOR" : "USER";
        audit.log(actorId, actorType, vendor.getCompanyName(),
            "COMPLIANCE_DOC_UPLOADED", "ComplianceDocument", saved.getId(),
            null,
            "type=" + documentType + ", version=" + nextVersion + ", expiry=" + expiry,
            "Compliance document uploaded: " + documentType + " (v" + nextVersion + ")");

        log.info("[AC #1] Compliance doc uploaded: vendorId={}, type={}, version={}, expiry={}",
            vendorId, documentType, nextVersion, expiry);

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // AC #1: Latest documents per vendor
    // -------------------------------------------------------------------------
    @Override
    public List<ComplianceDocResponse> getByVendor(Long vendorId) {
        return repo.findByVendorIdAndLatestTrue(vendorId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // AC #8: Version history
    // -------------------------------------------------------------------------
    @Override
    public List<ComplianceDocResponse> getVersionHistory(Long vendorId, String documentType) {
        return repo.findByVendorIdAndDocumentTypeOrderByVersionDesc(vendorId, documentType)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // AC #6: Admin compliance summary
    // -------------------------------------------------------------------------
    @Override
    public Map<String, Object> getComplianceSummary() {
        LocalDate today     = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        long totalDocs      = repo.count();
        long expiredDocs    = repo.findExpiredDocuments(today).size();
        long expiringDocs   = repo.countExpiringBefore(threshold);
        long nonCompliantVendors = repo.countVendorsWithExpiredDocs();
        long totalVendors   = vendorRepo.count();
        long compliantVendors = vendorRepo.countByCompliantTrue();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDocuments",       totalDocs);
        summary.put("expiredDocuments",     expiredDocs);
        summary.put("expiringIn30Days",     expiringDocs);
        summary.put("nonCompliantVendors",  nonCompliantVendors);
        summary.put("compliantVendors",     compliantVendors);
        summary.put("totalVendors",         totalVendors);
        summary.put("complianceRate",
            totalVendors > 0
                ? Math.round((compliantVendors * 100.0) / totalVendors)
                : 100);
        return summary;
    }

    // -------------------------------------------------------------------------
    // AC #6: Per-vendor compliance status list
    // -------------------------------------------------------------------------
    @Override
    public List<Map<String, Object>> getAllVendorComplianceStatus() {
        List<ComplianceDocument> allLatest = repo.findAllLatestOrderByExpiry();
        LocalDate today = LocalDate.now();

        // Group by vendor
        Map<Long, List<ComplianceDocument>> byVendor = allLatest.stream()
            .collect(Collectors.groupingBy(d -> d.getVendor().getId()));

        List<Map<String, Object>> result = new ArrayList<>();
        byVendor.forEach((vendorId, docs) -> {
            Vendor v = docs.get(0).getVendor();
            long expiredCount  = docs.stream().filter(ComplianceDocument::isExpired).count();
            long expiringCount = docs.stream()
                .filter(d -> !d.isExpired() && d.getExpiryDate().isBefore(today.plusDays(30)))
                .count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vendorId",       v.getId());
            row.put("vendorName",     v.getCompanyName());
            row.put("isCompliant",    v.isCompliant());
            row.put("totalDocs",      docs.size());
            row.put("expiredDocs",    expiredCount);
            row.put("expiringDocs",   expiringCount);
            row.put("documents",      docs.stream().map(this::toResponse).collect(Collectors.toList()));
            result.add(row);
        });

        // Sort: non-compliant first, then by expiring docs
        result.sort(Comparator
            .comparing((Map<String, Object> m) -> (Boolean) m.get("isCompliant"))
            .thenComparing(m -> -((Long) m.get("expiredDocs"))));

        return result;
    }

    @Override
    @Transactional
    public void delete(Long docId, String actorEmail) {
        ComplianceDocument doc = repo.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
        try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (Exception ignored) {}
        Long actorId = resolveActorId(actorEmail);
        audit.log(actorId, "USER", actorEmail, "COMPLIANCE_DOC_DELETED",
            "ComplianceDocument", docId, "Deleted: " + doc.getDocumentType());
        repo.delete(doc);
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(Long docId) {
        ComplianceDocument doc = repo.findById(docId)
            .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));
        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable");
            }
            String contentType = doc.getFileType() != null
                ? doc.getFileType()
                : Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            log.error("Error downloading compliance doc {}: {}", docId, e.getMessage());
            throw new RuntimeException("Failed to download file");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * AC #7: Validate file type (PDF, PNG, JPG/JPEG) and size (≤10 MB).
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File must not be empty.");
        }
        // Size check
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                "File size exceeds the 10 MB limit. Uploaded: "
                + (file.getSize() / (1024 * 1024)) + " MB.");
        }
        // MIME type check
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(
                "Invalid file type '" + contentType + "'. Allowed: PDF, PNG, JPG/JPEG.");
        }
        // Extension check (double-validation)
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                : "";
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BusinessException(
                    "Invalid file extension '." + ext + "'. Allowed: .pdf, .png, .jpg, .jpeg.");
            }
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new BusinessException("Invalid " + fieldName + " format. Use YYYY-MM-DD.");
        }
    }

    private Long resolveActorId(String email) {
        return vendorRepo.findByEmail(email)
            .map(Vendor::getId)
            .orElseGet(() -> userRepo.findByEmail(email)
                .map(User::getId)
                .orElse(0L));
    }

    private ComplianceDocResponse toResponse(ComplianceDocument d) {
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), d.getExpiryDate());
        return ComplianceDocResponse.builder()
            .id(d.getId())
            .vendorId(d.getVendor().getId())
            .vendorName(d.getVendor().getCompanyName())
            .documentType(d.getDocumentType())
            .fileName(d.getFileName())
            .fileSize(d.getFileSize())
            .fileType(d.getFileType())
            .issueDate(d.getIssueDate())
            .expiryDate(d.getExpiryDate())
            .expired(d.isExpired())
            .latest(d.isLatest())
            .version(d.getVersion())
            .uploadedAt(d.getUploadedAt())
            .daysUntilExpiry(daysUntilExpiry)
            .build();
    }
}
