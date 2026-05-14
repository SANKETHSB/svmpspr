package com.infosys.svpms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.svpms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * AC #12: Logs every unauthorized (403) access attempt to the audit trail
 * and returns a structured JSON error response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String email = "anonymous";
        String role  = "UNKNOWN";

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            email = auth.getName();
            role  = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");
        }

        String ip     = getIp(request);
        String method = request.getMethod();
        String uri    = request.getRequestURI();

        // AC #12: Log to audit trail
        log.warn("[AC #12] ACCESS DENIED: user={}, role={}, method={}, uri={}, ip={}",
            email, role, method, uri, ip);

        try {
            auditService.log(
                0L, "USER", email,
                "UNAUTHORIZED_ACCESS_ATTEMPT",
                "HTTP", 0L,
                null, null,
                "Access denied: " + method + " " + uri +
                    " by " + email + " (role=" + role + ", ip=" + ip + ")"
            );
        } catch (Exception e) {
            log.error("Failed to audit access denial: {}", e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
            "success", false,
            "message", "Access denied. You do not have permission to perform this action.",
            "path", uri
        ));
    }

    private String getIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
