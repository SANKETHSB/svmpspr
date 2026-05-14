package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rfqs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RFQ {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String rfqNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String terms;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RFQStatus status = RFQStatus.OPEN;

    @Builder.Default
    private int revisionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_vendor_id")
    private Vendor awardedVendor;

    @Column(length = 500)
    private String awardReason;

    private LocalDateTime awardedAt;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqItem> items;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL)
    private List<RfqVendorInvite> vendorInvites;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL)
    private List<Quotation> quotations;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum RFQStatus {
        OPEN, CLOSED, AWARDED, ARCHIVED
    }
}
