package com.infosys.svpms.controller;

import com.infosys.svpms.dto.response.ApiResponse;
import com.infosys.svpms.dto.response.VendorAnalyticsResponse;
import com.infosys.svpms.service.VendorAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/vendor-analytics")
@RequiredArgsConstructor
@Tag(name = "Vendor Analytics")
public class VendorAnalyticsController {

    private final VendorAnalyticsService analyticsService;

    // US 10 AC #10: Dashboard access role restricted
    @GetMapping("/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Get vendor analytics with date range filter")
    public ResponseEntity<ApiResponse<VendorAnalyticsResponse>> getVendorAnalytics(
            @PathVariable Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // US 10 AC #6: Filter by date range
        VendorAnalyticsResponse analytics = analyticsService.getVendorAnalytics(vendorId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Vendor analytics fetched", analytics));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Get analytics for all vendors")
    public ResponseEntity<ApiResponse<List<VendorAnalyticsResponse>>> getAllVendorsAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<VendorAnalyticsResponse> analytics = analyticsService.getAllVendorsAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("All vendors analytics fetched", analytics));
    }

    // US 10 AC #7: Export analytics
    @GetMapping("/{vendorId}/export")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Export vendor analytics as CSV")
    public ResponseEntity<byte[]> exportAnalytics(
            @PathVariable Long vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        byte[] csv = analyticsService.exportVendorAnalyticsCsv(vendorId, startDate, endDate);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vendor-analytics-" + vendorId + ".csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    // US 10 AC #9: Auto-update metrics endpoint (can be called manually or by scheduler)
    @PostMapping("/{vendorId}/update-metrics")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Manually trigger metrics update for a vendor")
    public ResponseEntity<ApiResponse<String>> updateMetrics(@PathVariable Long vendorId) {
        analyticsService.updateVendorMetrics(vendorId);
        return ResponseEntity.ok(ApiResponse.ok("Vendor metrics updated successfully"));
    }
}
