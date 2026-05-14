package com.infosys.svpms.dto.response;
import com.infosys.svpms.entity.RFQ;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RfqResponse {
    private Long id;
    private String rfqNumber;
    private String title;
    private String description;
    private String terms;
    private LocalDateTime deadline;
    private RFQ.RFQStatus status;
    private int revisionNumber;
    private String createdByName;
    private Long createdById;
    private String awardedVendorName;
    private Long awardedVendorId;
    private String awardReason;
    private LocalDateTime awardedAt;
    private List<RfqItemResponse> items;
    private List<VendorResponse> invitedVendors;
    private int quotationCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
