package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quotations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rfq_id", "vendor_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quotation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Double taxPercentage;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private Integer deliveryDays;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuotationStatus status = QuotationStatus.SUBMITTED;

    private Double weightedScore;

    @Column(length = 1000)
    private String evaluationComment;

    private String evaluatedBy;
    private LocalDateTime evaluatedAt;

    @Builder.Default
    @Column(name = "is_awarded")
    private boolean awarded = false;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationItem> items;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL)
    private List<QuotationDocument> documents;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum QuotationStatus {
        SUBMITTED, UNDER_EVALUATION, AWARDED, REJECTED
    }
}
