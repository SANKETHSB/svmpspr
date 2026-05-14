package com.infosys.svpms.dto.response;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplianceDocResponse {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private String documentType;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private boolean expired;
    private boolean latest;
    private int version;
    private LocalDateTime uploadedAt;
    /** Days until expiry — negative means already expired */
    private long daysUntilExpiry;
}
