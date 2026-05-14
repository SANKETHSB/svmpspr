package com.infosys.svpms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * US 08: RFQ Award Request DTO
 * Ensures award reason is mandatory and only one vendor is selectable
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfqAwardRequest {
    
    /**
     * The quotation ID to award (only one vendor selectable)
     */
    @NotNull(message = "Quotation ID is mandatory")
    private Long quotationId;
    
    /**
     * Award reason is mandatory for transparency and audit
     */
    @NotBlank(message = "Award reason is mandatory")
    private String awardReason;
    
    /**
     * Confirmation flag - must be true to proceed with award
     * This enforces the confirmation dialog requirement
     */
    @NotNull(message = "Award confirmation is required")
    private Boolean confirmed;
}
