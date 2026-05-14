package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_revision_history")
@Immutable
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RfqRevisionHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rfqId;

    @Column(nullable = false)
    private String rfqNumber;

    @Column(nullable = false)
    private int revisionNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String terms;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    private RFQ.RFQStatus status;

    @Column(columnDefinition = "TEXT")
    private String itemsSnapshot; // JSON snapshot of items

    @Column(nullable = false)
    private Long modifiedBy;

    @Column(nullable = false)
    private String modifiedByName;

    @Column(length = 1000)
    private String changeDescription;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
