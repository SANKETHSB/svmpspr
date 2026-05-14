package com.infosys.svpms.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class QuotationEvaluationRequest {
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") public Double score;
    @NotBlank @Size(min=10, max=1000) public String comment; // US 07: Evaluation comments mandatory before award
    public boolean award;
}
