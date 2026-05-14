package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 20)
    private String recipientType; // USER or VENDOR

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Builder.Default
    @Column(name = "is_read")
    private boolean read = false;

    private Long referenceId;
    private String referenceType;

    private LocalDateTime readAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NotificationType {
        VENDOR_APPROVED, VENDOR_REJECTED, RFQ_ASSIGNED, RFQ_AWARDED,
        RFQ_CLOSED, PO_ISSUED, COMPLIANCE_EXPIRY, GENERAL, ACCOUNT_LOCKED
    }
}
