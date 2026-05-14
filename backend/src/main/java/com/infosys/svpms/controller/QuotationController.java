package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.*;
import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/quotations")
@RequiredArgsConstructor
@Tag(name = "Quotation Management")
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Submit quotation for an RFQ")
    public ResponseEntity<ApiResponse<QuotationResponse>> submit(
            @Valid @RequestBody QuotationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Quotation submitted", quotationService.submit(req, ud.getUsername())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Resubmit/update quotation before deadline")
    public ResponseEntity<ApiResponse<QuotationResponse>> resubmit(
            @PathVariable Long id,
            @Valid @RequestBody QuotationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation updated", quotationService.resubmit(id, req, ud.getUsername())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<QuotationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation fetched", quotationService.getById(id)));
    }

    @GetMapping("/rfq/{rfqId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Get all quotations for an RFQ (sorted by amount)")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getByRfq(@PathVariable Long rfqId) {
        return ResponseEntity.ok(ApiResponse.ok("Quotations fetched", quotationService.getByRfq(rfqId)));
    }

    @GetMapping("/rfq/{rfqId}/compare")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Compare all quotations for an RFQ")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> compare(@PathVariable Long rfqId) {
        return ResponseEntity.ok(ApiResponse.ok("Comparison ready", quotationService.compareByRfq(rfqId)));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','VENDOR')")
    @Operation(summary = "Get quotations by vendor")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(ApiResponse.ok("Quotations fetched", quotationService.getByVendor(vendorId)));
    }

    @PostMapping("/{id}/evaluate")
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Evaluate and score a quotation")
    public ResponseEntity<ApiResponse<QuotationResponse>> evaluate(
            @PathVariable Long id,
            @Valid @RequestBody QuotationEvaluationRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation evaluated", quotationService.evaluate(id, req, ud.getUsername())));
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Upload supporting document to quotation")
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails ud) {
        quotationService.uploadDocument(id, file, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded successfully with integrity verification"));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get quotation supporting documents")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Documents fetched", quotationService.getDocuments(id)));
    }

    @GetMapping("/rfq/{rfqId}/export-pdf")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Export quotation comparison to PDF")
    public ResponseEntity<byte[]> exportComparisonToPdf(@PathVariable Long rfqId) {
        byte[] pdf = quotationService.exportComparisonToPdf(rfqId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quotation_comparison_" + rfqId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download/preview quotation document")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails ud) {
        byte[] fileContent = quotationService.downloadDocument(documentId, ud.getUsername());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(fileContent);
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Finalize quotation submission (requires supporting documents)")
    public ResponseEntity<ApiResponse<QuotationResponse>> finalizeSubmission(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Quotation finalized successfully", 
            quotationService.finalizeSubmission(id, ud.getUsername())));
    }
}
