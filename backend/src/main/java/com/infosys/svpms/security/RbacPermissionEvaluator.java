package com.infosys.svpms.security;

import com.infosys.svpms.entity.RolePermission;
import com.infosys.svpms.repository.RolePermissionRepository;
import com.infosys.svpms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * US-RBAC AC #6: Backend permission enforcement via Spring Security's PermissionEvaluator.
 * Used with @PreAuthorize("hasPermission(#module, 'READ')") etc.
 *
 * AC #11: No restart required — permissions are read from DB on every evaluation.
 * AC #12: Unauthorized access attempts are logged to audit trail.
 *
 * Default system roles (ADMIN, PROCUREMENT_MANAGER, COMPLIANCE_OFFICER) retain
 * their existing @PreAuthorize rules and are NOT evaluated here — they bypass
 * this evaluator via the SYSTEM_ROLE_FULL_ACCESS set.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacPermissionEvaluator implements PermissionEvaluator {

    private final RolePermissionRepository permissionRepo;
    private final AuditService auditService;

    /** AC #9: System roles always have full access — bypass DB lookup */
    private static final Set<String> SYSTEM_ROLE_FULL_ACCESS = Set.of(
        "ROLE_ADMIN", "ROLE_PROCUREMENT_MANAGER", "ROLE_COMPLIANCE_OFFICER", "ROLE_VENDOR"
    );

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || !auth.isAuthenticated()) return false;

        // System roles bypass granular permission check
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (SYSTEM_ROLE_FULL_ACCESS.contains(ga.getAuthority())) return true;
        }

        String module = targetDomainObject != null ? targetDomainObject.toString() : "";
        String action = permission != null ? permission.toString().toUpperCase() : "";
        String roleName = extractRoleName(auth);

        boolean granted = evaluate(roleName, module, action);

        if (!granted) {
            // AC #12: Log unauthorized access attempt
            logUnauthorizedAttempt(auth.getName(), roleName, module, action);
        }

        return granted;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return hasPermission(auth, targetType, permission);
    }

    // -------------------------------------------------------------------------
    // Core evaluation — reads from DB (AC #11: no restart needed)
    // -------------------------------------------------------------------------
    private boolean evaluate(String roleName, String module, String action) {
        List<RolePermission> perms = permissionRepo.findByRoleName(roleName);
        return perms.stream()
            .filter(p -> p.getModule().equalsIgnoreCase(module))
            .findFirst()
            .map(p -> switch (action) {
                case "CREATE" -> p.isCanCreate();
                case "READ"   -> p.isCanRead();
                case "UPDATE" -> p.isCanUpdate();
                case "DELETE" -> p.isCanDelete();
                default       -> false;
            })
            .orElse(false);
    }

    private String extractRoleName(Authentication auth) {
        return auth.getAuthorities().stream()
            .findFirst()
            .map(ga -> ga.getAuthority().replace("ROLE_", ""))
            .orElse("UNKNOWN");
    }

    // -------------------------------------------------------------------------
    // AC #12: Log unauthorized access attempts
    // -------------------------------------------------------------------------
    private void logUnauthorizedAttempt(String email, String role, String module, String action) {
        try {
            String ip = getCurrentIp();
            log.warn("[AC #12] UNAUTHORIZED ACCESS ATTEMPT: user={}, role={}, module={}, action={}, ip={}",
                email, role, module, action, ip);
            auditService.log(
                0L, "USER", email,
                "UNAUTHORIZED_ACCESS_ATTEMPT",
                module, 0L,
                null, null,
                "Unauthorized " + action + " attempt on module " + module
                    + " by " + email + " (role=" + role + ", ip=" + ip + ")"
            );
        } catch (Exception e) {
            log.error("Failed to log unauthorized attempt: {}", e.getMessage());
        }
    }

    private String getCurrentIp() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest req = attrs.getRequest();
            String fwd = req.getHeader("X-Forwarded-For");
            return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
