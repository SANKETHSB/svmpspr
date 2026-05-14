package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * US-RBAC: Maps a CustomRole to a module with CRUD-level permission flags.
 *
 * AC #2  – Permissions assignable at module level.
 * AC #3  – CRUD-level granularity: canCreate, canRead, canUpdate, canDelete.
 * AC #11 – No restart required: permissions read from DB on every request.
 */
@Entity
@Table(name = "role_permissions",
       uniqueConstraints = @UniqueConstraint(name = "uq_role_module", columnNames = {"role_id", "module"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private CustomRole role;

    /**
     * Module name — matches the system modules:
     * VENDORS, RFQS, QUOTATIONS, PURCHASE_ORDERS, USERS,
     * AUDIT_LOGS, NOTIFICATIONS, COMPLIANCE, ANALYTICS, DASHBOARD
     */
    @Column(nullable = false, length = 100)
    private String module;

    /** AC #3: CRUD-level granularity */
    @Builder.Default private boolean canCreate = false;
    @Builder.Default private boolean canRead   = false;
    @Builder.Default private boolean canUpdate = false;
    @Builder.Default private boolean canDelete = false;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
