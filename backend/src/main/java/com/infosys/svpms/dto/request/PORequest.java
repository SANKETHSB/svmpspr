package com.infosys.svpms.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PORequest {
    @NotNull @Future public LocalDate deliveryDate;
    @NotBlank public String shippingAddress;
    public String paymentTerms;
    public String specialInstructions;
}
