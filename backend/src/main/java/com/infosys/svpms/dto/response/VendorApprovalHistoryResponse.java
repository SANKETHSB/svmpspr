package com.infosys.svpms.dto.response;

import com.infosys.svpms.entity.Vendor;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorApprovalHistoryResponse {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private Long actorId;
    private String actorName;
    private Vendor.VendorStatus previousStatus;
    private Vendor.VendorStatus newStatus;
    private String reason;
    private String comments;
    private LocalDateTime actionTimestamp;
    private Long approvalTimeMinutes;
}
