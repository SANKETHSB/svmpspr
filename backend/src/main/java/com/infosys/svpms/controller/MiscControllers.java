package com.infosys.svpms.controller;

import com.infosys.svpms.dto.response.*;
import com.infosys.svpms.entity.*;
import com.infosys.svpms.repository.*;
import com.infosys.svpms.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
class NotificationController {
    private final NotificationService notifService;
    private final UserRepository userRepo;
    private final VendorRepository vendorRepo;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Notification>>> getMine(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var info = resolveActor(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Notifications",
            notifService.getMyNotifications((Long) info[0], (String) info[1],
                PageRequest.of(page, size))));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@AuthenticationPrincipal UserDetails ud) {
        var info = resolveActor(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Count",
            Map.of("count", notifService.getUnreadCount((Long) info[0], (String) info[1]))));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        var info = resolveActor(ud.getUsername());
        notifService.markRead(id, (Long) info[0]);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read"));
    }

    /**
     * US 13 AC #7: Mark notification as unread
     */
    @PatchMapping("/{id}/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as unread (US 13 AC #7)", 
               description = "Mark a read notification as unread for follow-up")
    public ResponseEntity<ApiResponse<Void>> markUnread(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        var info = resolveActor(ud.getUsername());
        notifService.markUnread(id, (Long) info[0]);
        return ResponseEntity.ok(ApiResponse.ok("Marked as unread"));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal UserDetails ud) {
        var info = resolveActor(ud.getUsername());
        notifService.markAllRead((Long) info[0], (String) info[1]);
        return ResponseEntity.ok(ApiResponse.ok("All marked as read"));
    }

    private Object[] resolveActor(String email) {
        var u = userRepo.findByEmail(email);
        if (u.isPresent()) return new Object[]{u.get().getId(), "USER"};
        var v = vendorRepo.findByEmail(email);
        if (v.isPresent()) return new Object[]{v.get().getId(), "VENDOR"};
        return new Object[]{0L, "USER"};
    }
}

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit & Compliance")
class AuditController {
    private final AuditService auditService;
    private final UserRepository userRepo;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogs(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : null;
        LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : null;
        Page<AuditLog> result = auditService.getLogs(actorId, action, entityType, from, to, PageRequest.of(page, size));
        // US 14 #12 — log access events themselves.
        recordAccess(ud, "AUDIT_VIEW",
            String.format("Viewed audit logs (filters: actorId=%s, action=%s, entityType=%s, from=%s, to=%s, page=%d, size=%d)",
                actorId, action, entityType, from, to, page, size));
        return ResponseEntity.ok(ApiResponse.ok("Audit logs", result));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Export audit logs as CSV (with optional date range)")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate) : null;
        LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate) : null;
        byte[] csv = auditService.exportCsv(entityType, from, to);
        // US 14 #12 — exporting is a privileged read; record it.
        recordAccess(ud, "AUDIT_EXPORT",
            String.format("Exported audit logs CSV (entityType=%s, from=%s, to=%s, bytes=%d)",
                entityType, from, to, csv.length));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @GetMapping("/integrity")
    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Verify the audit hash chain (US 14 #11)")
    public ResponseEntity<ApiResponse<com.infosys.svpms.dto.AuditIntegrityReport>> verify(
            @AuthenticationPrincipal UserDetails ud) {
        var report = auditService.verifyIntegrity();
        recordAccess(ud, "AUDIT_INTEGRITY_CHECK",
            String.format("Integrity check: intact=%s verified=%d/%d %s",
                report.isIntact(), report.getVerifiedRecords(), report.getTotalRecords(), report.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok("Audit integrity report", report));
    }

    @PostMapping("/retention/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force-run the retention purge (US 14 #9). Normally automatic.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runRetention(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(defaultValue = "365") int retentionDays) {
        int purged = auditService.applyRetention(retentionDays);
        recordAccess(ud, "AUDIT_RETENTION_TRIGGERED",
            "Manual retention run (days=" + retentionDays + ", purged=" + purged + ")");
        return ResponseEntity.ok(ApiResponse.ok("Retention complete",
            Map.of("retentionDays", retentionDays, "purged", purged)));
    }

    /** Self-audit helper for US 14 #12 (log access events). */
    private void recordAccess(UserDetails ud, String action, String description) {
        if (ud == null) return;
        var u = userRepo.findByEmail(ud.getUsername());
        if (u.isPresent()) {
            auditService.log(u.get().getId(), "USER", u.get().getName(),
                action, "AuditLog", 0L, description);
        }
    }
}

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
class DashboardController {
    private final com.infosys.svpms.service.impl.DashboardServiceImpl dashboardService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER','COMPLIANCE_OFFICER')")
    @Operation(summary = "Get procurement KPI metrics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getKpis() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats", dashboardService.getStats()));
    }
}
