package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_approval_history")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorApprovalHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vendor.VendorStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vendor.VendorStatus newStatus;

    @Column(length = 500)
    private String reason;

    @Column(length = 500)
    private String comments;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime actionTimestamp;

    @Column(nullable = false)
    private Long approvalTimeMinutes; // Time taken from registration to approval
}
