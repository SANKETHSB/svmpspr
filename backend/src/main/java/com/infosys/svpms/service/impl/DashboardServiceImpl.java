package com.infosys.svpms.service.impl;

import com.infosys.svpms.dto.response.DashboardResponse;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class DashboardServiceImpl {

    private final VendorRepository vendorRepo;
    private final RFQRepository rfqRepo;
    private final QuotationRepository quotationRepo;
    private final PurchaseOrderRepository poRepo;
    private final ComplianceDocumentRepository complianceRepo;

    public DashboardResponse getStats() {
        long expiring = complianceRepo.findExpiringBefore(LocalDate.now().plusDays(30)).size();
        long expired  = complianceRepo.findExpiredDocuments(LocalDate.now()).size();

        List<Map<String,Object>> vendorsByStatus = new ArrayList<>();
        for (Vendor.VendorStatus s : Vendor.VendorStatus.values()) {
            Map<String,Object> m = new HashMap<>(); m.put("status",s.name()); m.put("count",vendorRepo.findByStatus(s).size());
            vendorsByStatus.add(m);
        }
        List<Map<String,Object>> rfqTrend = new ArrayList<>();
        for (RFQ.RFQStatus s : RFQ.RFQStatus.values()) {
            Map<String,Object> m = new HashMap<>(); m.put("status",s.name()); m.put("count",rfqRepo.countByStatus(s));
            rfqTrend.add(m);
        }
        List<Map<String,Object>> topVendors = new ArrayList<>();
        vendorRepo.findByStatus(Vendor.VendorStatus.APPROVED).stream().sorted(
            Comparator.comparingInt(Vendor::getTotalRfqsWon).reversed()).limit(5).forEach(v -> {
            Map<String,Object> m = new HashMap<>(); m.put("name",v.getCompanyName()); m.put("won",v.getTotalRfqsWon()); m.put("score",v.getPerformanceScore());
            topVendors.add(m);
        });

        return DashboardResponse.builder()
            .totalVendors(vendorRepo.count())
            .pendingVendors(vendorRepo.findByStatus(Vendor.VendorStatus.PENDING_APPROVAL).size())
            .approvedVendors(vendorRepo.findByStatus(Vendor.VendorStatus.APPROVED).size())
            .rejectedVendors(vendorRepo.findByStatus(Vendor.VendorStatus.REJECTED).size())
            .suspendedVendors(vendorRepo.findByStatus(Vendor.VendorStatus.SUSPENDED).size())
            .totalRfqs(rfqRepo.count())
            .openRfqs(rfqRepo.countByStatus(RFQ.RFQStatus.OPEN))
            .closedRfqs(rfqRepo.countByStatus(RFQ.RFQStatus.CLOSED))
            .awardedRfqs(rfqRepo.countByStatus(RFQ.RFQStatus.AWARDED))
            .totalQuotations(quotationRepo.count())
            .totalPOs(poRepo.count())
            .expiringDocuments(expiring).expiredDocuments(expired)
            .vendorsByStatus(vendorsByStatus).rfqTrend(rfqTrend).topVendors(topVendors)
            .build();
    }
}
