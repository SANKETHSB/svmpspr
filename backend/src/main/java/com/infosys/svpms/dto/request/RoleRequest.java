package com.infosys.svpms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class RoleRequest {
    @NotBlank @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    /** List of module-level permission assignments */
    private List<ModulePermissionRequest> permissions;

    @Data
    public static class ModulePermissionRequest {
        @NotBlank
        private String module;
        private boolean canCreate;
        private boolean canRead;
        private boolean canUpdate;
        private boolean canDelete;
    }
}
