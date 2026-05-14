package com.infosys.svpms.dto.response;
import com.infosys.svpms.entity.Quotation;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuotationResponse {
    private Long id;
    private Long rfqId;
    private String rfqNumber;
    private Long vendorId;
    private String vendorName;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private Double taxPercentage;
    private String currency;
    private Integer deliveryDays;
    private String notes;
    private Quotation.QuotationStatus status;
    private Double weightedScore;
    private String evaluationComment;
    private String evaluatedBy;
    private LocalDateTime evaluatedAt;
    private boolean awarded;
    private boolean lowestBidder; // US 07: Highlight lowest bidder
    private List<QuotationItemResponse> items;
    private LocalDateTime submittedAt;
    private int documentCount; // US 07: Document count for preview
}
