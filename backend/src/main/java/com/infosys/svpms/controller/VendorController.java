package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.*;
import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor Management")
public class VendorController {

    private final VendorService vendorService;
    private final ComplianceDocumentService complianceService;

    @PostMapping("/register")
    @Operation(summary = "Public vendor self-registration")
    public ResponseEntity<ApiResponse<VendorResponse>> register(@Valid @RequestBody VendorRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Registration submitted. Awaiting admin approval.", vendorService.register(req)));
    }

    /**
     * US 12 AC #1: Search vendors by name, GST, registration ID
     * US 12 AC #4: Multi-criteria filtering
     * US 12 AC #5: Pagination support
     * US 12 AC #6: Sorting support (via sort parameter)
     * US 12 AC #11: Filters resettable (by omitting parameters)
     * 
     * Query Parameters:
     * - search: Search term for company name, GST, or registration ID
     * - status: PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED
     * - compliant: true/false (compliance document status)
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - sort: Sort field and direction (e.g., "companyName,asc" or "registeredAt,desc")
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Search and filter vendors (US 12)", 
               description = "Advanced search with multi-criteria filtering. " +
                           "Search by company name, GST, or registration ID. " +
                           "Filter by status and compliance. " +
                           "Supports pagination and sorting.")
    public ResponseEntity<ApiResponse<Page<VendorResponse>>> getAll(
            @RequestParam(required=false) String search,
            @RequestParam(required=false) Vendor.VendorStatus status,
            @RequestParam(required=false) Boolean compliant,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(defaultValue="registeredAt,desc") String sort) {
        
        // US 12 AC #6: Parse sort parameter (field,direction)
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        return ResponseEntity.ok(ApiResponse.ok("Vendors fetched",
            vendorService.getAll(search, status, compliant, 
                PageRequest.of(page, size, Sort.by(sortDirection, sortField)))));
    }

    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<VendorResponse>>> getApproved() {
        return ResponseEntity.ok(ApiResponse.ok("Approved vendors", vendorService.getApproved()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorResponse>> getMyProfile(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", vendorService.getMyProfile(ud.getUsername())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER','VENDOR')")
    public ResponseEntity<ApiResponse<VendorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor fetched", vendorService.getById(id)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor approved", vendorService.approve(id, ud.getUsername())));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorResponse>> reject(
            @PathVariable Long id,
            @RequestBody VendorApprovalRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor rejected", vendorService.reject(id, req.getReason(), ud.getUsername())));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorResponse>> suspend(
            @PathVariable Long id,
            @RequestBody VendorApprovalRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor suspended", vendorService.suspend(id, req.getReason(), ud.getUsername())));
    }

    @GetMapping("/{id}/compliance-documents")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER','VENDOR','PROCUREMENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<ComplianceDocResponse>>> getDocs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Documents fetched", complianceService.getByVendor(id)));
    }

    @GetMapping("/{id}/compliance-documents/history/{documentType}")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER','VENDOR','PROCUREMENT_MANAGER')")
    @Operation(summary = "Get version history for a compliance document type (AC #8)")
    public ResponseEntity<ApiResponse<List<ComplianceDocResponse>>> getDocVersionHistory(
            @PathVariable Long id,
            @PathVariable String documentType) {
        return ResponseEntity.ok(ApiResponse.ok("Version history fetched",
            complianceService.getVersionHistory(id, documentType)));
    }

    @GetMapping("/compliance-summary")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Admin compliance summary — AC #6")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getComplianceSummary() {
        return ResponseEntity.ok(ApiResponse.ok("Compliance summary fetched",
            complianceService.getComplianceSummary()));
    }

    @GetMapping("/compliance-status")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "All vendors with compliance status — AC #6")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getAllComplianceStatus() {
        return ResponseEntity.ok(ApiResponse.ok("Vendor compliance status fetched",
            complianceService.getAllVendorComplianceStatus()));
    }

    @GetMapping("/{id}/approval-history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get vendor approval history")
    public ResponseEntity<ApiResponse<List<VendorApprovalHistoryResponse>>> getApprovalHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Approval history fetched", vendorService.getApprovalHistory(id)));
    }

    @PostMapping("/{id}/compliance-documents")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Upload compliance document (AC #1,#2,#7,#8,#9)")
    public ResponseEntity<ApiResponse<ComplianceDocResponse>> uploadDoc(
            @PathVariable Long id,
            @RequestParam String documentType,
            @RequestParam String issueDate,
            @RequestParam String expiryDate,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Document uploaded",
            complianceService.upload(id, documentType, issueDate, expiryDate, file, ud.getUsername())));
    }

    @GetMapping("/{vendorId}/compliance-documents/{docId}/download")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Download/preview compliance document")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDoc(
            @PathVariable Long vendorId,
            @PathVariable Long docId) {
        return complianceService.downloadDocument(docId);
    }
}
