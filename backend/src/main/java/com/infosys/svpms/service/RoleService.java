package com.infosys.svpms.service;

import com.infosys.svpms.dto.request.RoleRequest;
import com.infosys.svpms.dto.response.RoleAssignmentHistoryResponse;
import com.infosys.svpms.dto.response.RoleResponse;
import java.util.List;

public interface RoleService {
    /** AC #1: Create a new custom role with module permissions */
    RoleResponse createRole(RoleRequest req, String actorEmail);

    /** AC #1: Edit role name, description, and permissions */
    RoleResponse updateRole(Long roleId, RoleRequest req, String actorEmail);

    /** AC #1, #5: Delete role — requires confirmation, blocked if system role */
    void deleteRole(Long roleId, String actorEmail);

    /** Get all roles */
    List<RoleResponse> getAllRoles();

    /** Get single role by id */
    RoleResponse getRoleById(Long id);

    /** AC #2,#3: Update permissions for a specific module on a role */
    RoleResponse updateModulePermissions(Long roleId, RoleRequest.ModulePermissionRequest req, String actorEmail);

    /** AC #4: Assign role to a user — reflected immediately */
    void assignRoleToUser(Long userId, String roleName, String reason, String actorEmail);

    /** AC #10: Role assignment history */
    List<RoleAssignmentHistoryResponse> getRoleAssignmentHistory(Long userId);

    /** AC #10: All role assignment history (admin view) */
    List<RoleAssignmentHistoryResponse> getAllRoleAssignmentHistory();

    /** Get effective permissions for a role name (used by frontend for AC #7) */
    List<RoleResponse.PermissionResponse> getPermissionsForRole(String roleName);
}
