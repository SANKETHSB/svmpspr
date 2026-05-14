package com.infosys.svpms.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data public class QuotationItemRequest {
    @NotNull public Long rfqItemId;
    @NotNull @DecimalMin("0.01") public BigDecimal unitPrice;
}
