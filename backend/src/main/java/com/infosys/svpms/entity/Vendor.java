package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vendors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vendor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 15)
    private String gstNumber;

    @Column(nullable = false, unique = true, length = 100)
    private String registrationId;

    @Column(length = 15)
    private String phone;

    @Column(length = 500)
    private String address;

    private String contactPerson;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING_APPROVAL;

    @Builder.Default
    @Column(name = "is_email_verified")
    private boolean emailVerified = false;

    @Builder.Default
    @Column(name = "is_compliant")
    private boolean compliant = true;

    @Builder.Default
    private double performanceScore = 0.0;

    @Builder.Default
    private int totalRfqsWon = 0;

    @Builder.Default
    private int totalRfqsParticipated = 0;

    private String rejectionReason;
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ComplianceDocument> complianceDocuments;

    @CreationTimestamp
    private LocalDateTime registeredAt;

    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private int failedLoginCount = 0;

    @Builder.Default
    @Column(name = "is_locked")
    private boolean locked = false;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public enum VendorStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED
    }
}
