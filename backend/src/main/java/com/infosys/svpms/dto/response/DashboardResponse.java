package com.infosys.svpms.dto.response;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponse {
    // Vendor stats
    private long totalVendors;
    private long pendingVendors;
    private long approvedVendors;
    private long rejectedVendors;
    private long suspendedVendors;
    private long nonCompliantVendors;

    // RFQ stats
    private long totalRfqs;
    private long openRfqs;
    private long closedRfqs;
    private long awardedRfqs;

    // Quotation stats
    private long totalQuotations;

    // PO stats
    private long totalPOs;
    private long generatedPOs;
    private long sentPOs;

    // Compliance
    private long expiringDocuments;
    private long expiredDocuments;

    // Chart data
    private List<Map<String, Object>> rfqTrend;
    private List<Map<String, Object>> vendorsByStatus;
    private List<Map<String, Object>> topVendors;
    private List<Map<String, Object>> poByMonth;
}
