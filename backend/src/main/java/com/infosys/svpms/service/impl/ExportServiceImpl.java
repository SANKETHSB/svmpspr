package com.infosys.svpms.service.impl;

import com.infosys.svpms.entity.*;
import com.infosys.svpms.exception.BusinessException;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.AuditService;
import com.infosys.svpms.service.ExportService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * ExportServiceImpl — implements all 12 acceptance criteria for
 * "Exportable Procurement Reports" user story.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final VendorRepository vendorRepo;
    private final RFQRepository rfqRepo;
    private final PurchaseOrderRepository poRepo;
    private final QuotationRepository quotationRepo;
    private final UserRepository userRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final AuditService audit;

    // ── AC #10: Module constants — must match role_permissions.module values ──
    private static final String MODULE_VENDORS         = "VENDORS";
    private static final String MODULE_RFQS            = "RFQS";
    private static final String MODULE_PURCHASE_ORDERS = "PURCHASE_ORDERS";

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ── AC #7: Standard file naming convention ────────────────────────────────
    public static String fileName(String prefix, String ext) {
        return prefix + "_" + LocalDateTime.now().format(FILE_FMT) + "." + ext;
    }

    // =========================================================================
    // AC #1: Vendor list → CSV
    // =========================================================================
    @Override
    public byte[] exportVendorsCsv(String search, Vendor.VendorStatus status,
                                    Boolean compliant, String actorEmail) {
        // AC #10: Verify the caller has read permission on the VENDORS module
        checkUserPermission(actorEmail, MODULE_VENDORS, "VENDOR_LIST_CSV_EXPORT");

        // AC #4: Reflect active filters
        List<Vendor> vendors = vendorRepo.searchVendors(
            search, status, compliant,
            PageRequest.of(0, Integer.MAX_VALUE, Sort.by("registeredAt").descending())
        ).getContent();

        // AC #8: Streaming write for large datasets
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter pw = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {

            // AC #5: Timestamp metadata header
            pw.println("# SVPMS Vendor Export Report");
            pw.println("# Generated: " + LocalDateTime.now().format(TS_FMT));
            pw.println("# Filters: search=" + nvl(search) + ", status=" + nvl(status)
                + ", compliant=" + nvl(compliant));
            pw.println("# Total Records: " + vendors.size());
            pw.println();

            // AC #9: Exclude restricted fields (password, internal IDs, file paths)
            pw.println("ID,Company Name,Email,GST Number,Registration ID,Phone,Contact Person,"
                + "Status,Compliant,Performance Score,RFQs Won,RFQs Participated,"
                + "Approved By,Approved At,Registered At");

            for (Vendor v : vendors) {
                // AC #12: Sanitize each field to prevent CSV injection
                pw.println(String.join(",",
                    safe(v.getId()),
                    safe(v.getCompanyName()),
                    safe(v.getEmail()),
                    safe(v.getGstNumber()),
                    safe(v.getRegistrationId()),
                    safe(v.getPhone()),
                    safe(v.getContactPerson()),
                    safe(v.getStatus()),
                    safe(v.isCompliant()),
                    safe(v.getPerformanceScore()),
                    safe(v.getTotalRfqsWon()),
                    safe(v.getTotalRfqsParticipated()),
                    safe(v.getApprovedBy() != null ? v.getApprovedBy().getName() : ""),
                    safe(v.getApprovedAt()),
                    safe(v.getRegisteredAt())
                ));
            }

            // AC #6: Log export action
            logExport(actorEmail, "VENDOR_LIST_CSV_EXPORT",
                "Exported " + vendors.size() + " vendors to CSV. Filters: search=" + nvl(search)
                    + ", status=" + nvl(status) + ", compliant=" + nvl(compliant));

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Vendor CSV export failed: {}", e.getMessage());
            throw new BusinessException("Failed to export vendor list: " + e.getMessage());
        }
    }

    // =========================================================================
    // AC #1: Vendor list → Excel (.xlsx)
    // =========================================================================
    @Override
    public byte[] exportVendorsExcel(String search, Vendor.VendorStatus status,
                                      Boolean compliant, String actorEmail) {
        // AC #10: Verify the caller has read permission on the VENDORS module
        checkUserPermission(actorEmail, MODULE_VENDORS, "VENDOR_LIST_EXCEL_EXPORT");

        // AC #4: Reflect active filters
        List<Vendor> vendors = vendorRepo.searchVendors(
            search, status, compliant,
            PageRequest.of(0, Integer.MAX_VALUE, Sort.by("registeredAt").descending())
        ).getContent();

        // AC #8: SXSSFWorkbook streams rows — handles large datasets without OOM
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Vendors");
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle metaStyle  = buildMetaStyle(wb);

            // AC #5: Timestamp metadata rows
            int rowIdx = 0;
            addMetaRow(sheet, rowIdx++, "SVPMS Vendor Export Report", metaStyle);
            addMetaRow(sheet, rowIdx++, "Generated: " + LocalDateTime.now().format(TS_FMT), metaStyle);
            addMetaRow(sheet, rowIdx++, "Filters: search=" + nvl(search)
                + " | status=" + nvl(status) + " | compliant=" + nvl(compliant), metaStyle);
            addMetaRow(sheet, rowIdx++, "Total Records: " + vendors.size(), metaStyle);
            rowIdx++; // blank row

            // AC #9: Column headers — no password, no file paths
            String[] headers = {"ID", "Company Name", "Email", "GST Number", "Registration ID",
                "Phone", "Contact Person", "Status", "Compliant", "Performance Score",
                "RFQs Won", "RFQs Participated", "Approved By", "Approved At", "Registered At"};
            Row hRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // AC #8: Stream data rows
            for (Vendor v : vendors) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                // AC #12: No formula injection — all values set as strings or numbers
                row.createCell(col++).setCellValue(v.getId());
                row.createCell(col++).setCellValue(sanitizeExcel(v.getCompanyName()));
                row.createCell(col++).setCellValue(sanitizeExcel(v.getEmail()));
                row.createCell(col++).setCellValue(sanitizeExcel(v.getGstNumber()));
                row.createCell(col++).setCellValue(sanitizeExcel(v.getRegistrationId()));
                row.createCell(col++).setCellValue(sanitizeExcel(v.getPhone()));
                row.createCell(col++).setCellValue(sanitizeExcel(v.getContactPerson()));
                row.createCell(col++).setCellValue(v.getStatus() != null ? v.getStatus().name() : "");
                row.createCell(col++).setCellValue(v.isCompliant() ? "Yes" : "No");
                row.createCell(col++).setCellValue(v.getPerformanceScore());
                row.createCell(col++).setCellValue(v.getTotalRfqsWon());
                row.createCell(col++).setCellValue(v.getTotalRfqsParticipated());
                row.createCell(col++).setCellValue(v.getApprovedBy() != null ? sanitizeExcel(v.getApprovedBy().getName()) : "");
                row.createCell(col++).setCellValue(v.getApprovedAt() != null ? v.getApprovedAt().format(TS_FMT) : "");
                row.createCell(col).setCellValue(v.getRegisteredAt() != null ? v.getRegisteredAt().format(TS_FMT) : "");
            }

            wb.write(baos);

            // AC #6: Log export action
            logExport(actorEmail, "VENDOR_LIST_EXCEL_EXPORT",
                "Exported " + vendors.size() + " vendors to Excel. Filters: search=" + nvl(search)
                    + ", status=" + nvl(status) + ", compliant=" + nvl(compliant));

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Vendor Excel export failed: {}", e.getMessage());
            throw new BusinessException("Failed to export vendor list to Excel: " + e.getMessage());
        }
    }

    // =========================================================================
    // AC #2: RFQ comparison report → PDF
    // =========================================================================
    @Override
    public byte[] exportRfqComparisonPdf(Long rfqId, String actorEmail) {
        RFQ rfq = rfqRepo.findById(rfqId)
            .orElseThrow(() -> new ResourceNotFoundException("RFQ", "id", rfqId));

        // AC #10: Only ADMIN/PROCUREMENT_MANAGER can export comparison
        checkUserPermission(actorEmail, MODULE_RFQS, "RFQ_COMPARISON_PDF_EXPORT");

        List<com.infosys.svpms.entity.Quotation> quotations =
            quotationRepo.findByRfqIdOrderByAmount(rfqId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            com.itextpdf.text.Font titleFont  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
            com.itextpdf.text.Font headFont   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
            com.itextpdf.text.Font metaFont   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, BaseColor.GRAY);

            // Title
            Paragraph title = new Paragraph("RFQ Quotation Comparison Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(Chunk.NEWLINE);

            // AC #5: Timestamp metadata
            doc.add(new Paragraph("Generated: " + LocalDateTime.now().format(TS_FMT), metaFont));
            doc.add(new Paragraph("Exported By: " + actorEmail, metaFont));
            doc.add(Chunk.NEWLINE);

            // RFQ details
            doc.add(new Paragraph("RFQ Number: " + rfq.getRfqNumber(), headFont));
            doc.add(new Paragraph("Title: " + sanitizePdf(rfq.getTitle()), normalFont));
            doc.add(new Paragraph("Status: " + rfq.getStatus(), normalFont));
            doc.add(new Paragraph("Deadline: " + rfq.getDeadline().format(TS_FMT), normalFont));
            doc.add(new Paragraph("Total Quotations: " + quotations.size(), normalFont));
            doc.add(Chunk.NEWLINE);

            if (quotations.isEmpty()) {
                doc.add(new Paragraph("No quotations submitted for this RFQ.", normalFont));
            } else {
                // Comparison table
                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{3f, 2f, 1.5f, 2f, 1.5f, 1.5f, 2f, 2f});

                String[] cols = {"Vendor", "Base Amount", "Tax %", "Grand Total",
                    "Delivery Days", "Score", "Status", "Evaluated By"};
                for (String col : cols) {
                    PdfPCell cell = new PdfPCell(new Phrase(col, headFont));
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(4);
                    table.addCell(cell);
                }

                BigDecimal lowest = quotations.get(0).getTotalAmount();
                for (com.infosys.svpms.entity.Quotation q : quotations) {
                    boolean isLowest = q.getTotalAmount().compareTo(lowest) == 0;
                    com.itextpdf.text.Font rowFont = isLowest
                        ? new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, new BaseColor(0, 128, 0))
                        : normalFont;

                    BigDecimal tax   = q.getTotalAmount().multiply(BigDecimal.valueOf(q.getTaxPercentage() / 100.0)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal grand = q.getTotalAmount().add(tax);

                    // AC #9: Exclude vendor password, internal IDs
                    addPdfCell(table, sanitizePdf(q.getVendor().getCompanyName()) + (isLowest ? " ★" : ""), rowFont);
                    addPdfCell(table, q.getTotalAmount() + " " + q.getCurrency(), rowFont);
                    addPdfCell(table, q.getTaxPercentage() + "%", rowFont);
                    addPdfCell(table, grand + " " + q.getCurrency(), rowFont);
                    addPdfCell(table, String.valueOf(q.getDeliveryDays()), rowFont);
                    addPdfCell(table, q.getWeightedScore() != null ? String.format("%.2f", q.getWeightedScore()) : "—", rowFont);
                    addPdfCell(table, q.getStatus().name(), rowFont);
                    addPdfCell(table, q.getEvaluatedBy() != null ? sanitizePdf(q.getEvaluatedBy()) : "—", rowFont);
                }
                doc.add(table);
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("★ = Lowest bidder", metaFont));
            }

            // AC #5: Footer with timestamp
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Report generated by SVPMS on " + LocalDateTime.now().format(TS_FMT), metaFont));

            doc.close();

            // AC #6: Log export
            logExport(actorEmail, "RFQ_COMPARISON_PDF_EXPORT",
                "Exported RFQ comparison PDF for RFQ " + rfq.getRfqNumber()
                    + " (" + quotations.size() + " quotations)");

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("RFQ comparison PDF export failed: {}", e.getMessage());
            throw new BusinessException("Failed to export RFQ comparison: " + e.getMessage());
        }
    }

    // =========================================================================
    // AC #3: PO history → CSV
    // =========================================================================
    @Override
    public byte[] exportPoHistoryCsv(String search, Long vendorId,
                                      PurchaseOrder.POStatus status,
                                      LocalDateTime dateFrom, LocalDateTime dateTo,
                                      String actorEmail) {
        // AC #10: Verify the caller has read permission on the PURCHASE_ORDERS module
        checkUserPermission(actorEmail, MODULE_PURCHASE_ORDERS, "PO_HISTORY_CSV_EXPORT");

        // AC #4: Reflect active filters
        List<PurchaseOrder> pos = poRepo.filterPOs(
            search, vendorId, status, dateFrom, dateTo,
            PageRequest.of(0, Integer.MAX_VALUE, Sort.by("generatedAt").descending())
        ).getContent();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter pw = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {

            // AC #5: Timestamp metadata
            pw.println("# SVPMS Purchase Order History Report");
            pw.println("# Generated: " + LocalDateTime.now().format(TS_FMT));
            pw.println("# Filters: search=" + nvl(search) + ", vendorId=" + nvl(vendorId)
                + ", status=" + nvl(status) + ", from=" + nvl(dateFrom) + ", to=" + nvl(dateTo));
            pw.println("# Total Records: " + pos.size());
            pw.println();

            // AC #9: No internal file paths, no passwords
            pw.println("PO Number,RFQ Number,Vendor,Total Amount,Currency,Status,"
                + "Delivery Date,Payment Terms,Generated By,Generated At");

            for (PurchaseOrder po : pos) {
                pw.println(String.join(",",
                    safe(po.getPoNumber()),
                    safe(po.getRfq().getRfqNumber()),
                    safe(po.getVendor().getCompanyName()),
                    safe(po.getTotalAmount()),
                    safe(po.getCurrency()),
                    safe(po.getStatus()),
                    safe(po.getDeliveryDate()),
                    safe(po.getPaymentTerms()),
                    safe(po.getGeneratedBy().getName()),
                    safe(po.getGeneratedAt())
                ));
            }

            // AC #6: Log export
            logExport(actorEmail, "PO_HISTORY_CSV_EXPORT",
                "Exported " + pos.size() + " POs to CSV. Filters: vendorId=" + nvl(vendorId)
                    + ", status=" + nvl(status));

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PO CSV export failed: {}", e.getMessage());
            throw new BusinessException("Failed to export PO history: " + e.getMessage());
        }
    }

    // =========================================================================
    // AC #3: PO history → Excel
    // =========================================================================
    @Override
    public byte[] exportPoHistoryExcel(String search, Long vendorId,
                                        PurchaseOrder.POStatus status,
                                        LocalDateTime dateFrom, LocalDateTime dateTo,
                                        String actorEmail) {
        // AC #10: Verify the caller has read permission on the PURCHASE_ORDERS module
        checkUserPermission(actorEmail, MODULE_PURCHASE_ORDERS, "PO_HISTORY_EXCEL_EXPORT");

        // AC #4: Reflect active filters
        List<PurchaseOrder> pos = poRepo.filterPOs(
            search, vendorId, status, dateFrom, dateTo,
            PageRequest.of(0, Integer.MAX_VALUE, Sort.by("generatedAt").descending())
        ).getContent();

        // AC #8: SXSSFWorkbook for large datasets
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("PO History");
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle metaStyle   = buildMetaStyle(wb);

            int rowIdx = 0;
            // AC #5: Metadata rows
            addMetaRow(sheet, rowIdx++, "SVPMS Purchase Order History Report", metaStyle);
            addMetaRow(sheet, rowIdx++, "Generated: " + LocalDateTime.now().format(TS_FMT), metaStyle);
            addMetaRow(sheet, rowIdx++, "Filters: vendorId=" + nvl(vendorId)
                + " | status=" + nvl(status) + " | from=" + nvl(dateFrom) + " | to=" + nvl(dateTo), metaStyle);
            addMetaRow(sheet, rowIdx++, "Total Records: " + pos.size(), metaStyle);
            rowIdx++;

            String[] headers = {"PO Number", "RFQ Number", "Vendor", "Total Amount", "Currency",
                "Status", "Delivery Date", "Payment Terms", "Generated By", "Generated At"};
            Row hRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (PurchaseOrder po : pos) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(sanitizeExcel(po.getPoNumber()));
                row.createCell(col++).setCellValue(sanitizeExcel(po.getRfq().getRfqNumber()));
                row.createCell(col++).setCellValue(sanitizeExcel(po.getVendor().getCompanyName()));
                row.createCell(col++).setCellValue(po.getTotalAmount().doubleValue());
                row.createCell(col++).setCellValue(sanitizeExcel(po.getCurrency()));
                row.createCell(col++).setCellValue(po.getStatus().name());
                row.createCell(col++).setCellValue(po.getDeliveryDate() != null ? po.getDeliveryDate().toString() : "");
                row.createCell(col++).setCellValue(sanitizeExcel(po.getPaymentTerms()));
                row.createCell(col++).setCellValue(sanitizeExcel(po.getGeneratedBy().getName()));
                row.createCell(col).setCellValue(po.getGeneratedAt() != null ? po.getGeneratedAt().format(TS_FMT) : "");
            }

            wb.write(baos);

            // AC #6: Log export
            logExport(actorEmail, "PO_HISTORY_EXCEL_EXPORT",
                "Exported " + pos.size() + " POs to Excel. Filters: vendorId=" + nvl(vendorId)
                    + ", status=" + nvl(status));

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PO Excel export failed: {}", e.getMessage());
            throw new BusinessException("Failed to export PO history to Excel: " + e.getMessage());
        }
    }

    // =========================================================================
    // AC #3: RFQ list → CSV
    // =========================================================================
    @Override
    public byte[] exportRfqsCsv(String search, RFQ.RFQStatus status,
                                  Long createdById, LocalDateTime deadlineFrom,
                                  LocalDateTime deadlineTo, String actorEmail) {
        // AC #10: Verify the caller has read permission on the RFQS module
        checkUserPermission(actorEmail, MODULE_RFQS, "RFQ_LIST_CSV_EXPORT");

        // AC #4: Reflect active filters
        List<RFQ> rfqs = rfqRepo.searchRFQs(
            search, status, createdById, deadlineFrom, deadlineTo,
            PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").descending())
        ).getContent();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter pw = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {

            // AC #5: Timestamp metadata
            pw.println("# SVPMS RFQ Report");
            pw.println("# Generated: " + LocalDateTime.now().format(TS_FMT));
            pw.println("# Filters: search=" + nvl(search) + ", status=" + nvl(status)
                + ", createdById=" + nvl(createdById));
            pw.println("# Total Records: " + rfqs.size());
            pw.println();

            // AC #9: No internal fields
            pw.println("RFQ Number,Title,Status,Deadline,Created By,Revision,Awarded Vendor,Created At");

            for (RFQ r : rfqs) {
                pw.println(String.join(",",
                    safe(r.getRfqNumber()),
                    safe(r.getTitle()),
                    safe(r.getStatus()),
                    safe(r.getDeadline()),
                    safe(r.getCreatedBy().getName()),
                    safe(r.getRevisionNumber()),
                    safe(r.getAwardedVendor() != null ? r.getAwardedVendor().getCompanyName() : ""),
                    safe(r.getCreatedAt())
                ));
            }

            // AC #6: Log export
            logExport(actorEmail, "RFQ_LIST_CSV_EXPORT",
                "Exported " + rfqs.size() + " RFQs to CSV. Filters: status=" + nvl(status));

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("RFQ CSV export failed: {}", e.getMessage());
            throw new BusinessException("Failed to export RFQ list: " + e.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** AC #6: Log every export action to audit trail */
    private void logExport(String actorEmail, String action, String description) {
        try {
            userRepo.findByEmail(actorEmail).ifPresent(u ->
                audit.log(u.getId(), "USER", u.getName(), action, "Export", 0L, description)
            );
            log.info("[AC #6] Export logged: action={}, actor={}", action, actorEmail);
        } catch (Exception e) {
            log.error("Failed to log export action: {}", e.getMessage());
        }
    }

    /**
     * AC #10: Verify the authenticated user has canRead = true on the given module.
     *
     * Resolution order:
     *   1. Look up the User by email — must exist and be active.
     *   2. Determine the effective role name:
     *        - If customRoleName is set, use that (custom RBAC role).
     *        - Otherwise fall back to the system enum role name
     *          (ADMIN / PROCUREMENT_MANAGER / COMPLIANCE_OFFICER).
     *   3. Query role_permissions for that role + module.
     *   4. If no row exists or canRead is false, deny with a 403-style exception.
     *
     * This means a custom role with canRead = false on VENDORS will be blocked
     * even if the Spring @PreAuthorize role check passed at the controller layer.
     */
    private void checkUserPermission(String actorEmail, String module, String exportAction) {
        // Step 1: user must exist and be active
        User user = userRepo.findByEmail(actorEmail)
            .orElseThrow(() -> new BusinessException(
                "Unauthorized: user not found for export action " + exportAction));

        if (!user.isActive()) {
            throw new BusinessException(
                "Unauthorized: account is inactive — export action " + exportAction + " denied");
        }

        // Step 2: resolve effective role name
        String effectiveRole = (user.getCustomRoleName() != null && !user.getCustomRoleName().isBlank())
            ? user.getCustomRoleName()
            : user.getRole().name();

        // Step 3: look up the permission row for this role + module
        Optional<RolePermission> permOpt = rolePermissionRepo.findByRoleName(effectiveRole)
            .stream()
            .filter(rp -> module.equalsIgnoreCase(rp.getModule()))
            .findFirst();

        // Step 4: deny if no row or canRead is false
        if (permOpt.isEmpty() || !permOpt.get().isCanRead()) {
            // Audit the denied attempt before throwing
            audit.log(user.getId(), "USER", user.getName(),
                "EXPORT_PERMISSION_DENIED", "Export", 0L,
                "Denied export action " + exportAction + " — role '" + effectiveRole
                    + "' lacks canRead on module " + module);
            log.warn("[AC #10] Export denied: user={}, role={}, module={}, action={}",
                actorEmail, effectiveRole, module, exportAction);
            throw new BusinessException(
                "Access denied: role '" + effectiveRole + "' does not have read permission on module " + module);
        }

        log.debug("[AC #10] Export permitted: user={}, role={}, module={}, action={}",
            actorEmail, effectiveRole, module, exportAction);
    }

    /**
     * AC #12: Sanitize CSV field — prevent formula injection.
     * Prefixes dangerous characters (=, +, -, @, TAB, CR) with a single quote.
     */
    private String safe(Object value) {
        if (value == null) return "";
        String s = value.toString().trim();
        // AC #12: Strip injection characters
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-")
                || s.startsWith("@") || s.startsWith("\t") || s.startsWith("\r")) {
            s = "'" + s;
        }
        // Escape commas and quotes for CSV
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * AC #12: Sanitize Excel cell value — prevent formula injection.
     * Excel formulas start with =, +, -, @.
     */
    private String sanitizeExcel(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            s = "'" + s; // prefix with apostrophe to neutralize formula
        }
        return s;
    }

    /**
     * AC #12: Sanitize PDF text — prevent null bytes and control characters.
     */
    private String sanitizePdf(String value) {
        if (value == null) return "";
        // Remove null bytes and non-printable control characters
        return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
    }

    private String nvl(Object o) { return o == null ? "none" : o.toString(); }

    private void addPdfCell(PdfPTable table, String text, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle buildMetaStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private void addMetaRow(Sheet sheet, int rowIdx, String text, CellStyle style) {
        Row row = sheet.createRow(rowIdx);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }
}
