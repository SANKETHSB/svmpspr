package com.infosys.svpms.service;

import com.infosys.svpms.entity.PurchaseOrder;
import com.infosys.svpms.entity.RFQ;
import com.infosys.svpms.entity.Vendor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Exportable Procurement Reports — all 12 AC.
 *
 * AC #1  – Vendor list exportable to CSV/Excel
 * AC #2  – RFQ comparison report exportable to PDF
 * AC #3  – PO history exportable
 * AC #4  – Export reflects active filters
 * AC #5  – Reports include timestamp metadata
 * AC #6  – Export action logged
 * AC #7  – File naming follows standard convention
 * AC #8  – Large dataset exports optimized (streaming)
 * AC #9  – Exports exclude restricted fields (passwords, internal paths)
 * AC #10 – Exports respect user permissions
 * AC #11 – Generated reports securely downloadable
 * AC #12 – System prevents export injection vulnerabilities
 */
public interface ExportService {

    /** AC #1: Vendor list → CSV */
    byte[] exportVendorsCsv(String search, Vendor.VendorStatus status,
                             Boolean compliant, String actorEmail);

    /** AC #1: Vendor list → Excel (.xlsx) */
    byte[] exportVendorsExcel(String search, Vendor.VendorStatus status,
                               Boolean compliant, String actorEmail);

    /** AC #2: RFQ comparison report → PDF */
    byte[] exportRfqComparisonPdf(Long rfqId, String actorEmail);

    /** AC #3: PO history → CSV */
    byte[] exportPoHistoryCsv(String search, Long vendorId,
                               PurchaseOrder.POStatus status,
                               LocalDateTime dateFrom, LocalDateTime dateTo,
                               String actorEmail);

    /** AC #3: PO history → Excel */
    byte[] exportPoHistoryExcel(String search, Long vendorId,
                                 PurchaseOrder.POStatus status,
                                 LocalDateTime dateFrom, LocalDateTime dateTo,
                                 String actorEmail);

    /** AC #3: RFQ list → CSV */
    byte[] exportRfqsCsv(String search, RFQ.RFQStatus status,
                          Long createdById, LocalDateTime deadlineFrom,
                          LocalDateTime deadlineTo, String actorEmail);
}
