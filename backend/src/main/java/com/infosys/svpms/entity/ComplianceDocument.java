package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplianceDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, length = 100)
    private String documentType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    @Column(length = 100)
    private String fileType;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Builder.Default
    @Column(name = "is_expired")
    private boolean expired = false;

    @Builder.Default
    @Column(name = "expiry_warning_sent")
    private boolean expiryWarningSent = false;

    @Builder.Default
    private int version = 1;

    @Builder.Default
    @Column(name = "is_latest")
    private boolean latest = true;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}
