package com.infosys.svpms.dto.response;
import com.infosys.svpms.entity.PurchaseOrder;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * US 09: Purchase Order Response DTO
 * Contains all PO details including PDF path
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class POResponse {
    private Long id;
    private String poNumber;
    private Long rfqId;
    private String rfqNumber;
    private Long vendorId;
    private String vendorName;
    private Long quotationId;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate deliveryDate;
    private String shippingAddress;
    private String paymentTerms;
    private String specialInstructions;
    private PurchaseOrder.POStatus status;
    private String generatedByName;
    private LocalDateTime generatedAt;
    private String pdfPath; // US 09: PDF path for generated document
}
