package com.infosys.svpms.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuotationRequest {
    @NotNull public Long rfqId;
    @NotNull @DecimalMin("0.01") public BigDecimal totalAmount;
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") public Double taxPercentage;
    @NotBlank @Size(max=10) public String currency;
    @NotNull @Min(1) public Integer deliveryDays;
    @Size(max=1000) public String notes;
    @NotEmpty @Valid public List<QuotationItemRequest> items;
}
