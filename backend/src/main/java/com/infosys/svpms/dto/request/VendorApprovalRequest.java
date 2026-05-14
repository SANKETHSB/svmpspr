package com.infosys.svpms.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorApprovalRequest {
    private String action; // APPROVE, REJECT, SUSPEND
    
    @NotBlank(message = "Reason is mandatory for rejection/suspension")
    private String reason;
}
