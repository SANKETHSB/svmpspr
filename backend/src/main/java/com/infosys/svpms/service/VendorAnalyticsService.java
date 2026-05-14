package com.infosys.svpms.service;

import com.infosys.svpms.dto.response.VendorAnalyticsResponse;
import java.time.LocalDate;
import java.util.List;

public interface VendorAnalyticsService {
    
    // US 10: Get comprehensive analytics for a vendor
    VendorAnalyticsResponse getVendorAnalytics(Long vendorId, LocalDate startDate, LocalDate endDate);
    
    // US 10: Get analytics for all vendors (for comparison)
    List<VendorAnalyticsResponse> getAllVendorsAnalytics(LocalDate startDate, LocalDate endDate);
    
    // US 10 AC #7: Export analytics
    byte[] exportVendorAnalyticsCsv(Long vendorId, LocalDate startDate, LocalDate endDate);
    
    // US 10 AC #9: Auto-update metrics (called after quotation/PO events)
    void updateVendorMetrics(Long vendorId);
    
    // US 10 AC #8: Calculate performance rating
    double calculatePerformanceScore(Long vendorId);
}
