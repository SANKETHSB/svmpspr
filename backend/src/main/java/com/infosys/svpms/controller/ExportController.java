package com.infosys.svpms.controller;

import com.infosys.svpms.entity.PurchaseOrder;
import com.infosys.svpms.entity.RFQ;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.service.ExportService;
import com.infosys.svpms.service.impl.ExportServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Export Controller — Exportable Procurement Reports
 *
 * AC #1  – Vendor list → CSV / Excel
 * AC #2  – RFQ comparison → PDF
 * AC #3  – PO history → CSV / Excel; RFQ list → CSV
 * AC #4  – All exports reflect active filters (query params)
 * AC #5  – Timestamp metadata embedded in every export
 * AC #6  – Every export action logged (in ExportService)
 * AC #7  – File naming: {type}_{yyyyMMdd_HHmmss}.{ext}
 * AC #8  – Large datasets handled via streaming (SXSSFWorkbook)
 * AC #9  – Restricted fields excluded (passwords, file paths)
 * AC #10 – Role-based access via @PreAuthorize
 * AC #11 – Secure download: Content-Disposition attachment, no path traversal
 * AC #12 – Injection prevention in ExportService (CSV/Excel/PDF sanitization)
 */
@RestController
@RequestMapping("/exports")
@RequiredArgsConstructor
@Tag(name = "Procurement Reports Export")
public class ExportController {

    private final ExportService exportService;

    // ── AC #1: Vendor list → CSV ──────────────────────────────────────────────
    @GetMapping("/vendors/csv")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Export vendor list to CSV (AC #1,#4,#5,#6,#7,#9,#12)")
    public ResponseEntity<byte[]> exportVendorsCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Vendor.VendorStatus status,
            @RequestParam(required = false) Boolean compliant,
            @AuthenticationPrincipal UserDetails ud) {

        byte[] data = exportService.exportVendorsCsv(search, status, compliant, ud.getUsername());
        // AC #7: Standard file naming; AC #11: attachment disposition
        return download(data, ExportServiceImpl.fileName("vendors", "csv"), "text/csv");
    }

    // ── AC #1: Vendor list → Excel ────────────────────────────────────────────
    @GetMapping("/vendors/excel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Export vendor list to Excel (AC #1,#4,#5,#6,#7,#8,#9,#12)")
    public ResponseEntity<byte[]> exportVendorsExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Vendor.VendorStatus status,
            @RequestParam(required = false) Boolean compliant,
            @AuthenticationPrincipal UserDetails ud) {

        byte[] data = exportService.exportVendorsExcel(search, status, compliant, ud.getUsername());
        return download(data, ExportServiceImpl.fileName("vendors", "xlsx"),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    // ── AC #2: RFQ comparison → PDF ───────────────────────────────────────────
    @GetMapping("/rfqs/{rfqId}/comparison/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Export RFQ quotation comparison to PDF (AC #2,#5,#6,#7,#9,#10,#12)")
    public ResponseEntity<byte[]> exportRfqComparisonPdf(
            @PathVariable Long rfqId,
            @AuthenticationPrincipal UserDetails ud) {

        byte[] data = exportService.exportRfqComparisonPdf(rfqId, ud.getUsername());
        return download(data, ExportServiceImpl.fileName("rfq_comparison_" + rfqId, "pdf"),
            "application/pdf");
    }

    // ── AC #3: PO history → CSV ───────────────────────────────────────────────
    @GetMapping("/purchase-orders/csv")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Export PO history to CSV (AC #3,#4,#5,#6,#7,#9,#12)")
    public ResponseEntity<byte[]> exportPoHistoryCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) PurchaseOrder.POStatus status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @AuthenticationPrincipal UserDetails ud) {

        LocalDateTime from = dateFrom != null ? LocalDateTime.parse(dateFrom) : null;
        LocalDateTime to   = dateTo   != null ? LocalDateTime.parse(dateTo)   : null;
        byte[] data = exportService.exportPoHistoryCsv(search, vendorId, status, from, to, ud.getUsername());
        return download(data, ExportServiceImpl.fileName("po_history", "csv"), "text/csv");
    }

    // ── AC #3: PO history → Excel ─────────────────────────────────────────────
    @GetMapping("/purchase-orders/excel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Export PO history to Excel (AC #3,#4,#5,#6,#7,#8,#9,#12)")
    public ResponseEntity<byte[]> exportPoHistoryExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) PurchaseOrder.POStatus status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @AuthenticationPrincipal UserDetails ud) {

        LocalDateTime from = dateFrom != null ? LocalDateTime.parse(dateFrom) : null;
        LocalDateTime to   = dateTo   != null ? LocalDateTime.parse(dateTo)   : null;
        byte[] data = exportService.exportPoHistoryExcel(search, vendorId, status, from, to, ud.getUsername());
        return download(data, ExportServiceImpl.fileName("po_history", "xlsx"),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    // ── AC #3: RFQ list → CSV ─────────────────────────────────────────────────
    @GetMapping("/rfqs/csv")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Export RFQ list to CSV (AC #3,#4,#5,#6,#7,#9,#12)")
    public ResponseEntity<byte[]> exportRfqsCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RFQ.RFQStatus status,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) String deadlineFrom,
            @RequestParam(required = false) String deadlineTo,
            @AuthenticationPrincipal UserDetails ud) {

        LocalDateTime from = deadlineFrom != null ? LocalDateTime.parse(deadlineFrom) : null;
        LocalDateTime to   = deadlineTo   != null ? LocalDateTime.parse(deadlineTo)   : null;
        byte[] data = exportService.exportRfqsCsv(search, status, createdById, from, to, ud.getUsername());
        return download(data, ExportServiceImpl.fileName("rfqs", "csv"), "text/csv");
    }

    // ── AC #11: Secure download helper ───────────────────────────────────────
    private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
        // AC #11: Content-Disposition attachment prevents inline execution
        // AC #11: Sanitize filename — no path traversal characters
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(data.length)
            .body(data);
    }
}
