package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String poNumber;

    @OneToOne
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @OneToOne
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    private String shippingAddress;
    private String paymentTerms;
    private String specialInstructions;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private POStatus status = POStatus.GENERATED;

    @ManyToOne
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    private String pdfPath;

    @CreationTimestamp
    private LocalDateTime generatedAt;

    public enum POStatus {
        GENERATED, SENT, RECEIVED, CLOSED
    }
}
