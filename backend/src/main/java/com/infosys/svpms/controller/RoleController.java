package com.infosys.svpms.controller;

import com.infosys.svpms.dto.request.RoleRequest;
import com.infosys.svpms.dto.response.ApiResponse;
import com.infosys.svpms.dto.response.RoleAssignmentHistoryResponse;
import com.infosys.svpms.dto.response.RoleResponse;
import com.infosys.svpms.service.RoleService;
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
import java.util.Map;

/**
 * Role & Permission Management Controller
 *
 * AC #1  – CRUD endpoints for roles
 * AC #2  – Module-level permission assignment
 * AC #3  – CRUD-level granularity per module
 * AC #4  – Changes reflected immediately (DB-driven)
 * AC #5  – Delete requires confirmation flag
 * AC #6  – All endpoints protected by @PreAuthorize
 * AC #8  – All actions logged via RoleService → AuditService
 * AC #9  – System roles protected
 * AC #10 – Role assignment history endpoints
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Role & Permission Management")
public class RoleController {

    private final RoleService roleService;

    // ── AC #1: Get all roles ──────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all roles with their permissions")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok("Roles fetched", roleService.getAllRoles()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Role fetched", roleService.getRoleById(id)));
    }

    // ── AC #1: Create role ────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new role with module permissions (AC #1,#2,#3)")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Role created", roleService.createRole(req, ud.getUsername())));
    }

    // ── AC #1: Edit role ──────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update role name, description and all permissions (AC #1,#2,#3)")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Role updated", roleService.updateRole(id, req, ud.getUsername())));
    }

    // ── AC #1, #5: Delete role with confirmation ──────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete role — requires confirmed=true (AC #1,#5,#9)")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean confirmed,
            @AuthenticationPrincipal UserDetails ud) {
        // AC #5: Require explicit confirmation before deleting active roles
        if (!confirmed) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Deletion requires confirmed=true. " +
                    "Verify no users are assigned this role before confirming."));
        }
        roleService.deleteRole(id, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Role deleted"));
    }

    // ── AC #2,#3: Update single module permissions ────────────────────────────
    @PatchMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update CRUD permissions for a specific module (AC #2,#3,#4)")
    public ResponseEntity<ApiResponse<RoleResponse>> updateModulePermissions(
            @PathVariable Long id,
            @RequestBody RoleRequest.ModulePermissionRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Permissions updated",
            roleService.updateModulePermissions(id, req, ud.getUsername())));
    }

    // ── AC #4: Assign role to user ────────────────────────────────────────────
    @PatchMapping("/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a role to a user — reflected immediately (AC #4,#10)")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        String roleName = body.get("roleName");
        String reason   = body.getOrDefault("reason", null);
        if (roleName == null || roleName.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("roleName is required"));
        }
        roleService.assignRoleToUser(userId, roleName, reason, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Role assigned successfully"));
    }

    // ── AC #10: Role assignment history ──────────────────────────────────────
    @GetMapping("/assignment-history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all role assignment history (AC #10)")
    public ResponseEntity<ApiResponse<List<RoleAssignmentHistoryResponse>>> getAllHistory() {
        return ResponseEntity.ok(ApiResponse.ok("History fetched",
            roleService.getAllRoleAssignmentHistory()));
    }

    @GetMapping("/assignment-history/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get role assignment history for a specific user (AC #10)")
    public ResponseEntity<ApiResponse<List<RoleAssignmentHistoryResponse>>> getUserHistory(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("User history fetched",
            roleService.getRoleAssignmentHistory(userId)));
    }

    // ── AC #7: Get permissions for current user's role (used by frontend) ─────
    @GetMapping("/my-permissions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get permissions for the current user's role (AC #7)")
    public ResponseEntity<ApiResponse<List<RoleResponse.PermissionResponse>>> getMyPermissions(
            @AuthenticationPrincipal UserDetails ud) {
        // Extract role name from authority (strip ROLE_ prefix)
        String roleName = ud.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("");
        return ResponseEntity.ok(ApiResponse.ok("Permissions fetched",
            roleService.getPermissionsForRole(roleName)));
    }
}
