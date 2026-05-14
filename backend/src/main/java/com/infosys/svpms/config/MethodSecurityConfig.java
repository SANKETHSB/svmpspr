package com.infosys.svpms.config;

import com.infosys.svpms.security.RbacPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * AC #6: Registers the custom RbacPermissionEvaluator so that
 * @PreAuthorize("hasPermission(#module, 'READ')") works across all controllers.
 * AC #11: No restart required — evaluator reads permissions from DB on every call.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class MethodSecurityConfig {

    private final RbacPermissionEvaluator rbacPermissionEvaluator;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(rbacPermissionEvaluator);
        return handler;
    }
}
