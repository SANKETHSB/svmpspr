package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.*;
import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.entity.RFQ;
import com.infosys.svpms.service.RfqService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rfqs")
@RequiredArgsConstructor
@Tag(name = "RFQ Management")
public class RfqController {

    private final RfqService rfqService;

    @PostMapping
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Create new RFQ")
    public ResponseEntity<ApiResponse<RfqResponse>> create(
            @Valid @RequestBody RfqRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("RFQ created", rfqService.create(req, ud.getUsername())));
    }

    /**
     * US 12 AC #2: Filter RFQs by status (Open, Closed, Awarded, Archived)
     * US 12 AC #4: Multi-criteria filtering
     * US 12 AC #5: Pagination support
     * US 12 AC #6: Sorting support
     * US 12 AC #11: Filters resettable (by omitting parameters)
     * 
     * Query Parameters:
     * - search: Search term for title, RFQ number, or description
     * - status: OPEN, CLOSED, AWARDED, ARCHIVED
     * - createdById: Filter by creator user ID
     * - deadlineFrom: Filter RFQs with deadline after this date (ISO-8601 format)
     * - deadlineTo: Filter RFQs with deadline before this date (ISO-8601 format)
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - sort: Sort field and direction (e.g., "deadline,asc" or "createdAt,desc")
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Search and filter RFQs (US 12)", 
               description = "Advanced search with multi-criteria filtering. " +
                           "Search by title, RFQ number, or description. " +
                           "Filter by status, creator, and deadline range. " +
                           "Supports pagination and sorting.")
    public ResponseEntity<ApiResponse<Page<RfqResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RFQ.RFQStatus status,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) String deadlineFrom,
            @RequestParam(required = false) String deadlineTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        // US 12 AC #6: Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Parse date parameters (ISO-8601 format)
        java.time.LocalDateTime deadlineFromDate = null;
        java.time.LocalDateTime deadlineToDate = null;
        
        if (deadlineFrom != null && !deadlineFrom.trim().isEmpty()) {
            try {
                deadlineFromDate = java.time.LocalDateTime.parse(deadlineFrom);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Invalid deadlineFrom format. Use ISO-8601: yyyy-MM-ddTHH:mm:ss"));
            }
        }
        
        if (deadlineTo != null && !deadlineTo.trim().isEmpty()) {
            try {
                deadlineToDate = java.time.LocalDateTime.parse(deadlineTo);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Invalid deadlineTo format. Use ISO-8601: yyyy-MM-ddTHH:mm:ss"));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.ok("RFQs fetched",
            rfqService.getAll(search, status, createdById, deadlineFromDate, deadlineToDate,
                PageRequest.of(page, size, Sort.by(sortDirection, sortField)))));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get RFQs for a vendor")
    public ResponseEntity<ApiResponse<Page<RfqResponse>>> getForVendor(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor RFQs fetched",
            rfqService.getForVendor(vendorId, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RfqResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("RFQ fetched", rfqService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Update RFQ (only OPEN)")
    public ResponseEntity<ApiResponse<RfqResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RfqRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("RFQ updated", rfqService.update(id, req, ud.getUsername())));
    }

    @PostMapping("/{id}/invite-vendors")
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Invite additional vendors")
    public ResponseEntity<ApiResponse<RfqResponse>> inviteVendors(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Vendors invited",
            rfqService.inviteVendors(id, body.get("vendorIds"), ud.getUsername())));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Manually close RFQ")
    public ResponseEntity<ApiResponse<RfqResponse>> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("RFQ closed", rfqService.close(id, ud.getUsername())));
    }

    /**
     * US 08: Award RFQ to a vendor
     * 
     * Role-based validation: Only PROCUREMENT_MANAGER can award RFQs
     * Requires confirmation dialog (enforced by confirmed flag in request)
     * Award reason is mandatory (validated in RfqAwardRequest)
     */
    @PatchMapping("/{id}/award")
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Award RFQ to a vendor (US 08)", 
               description = "Awards the RFQ to the selected vendor. Requires confirmation and mandatory award reason. " +
                           "Only one vendor can be selected. RFQ will be locked from further edits after award.")
    public ResponseEntity<ApiResponse<RfqResponse>> award(
            @PathVariable Long id,
            @Valid @RequestBody RfqAwardRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        RfqResponse response = rfqService.award(id, request, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
            "RFQ awarded successfully. Purchase Order generation is now enabled.", 
            response));
    }

    /**
     * US 11 AC #6: Admin Override to Reopen Closed RFQ
     * 
     * Role-based validation: Only ADMIN can reopen RFQs
     * Requires mandatory reason for audit trail
     * New deadline must be in the future
     * Only CLOSED RFQs can be reopened (not AWARDED or ARCHIVED)
     */
    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reopen a closed RFQ (US 11 - Admin Override)", 
               description = "Allows administrators to reopen a CLOSED RFQ with a new deadline. " +
                           "Requires mandatory reason (min 20 characters) for audit trail. " +
                           "New deadline must be in the future. " +
                           "All invited vendors will be notified about the reopening. " +
                           "Cannot reopen AWARDED or ARCHIVED RFQs.")
    public ResponseEntity<ApiResponse<RfqResponse>> reopenRfq(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails ud) {
        
        // Extract and validate request parameters
        if (!body.containsKey("newDeadline") || body.get("newDeadline") == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("New deadline is required"));
        }
        
        if (!body.containsKey("reason") || body.get("reason") == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Reason is required for reopening RFQ"));
        }
        
        String newDeadlineStr = body.get("newDeadline").toString();
        String reason = body.get("reason").toString();
        
        // Parse deadline
        java.time.LocalDateTime newDeadline;
        try {
            newDeadline = java.time.LocalDateTime.parse(newDeadlineStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Invalid deadline format. Use ISO-8601 format: yyyy-MM-ddTHH:mm:ss"));
        }
        
        RfqResponse response = rfqService.reopenRfq(id, newDeadline, reason, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
            "RFQ reopened successfully. Vendors have been notified.", 
            response));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasRole('PROCUREMENT_MANAGER')")
    @Operation(summary = "Upload attachment to RFQ")
    public ResponseEntity<ApiResponse<String>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails ud) {
        rfqService.uploadAttachment(id, file, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded successfully"));
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get RFQ attachments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Attachments fetched", rfqService.getAttachments(id)));
    }

    @GetMapping("/{id}/revisions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get RFQ revision history")
    public ResponseEntity<ApiResponse<List<RfqRevisionHistoryResponse>>> getRevisionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Revision history fetched", rfqService.getRevisionHistory(id)));
    }

    @GetMapping("/{id}/revisions/{revisionNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get specific RFQ revision (read-only)")
    public ResponseEntity<ApiResponse<RfqRevisionHistoryResponse>> getRevisionByNumber(
            @PathVariable Long id,
            @PathVariable int revisionNumber) {
        return ResponseEntity.ok(ApiResponse.ok("Revision fetched", rfqService.getRevisionByNumber(id, revisionNumber)));
    }

    @GetMapping("/{id}/revisions/{revisionNumber}/export")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    @Operation(summary = "Export RFQ revision as text file")
    public ResponseEntity<byte[]> exportRevision(
            @PathVariable Long id,
            @PathVariable int revisionNumber) {
        byte[] export = rfqService.exportRfqRevision(id, revisionNumber);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rfq_revision_" + id + "_" + revisionNumber + ".txt")
            .contentType(MediaType.TEXT_PLAIN)
            .body(export);
    }
}
