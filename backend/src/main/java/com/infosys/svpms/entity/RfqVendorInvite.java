package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_vendor_invites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rfq_id", "vendor_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RfqVendorInvite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @CreationTimestamp
    private LocalDateTime invitedAt;

    @Builder.Default
    private boolean hasResponded = false;
}
