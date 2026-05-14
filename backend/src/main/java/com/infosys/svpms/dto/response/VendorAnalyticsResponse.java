package com.infosys.svpms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorAnalyticsResponse {
    
    // US 10 AC #1: Total RFQs participated count
    private Long vendorId;
    private String vendorName;
    private int totalRfqsParticipated;
    
    // US 10 AC #2: Win ratio calculation
    private int totalRfqsWon;
    private double winRatio; // percentage
    
    // US 10 AC #3: Average bid amount
    private BigDecimal averageBidAmount;
    private String currency;
    
    // US 10 AC #4: On-time PO fulfillment tracking
    private int totalPOs;
    private int onTimePOs;
    private double onTimeDeliveryRate; // percentage
    
    // US 10 AC #8: Performance rating system
    private double performanceScore; // 0-100
    private String performanceGrade; // A, B, C, D, F
    
    // US 10 AC #5: Graphical trends (time series data)
    private List<TrendData> quotationTrends;
    private List<TrendData> winTrends;
    
    // US 10 AC #11: Historical comparison
    private HistoricalComparison historicalComparison;
    
    // Additional metrics
    private int totalQuotationsSubmitted;
    private int rejectedQuotations;
    private BigDecimal totalValueWon;
    private BigDecimal lowestBid;
    private BigDecimal highestBid;
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendData {
        private String period; // YYYY-MM format
        private long count;
        private BigDecimal value;
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HistoricalComparison {
        private String comparisonPeriod;
        private double winRatioChange; // percentage change
        private double performanceScoreChange;
        private BigDecimal avgBidChange;
        private String trend; // IMPROVING, DECLINING, STABLE
    }
}
