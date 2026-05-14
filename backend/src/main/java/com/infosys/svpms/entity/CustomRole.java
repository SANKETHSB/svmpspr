package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * US-RBAC: Custom Role entity.
 * Default system roles (ADMIN, PROCUREMENT_MANAGER, COMPLIANCE_OFFICER) are
 * stored in User.Role enum and are protected — they cannot be deleted.
 * Custom roles created by admins are stored here.
 *
 * AC #1  – Admin can create/edit/delete roles.
 * AC #9  – Default system roles are protected (system = true).
 * AC #10 – Role assignment history maintained via RoleAssignmentHistory.
 */
@Entity
@Table(name = "custom_roles", uniqueConstraints = @UniqueConstraint(name = "uq_role_name", columnNames = "name"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomRole {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique role name, e.g. "FINANCE_REVIEWER" */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * AC #9: System roles cannot be deleted or renamed.
     * Seeded at startup for ADMIN, PROCUREMENT_MANAGER, COMPLIANCE_OFFICER.
     */
    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    /** AC #5: Track whether any users are currently assigned this role */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RolePermission> permissions;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
