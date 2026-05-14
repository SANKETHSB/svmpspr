package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rfq_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RfqItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 50)
    private String unit;

    private String specifications;
}
