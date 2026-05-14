package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.PORequest;
import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.entity.PurchaseOrder;
import com.infosys.svpms.service.PurchaseOrderService;
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
import java.util.Map;

/**
 * US 09: Purchase Order Management Controller
 * 
 * Handles automated PO generation and management
 */
@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Order Management", description = "US 09: Automated PO generation and management")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    /**
     * US 09: Generate Purchase Order from awarded RFQ
     * 
     * Acceptance Criteria:
     * - PO number auto-generated
     * - PO inherits quotation data
     * - Total cost validated
     * - PDF generated
     * - Vendor notified
     * - PO status = Generated
     * - Delivery date required
     * - Only awarded RFQ eligible
     * - Audit logged
     */
    @PostMapping("/rfq/{rfqId}")
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Generate PO from awarded RFQ (US 09)", 
               description = "Automatically generates a Purchase Order from an awarded RFQ. " +
                           "PO number is auto-generated, data is inherited from quotation, " +
                           "PDF is generated, and vendor is notified.")
    public ResponseEntity<ApiResponse<POResponse>> generate(
            @PathVariable Long rfqId,
            @Valid @RequestBody PORequest req,
            @AuthenticationPrincipal UserDetails ud) {
        POResponse response = poService.generate(rfqId, req, ud.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Purchase Order generated successfully. Vendor has been notified.", response));
    }

    /**
     * US 09: List all POs with filters (Searchable via filters)
     */
    /**
     * US 09: List all POs with filters
     * US 12 AC #3: Filter POs by vendor, date range, and status
     * US 12 AC #4: Multi-criteria filtering
     * US 12 AC #5: Pagination support
     * US 12 AC #6: Sorting support
     * US 12 AC #11: Filters resettable (by omitting parameters)
     * 
     * Query Parameters:
     * - search: Search term for PO number or RFQ number
     * - vendorId: Filter by vendor ID
     * - status: GENERATED, ISSUED, RECEIVED, CLOSED, CANCELLED
     * - dateFrom: Filter POs generated after this date (ISO-8601 format)
     * - dateTo: Filter POs generated before this date (ISO-8601 format)
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - sort: Sort field and direction (e.g., "generatedAt,desc" or "poNumber,asc")
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Search and filter POs (US 09, US 12)", 
               description = "Advanced search with multi-criteria filtering. " +
                           "Search by PO number or RFQ number. " +
                           "Filter by vendor, status, and date range. " +
                           "Supports pagination and sorting.")
    public ResponseEntity<ApiResponse<Page<POResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) PurchaseOrder.POStatus status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "generatedAt,desc") String sort) {
        
        // US 12 AC #6: Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Parse date parameters (ISO-8601 format)
        java.time.LocalDateTime dateFromParsed = null;
        java.time.LocalDateTime dateToParsed = null;
        
        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            try {
                dateFromParsed = java.time.LocalDateTime.parse(dateFrom);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Invalid dateFrom format. Use ISO-8601: yyyy-MM-ddTHH:mm:ss"));
            }
        }
        
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            try {
                dateToParsed = java.time.LocalDateTime.parse(dateTo);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Invalid dateTo format. Use ISO-8601: yyyy-MM-ddTHH:mm:ss"));
            }
        }
        
        Page<POResponse> pos = poService.getAll(search, vendorId, status, dateFromParsed, dateToParsed,
            PageRequest.of(page, size, Sort.by(sortDirection, sortField)));
        return ResponseEntity.ok(ApiResponse.ok("Purchase Orders fetched successfully", pos));
    }

    /**
     * US 09: Get PO by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get PO by ID", 
               description = "Retrieve detailed information about a specific Purchase Order.")
    public ResponseEntity<ApiResponse<POResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order fetched", poService.getById(id)));
    }

    /**
     * US 09: Export PO as PDF (PO exportable)
     */
    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','VENDOR')")
    @Operation(summary = "Download PO as PDF (US 09)", 
               description = "Export Purchase Order as a professional PDF document with all details.")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        byte[] pdf = poService.exportPdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PO-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    /**
     * US 09: Update PO status
     * Note: PO is immutable after issuance, only status can be updated
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Update PO status (US 09)", 
               description = "Update Purchase Order status. PO is immutable after issuance - only status can be changed.")
    public ResponseEntity<ApiResponse<POResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        PurchaseOrder.POStatus status = PurchaseOrder.POStatus.valueOf(body.get("status"));
        POResponse response = poService.updateStatus(id, status, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Purchase Order status updated successfully", response));
    }
}
