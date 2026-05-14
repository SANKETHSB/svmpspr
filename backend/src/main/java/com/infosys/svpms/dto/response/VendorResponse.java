package com.infosys.svpms.dto.response;
import com.infosys.svpms.entity.Vendor;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VendorResponse {
    private Long id;
    private String companyName;
    private String email;
    private String gstNumber;
    private String registrationId;
    private String phone;
    private String address;
    private String contactPerson;
    private Vendor.VendorStatus status;
    private boolean emailVerified;
    private boolean compliant;
    private double performanceScore;
    private int totalRfqsWon;
    private int totalRfqsParticipated;
    private String rejectionReason;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime registeredAt;
}
