package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.request.*;
import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository repo;
    private final QuotationDocumentRepository docRepo;
    private final RFQRepository rfqRepo;
    private final RfqItemRepository rfqItemRepo;
    private final VendorRepository vendorRepo;
    private final UserRepository userRepo;
    private final RfqVendorInviteRepository inviteRepo;
    private final AuditService audit;
    private final NotificationService notif;
    private final EmailService emailService; // US 06: confirmation email
    
    @org.springframework.beans.factory.annotation.Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override @Transactional
    public QuotationResponse submit(QuotationRequest req, String vendorEmail) {
        Vendor vendor = vendorRepo.findByEmail(vendorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor","email",vendorEmail));
        if (vendor.getStatus() != Vendor.VendorStatus.APPROVED)
            throw new BusinessException("Only approved vendors can submit quotations.");
        RFQ rfq = rfqRepo.findById(req.getRfqId())
            .orElseThrow(() -> new ResourceNotFoundException("RFQ","id",req.getRfqId()));
        
        // US 06: System prevents duplicate vendor submissions after award
        if (rfq.getStatus() == RFQ.RFQStatus.AWARDED) {
            throw new BusinessException("Cannot submit quotation. RFQ has already been awarded.");
        }
        
        // US 11 AC #3: Late quotation submissions must return HTTP 400 with appropriate message
        // US 11 AC #7: Closed RFQs must not accept new submissions
        if (rfq.getStatus() == RFQ.RFQStatus.CLOSED) {
            throw new BusinessException(
                String.format(
                    "Cannot submit quotation. RFQ '%s' is CLOSED. " +
                    "The submission deadline (%s) has passed and the RFQ was automatically closed. " +
                    "Late submissions are not accepted to ensure fairness and compliance. " +
                    "If you believe this closure was premature, please contact the procurement team.",
                    rfq.getTitle(),
                    rfq.getDeadline()
                )
            );
        }
        
        if (rfq.getStatus() != RFQ.RFQStatus.OPEN)
            throw new BusinessException("RFQ is not open for quotation submission.");
        
        // US 06: Submission only before deadline + Late submissions automatically rejected
        // US 11 AC #3: Server-side deadline validation with clear error message
        LocalDateTime serverTime = LocalDateTime.now();
        if (rfq.getDeadline().isBefore(serverTime)) {
            throw new BusinessException(
                String.format(
                    "Submission deadline has passed. Late submissions are not accepted. " +
                    "RFQ: %s, Deadline: %s, Current Server Time: %s. " +
                    "The RFQ will be automatically closed shortly.",
                    rfq.getRfqNumber(),
                    rfq.getDeadline(),
                    serverTime
                )
            );
        }
        
        if (!inviteRepo.existsByRfqIdAndVendorId(rfq.getId(), vendor.getId()))
            throw new BusinessException("Your vendor is not invited to this RFQ.");
        if (repo.existsByRfqIdAndVendorId(rfq.getId(), vendor.getId()))
            throw new DuplicateException("You have already submitted a quotation for this RFQ. Use resubmit.");

        // US 06: Price must be positive decimal (already validated by @DecimalMin in DTO)
        // US 06: Tax & currency mandatory (already validated by @NotNull/@NotBlank in DTO)
        
        Quotation q = Quotation.builder()
            .rfq(rfq).vendor(vendor)
            .totalAmount(req.getTotalAmount()).taxPercentage(req.getTaxPercentage())
            .currency(req.getCurrency()).deliveryDays(req.getDeliveryDays())
            .notes(req.getNotes()).build();

        List<QuotationItem> items = buildItems(q, rfq, req);
        q.setItems(items);
        Quotation saved = repo.save(q);
        
        // US 06: Supporting documents required - Check after initial save
        // Note: Documents must be uploaded separately after quotation creation
        // This is enforced in the frontend and can be validated during final submission

        // Mark invite as responded
        inviteRepo.findByRfqIdAndVendorId(rfq.getId(), vendor.getId())
            .ifPresent(inv -> { inv.setHasResponded(true); inviteRepo.save(inv); });

        // Update vendor stats
        vendor.setTotalRfqsParticipated(vendor.getTotalRfqsParticipated() + 1);
        vendorRepo.save(vendor);

        // US 06: Submission action logged
        audit.log(vendor.getId(),"VENDOR",vendor.getCompanyName(),"QUOTATION_SUBMITTED","Quotation",saved.getId(),
            "Submitted for RFQ: "+rfq.getRfqNumber()+" | Amount: "+req.getTotalAmount()+" "+req.getCurrency());
        
        // US 06: Confirmation email sent (in-app + console email)
        notif.send(vendor.getId(),"VENDOR","Quotation Submitted Successfully",
            "Your quotation for RFQ "+rfq.getRfqNumber()+" has been submitted successfully. Submission ID: "+saved.getId(),
            com.infosys.svpms.entity.Notification.NotificationType.GENERAL, saved.getId(), "Quotation");
        emailService.sendQuotationSubmissionEmail(
            vendor.getEmail(), vendor.getCompanyName(),
            rfq.getRfqNumber(), rfq.getTitle(),
            saved.getId(),
            String.valueOf(req.getTotalAmount()), req.getCurrency(),
            String.valueOf(saved.getSubmittedAt()));
        
        // Notify procurement manager
        userRepo.findByRole(com.infosys.svpms.entity.User.Role.PROCUREMENT_MANAGER).forEach(manager -> {
            notif.send(manager.getId(),"USER","New Quotation Received",
                vendor.getCompanyName()+" submitted a quotation for RFQ "+rfq.getRfqNumber(),
                com.infosys.svpms.entity.Notification.NotificationType.GENERAL, saved.getId(), "Quotation");
        });
        
        return toResponse(saved);
    }

    @Override @Transactional
    public QuotationResponse resubmit(Long id, QuotationRequest req, String vendorEmail) {
        Quotation q = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quotation","id",id));
        Vendor vendor = vendorRepo.findByEmail(vendorEmail).orElseThrow();
        if (!q.getVendor().getId().equals(vendor.getId()))
            throw new BusinessException("Not your quotation.");
        
        // US 06: System prevents duplicate vendor submissions after award
        if (q.getRfq().getStatus() == RFQ.RFQStatus.AWARDED) {
            throw new BusinessException("Cannot resubmit quotation. RFQ has already been awarded.");
        }
        
        // US 11 AC #3: Closed RFQs must not accept resubmissions
        if (q.getRfq().getStatus() == RFQ.RFQStatus.CLOSED) {
            throw new BusinessException(
                String.format(
                    "Cannot resubmit quotation. RFQ '%s' is CLOSED. " +
                    "The submission deadline has passed. " +
                    "Your original quotation will be considered for evaluation.",
                    q.getRfq().getTitle()
                )
            );
        }
        
        // US 06: Overwriting previous submission allowed before deadline
        // US 11 AC #3: Server-side deadline validation
        LocalDateTime serverTime = LocalDateTime.now();
        if (q.getRfq().getDeadline().isBefore(serverTime)) {
            throw new BusinessException(
                String.format(
                    "Deadline has passed. Cannot resubmit. " +
                    "Deadline: %s, Current Server Time: %s. " +
                    "Your original quotation will be evaluated.",
                    q.getRfq().getDeadline(),
                    serverTime
                )
            );
        }
        
        q.setTotalAmount(req.getTotalAmount());
        q.setTaxPercentage(req.getTaxPercentage());
        q.setCurrency(req.getCurrency());
        q.setDeliveryDays(req.getDeliveryDays());
        q.setNotes(req.getNotes());
        q.getItems().clear();
        q.getItems().addAll(buildItems(q, q.getRfq(), req));
        Quotation saved = repo.save(q);
        
        audit.log(vendor.getId(),"VENDOR",vendor.getCompanyName(),"QUOTATION_RESUBMITTED","Quotation",id,
            "Resubmitted for RFQ: "+q.getRfq().getRfqNumber()+" | New Amount: "+req.getTotalAmount()+" "+req.getCurrency());
        
        // Confirmation notification (in-app + console email)
        notif.send(vendor.getId(),"VENDOR","Quotation Updated Successfully",
            "Your quotation for RFQ "+q.getRfq().getRfqNumber()+" has been updated successfully.",
            com.infosys.svpms.entity.Notification.NotificationType.GENERAL, saved.getId(), "Quotation");
        emailService.sendQuotationSubmissionEmail(
            vendor.getEmail(), vendor.getCompanyName(),
            q.getRfq().getRfqNumber(), q.getRfq().getTitle(),
            saved.getId(),
            String.valueOf(req.getTotalAmount()), req.getCurrency(),
            String.valueOf(saved.getSubmittedAt()));
        
        return toResponse(saved);
    }

    @Override
    public QuotationResponse getById(Long id) {
        return toResponse(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quotation","id",id)));
    }

    @Override
    public List<QuotationResponse> getByRfq(Long rfqId) {
        return repo.findByRfqIdOrderByAmount(rfqId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<QuotationResponse> getByVendor(Long vendorId) {
        return repo.findByVendorId(vendorId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<QuotationResponse> compareByRfq(Long rfqId) {
        List<Quotation> quotations = repo.findByRfqIdOrderByAmount(rfqId);
        
        // US 07: Highlight lowest bidder
        BigDecimal lowestAmount = quotations.isEmpty() ? null : quotations.get(0).getTotalAmount();
        
        return quotations.stream()
            .map(q -> {
                QuotationResponse response = toResponse(q);
                // Mark as lowest bidder if amount matches the lowest
                if (lowestAmount != null && q.getTotalAmount().compareTo(lowestAmount) == 0) {
                    response.setLowestBidder(true);
                }
                return response;
            })
            .collect(Collectors.toList());
    }

    @Override @Transactional
    public QuotationResponse evaluate(Long id, QuotationEvaluationRequest req, String actorEmail) {
        Quotation q = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quotation","id",id));
        User actor = userRepo.findByEmail(actorEmail).orElseThrow();
        
        // US 07: Managers can evaluate quotations at any time for comparison purposes
        // Note: Evaluation can be done even while RFQ is OPEN to allow preliminary scoring
        RFQ rfq = q.getRfq();
        
        // US 07: Evaluation comments mandatory before award (validated by @NotBlank in DTO)
        // US 07: Score recalculated automatically on edit
        Double oldScore = q.getWeightedScore();
        q.setWeightedScore(req.getScore());
        q.setEvaluationComment(req.getComment());
        q.setEvaluatedBy(actor.getName());
        q.setEvaluatedAt(LocalDateTime.now());
        q.setStatus(Quotation.QuotationStatus.UNDER_EVALUATION);
        Quotation saved = repo.save(q);
        
        String auditMessage = "Score: "+req.getScore();
        if (oldScore != null) {
            auditMessage += " (Previous: "+oldScore+", Recalculated)";
        }
        audit.log(actor.getId(),"USER",actor.getName(),"QUOTATION_EVALUATED","Quotation",id,auditMessage);
        
        return toResponse(saved);
    }

    @Override @Transactional
    public void uploadDocument(Long quotationId, org.springframework.web.multipart.MultipartFile file, String vendorEmail) {
        Quotation quotation = repo.findById(quotationId)
            .orElseThrow(() -> new ResourceNotFoundException("Quotation","id",quotationId));
        Vendor vendor = vendorRepo.findByEmail(vendorEmail).orElseThrow();
        
        if (!quotation.getVendor().getId().equals(vendor.getId())) {
            throw new BusinessException("Not your quotation.");
        }
        
        if (quotation.getRfq().getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot upload documents after deadline.");
        }
        
        // US 06: File integrity verified - Calculate SHA-256 checksum
        String checksum = calculateChecksum(file);
        
        String fileName = java.util.UUID.randomUUID() + "_" + file.getOriginalFilename();
        java.nio.file.Path path = java.nio.file.Paths.get(uploadDir, "quotations", String.valueOf(quotationId));
        
        try {
            java.nio.file.Files.createDirectories(path);
            java.nio.file.Files.copy(file.getInputStream(), path.resolve(fileName), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store document: " + e.getMessage());
        }
        
        QuotationDocument document = QuotationDocument.builder()
            .quotation(quotation)
            .fileName(file.getOriginalFilename())
            .filePath(path.resolve(fileName).toString())
            .fileSize(file.getSize())
            .fileType(file.getContentType())
            .checksum(checksum)
            .build();
        
        docRepo.save(document);
        audit.log(vendor.getId(),"VENDOR",vendor.getCompanyName(),"QUOTATION_DOCUMENT_UPLOADED","Quotation",quotationId,
            "Uploaded document: "+file.getOriginalFilename()+" | Checksum: "+checksum);
    }

    @Override
    public List<java.util.Map<String, Object>> getDocuments(Long quotationId) {
        return docRepo.findByQuotationId(quotationId).stream()
            .map(doc -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", doc.getId());
                map.put("fileName", doc.getFileName());
                map.put("fileSize", doc.getFileSize());
                map.put("fileType", doc.getFileType());
                map.put("checksum", doc.getChecksum());
                map.put("uploadedAt", doc.getUploadedAt());
                return map;
            })
            .collect(Collectors.toList());
    }

    @Override @Transactional
    public void rejectLateSubmissions() {
        // US 06: Late submissions automatically rejected
        List<RFQ> expiredRfqs = rfqRepo.findExpiredOpenRFQs(LocalDateTime.now());
        
        expiredRfqs.forEach(rfq -> {
            List<Quotation> quotations = repo.findByRfqId(rfq.getId());
            quotations.stream()
                .filter(q -> q.getStatus() == Quotation.QuotationStatus.SUBMITTED)
                .filter(q -> q.getSubmittedAt().isAfter(rfq.getDeadline()))
                .forEach(q -> {
                    q.setStatus(Quotation.QuotationStatus.REJECTED);
                    q.setEvaluationComment("Automatically rejected: Submitted after deadline");
                    repo.save(q);
                    
                    notif.send(q.getVendor().getId(),"VENDOR","Quotation Rejected - Late Submission",
                        "Your quotation for RFQ "+rfq.getRfqNumber()+" was submitted after the deadline and has been automatically rejected.",
                        com.infosys.svpms.entity.Notification.NotificationType.GENERAL, q.getId(), "Quotation");
                    
                    audit.log(0L,"SYSTEM","SCHEDULER","QUOTATION_AUTO_REJECTED","Quotation",q.getId(),
                        "Late submission rejected for RFQ: "+rfq.getRfqNumber());
                });
        });
    }

    @Override
    public byte[] exportComparisonToPdf(Long rfqId) {
        // US 07: Export comparison to PDF
        RFQ rfq = rfqRepo.findById(rfqId)
            .orElseThrow(() -> new ResourceNotFoundException("RFQ","id",rfqId));
        List<Quotation> quotations = repo.findByRfqIdOrderByAmount(rfqId);
        
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4.rotate());
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Title
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Quotation Comparison Report", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // RFQ Details
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
            document.add(new com.itextpdf.text.Paragraph("RFQ Number: " + rfq.getRfqNumber(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("RFQ Title: " + rfq.getTitle(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Deadline: " + rfq.getDeadline(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Total Quotations: " + quotations.size(), normalFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // Comparison Table
            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 2, 2, 2, 2, 2, 3});
            
            // Header
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
            String[] headers = {"Vendor", "Total Amount", "Tax", "Grand Total", "Delivery Days", "Score", "Status", "Evaluated By"};
            for (String header : headers) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                table.addCell(cell);
            }
            
            // Data rows
            BigDecimal lowestAmount = quotations.isEmpty() ? null : quotations.get(0).getTotalAmount();
            for (Quotation q : quotations) {
                BigDecimal tax = q.getTotalAmount().multiply(BigDecimal.valueOf(q.getTaxPercentage()/100.0)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal grand = q.getTotalAmount().add(tax);
                
                // Highlight lowest bidder
                com.itextpdf.text.Font cellFont = normalFont;
                if (lowestAmount != null && q.getTotalAmount().compareTo(lowestAmount) == 0) {
                    cellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
                }
                
                table.addCell(new com.itextpdf.text.Phrase(q.getVendor().getCompanyName(), cellFont));
                table.addCell(new com.itextpdf.text.Phrase(q.getTotalAmount() + " " + q.getCurrency(), cellFont));
                table.addCell(new com.itextpdf.text.Phrase(tax + " " + q.getCurrency(), cellFont));
                table.addCell(new com.itextpdf.text.Phrase(grand + " " + q.getCurrency(), cellFont));
                table.addCell(new com.itextpdf.text.Phrase(String.valueOf(q.getDeliveryDays()), cellFont));
                table.addCell(new com.itextpdf.text.Phrase(q.getWeightedScore() != null ? String.format("%.2f", q.getWeightedScore()) : "N/A", cellFont));
                table.addCell(new com.itextpdf.text.Phrase(q.getStatus().toString(), cellFont));
                table.addCell(new com.itextpdf.text.Phrase(q.getEvaluatedBy() != null ? q.getEvaluatedBy() : "N/A", cellFont));
            }
            
            document.add(table);
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // Footer
            document.add(new com.itextpdf.text.Paragraph("Generated on: " + LocalDateTime.now(), normalFont));
            document.add(new com.itextpdf.text.Paragraph("Note: Lowest bidder is highlighted in bold.", normalFont));
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadDocument(Long documentId, String actorEmail) {
        // US 07: Attachments previewable inline
        QuotationDocument doc = docRepo.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document","id",documentId));
        
        // Verify access - only managers and the vendor who owns the quotation can download
        User user = userRepo.findByEmail(actorEmail).orElse(null);
        Vendor vendor = vendorRepo.findByEmail(actorEmail).orElse(null);
        
        boolean hasAccess = false;
        if (user != null && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.PROCUREMENT_MANAGER)) {
            hasAccess = true;
        } else if (vendor != null && doc.getQuotation().getVendor().getId().equals(vendor.getId())) {
            hasAccess = true;
        }
        
        if (!hasAccess) {
            throw new BusinessException("You do not have permission to access this document.");
        }
        
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(doc.getFilePath());
            byte[] fileContent = java.nio.file.Files.readAllBytes(path);
            
            // Verify file integrity
            String currentChecksum = calculateChecksumFromBytes(fileContent);
            if (!currentChecksum.equals(doc.getChecksum())) {
                throw new BusinessException("File integrity check failed. Document may have been tampered with.");
            }
            
            return fileContent;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read document: " + e.getMessage());
        }
    }

    @Override @Transactional
    public QuotationResponse finalizeSubmission(Long quotationId, String vendorEmail) {
        Quotation quotation = repo.findById(quotationId)
            .orElseThrow(() -> new ResourceNotFoundException("Quotation","id",quotationId));
        Vendor vendor = vendorRepo.findByEmail(vendorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor","email",vendorEmail));
        
        if (!quotation.getVendor().getId().equals(vendor.getId())) {
            throw new BusinessException("Not your quotation.");
        }
        
        // US 06: Supporting documents required - Validate at least one document is uploaded
        long documentCount = docRepo.countByQuotationId(quotationId);
        if (documentCount == 0) {
            throw new BusinessException("At least one supporting document is required before finalizing submission. Please upload documents such as technical specifications, certifications, or company profile.");
        }
        
        // Check deadline again
        if (quotation.getRfq().getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Deadline has passed. Cannot finalize submission.");
        }
        
        // Check RFQ status
        if (quotation.getRfq().getStatus() == RFQ.RFQStatus.AWARDED) {
            throw new BusinessException("Cannot finalize submission. RFQ has already been awarded.");
        }
        
        // Mark as finalized (status is already SUBMITTED from initial save)
        quotation.setStatus(Quotation.QuotationStatus.SUBMITTED);
        Quotation saved = repo.save(quotation);
        
        audit.log(vendor.getId(),"VENDOR",vendor.getCompanyName(),"QUOTATION_FINALIZED","Quotation",quotationId,
            "Quotation finalized with "+documentCount+" supporting document(s)");
        
        notif.send(vendor.getId(),"VENDOR","Quotation Finalized",
            "Your quotation for RFQ "+quotation.getRfq().getRfqNumber()+" has been finalized with all required documents.",
            com.infosys.svpms.entity.Notification.NotificationType.GENERAL, saved.getId(), "Quotation");
        
        return toResponse(saved);
    }

    // Helper method to calculate SHA-256 checksum for file integrity
    private String calculateChecksum(org.springframework.web.multipart.MultipartFile file) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate checksum: " + e.getMessage());
        }
    }

    private String calculateChecksumFromBytes(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate checksum: " + e.getMessage());
        }
    }

    private List<QuotationItem> buildItems(Quotation q, RFQ rfq, QuotationRequest req) {
        return req.getItems().stream().map(ir -> {
            RfqItem rfqItem = rfqItemRepo.findById(ir.getRfqItemId())
                .orElseThrow(() -> new ResourceNotFoundException("RfqItem","id",ir.getRfqItemId()));
            BigDecimal total = ir.getUnitPrice().multiply(BigDecimal.valueOf(rfqItem.getQuantity()));
            return QuotationItem.builder().quotation(q).rfqItem(rfqItem)
                .unitPrice(ir.getUnitPrice()).totalPrice(total).build();
        }).collect(Collectors.toList());
    }

    private QuotationResponse toResponse(Quotation q) {
        BigDecimal tax = q.getTotalAmount().multiply(BigDecimal.valueOf(q.getTaxPercentage()/100.0)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grand = q.getTotalAmount().add(tax);
        List<QuotationItemResponse> items = q.getItems() == null ? List.of() : q.getItems().stream().map(i ->
            QuotationItemResponse.builder()
                .id(i.getId()).rfqItemId(i.getRfqItem().getId())
                .itemName(i.getRfqItem().getItemName()).quantity(i.getRfqItem().getQuantity())
                .unit(i.getRfqItem().getUnit()).unitPrice(i.getUnitPrice()).totalPrice(i.getTotalPrice())
                .build()).collect(Collectors.toList());
        
        // US 07: Document count for preview
        int docCount = (int) docRepo.countByQuotationId(q.getId());
        
        return QuotationResponse.builder()
            .id(q.getId()).rfqId(q.getRfq().getId()).rfqNumber(q.getRfq().getRfqNumber())
            .vendorId(q.getVendor().getId()).vendorName(q.getVendor().getCompanyName())
            .totalAmount(q.getTotalAmount()).taxAmount(tax).grandTotal(grand)
            .taxPercentage(q.getTaxPercentage()).currency(q.getCurrency())
            .deliveryDays(q.getDeliveryDays()).notes(q.getNotes())
            .status(q.getStatus()).weightedScore(q.getWeightedScore())
            .evaluationComment(q.getEvaluationComment()).evaluatedBy(q.getEvaluatedBy())
            .evaluatedAt(q.getEvaluatedAt()).awarded(q.isAwarded())
            .lowestBidder(false) // Will be set in compareByRfq if applicable
            .items(items).submittedAt(q.getSubmittedAt())
            .documentCount(docCount)
            .build();
    }
}
