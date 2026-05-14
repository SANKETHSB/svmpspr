package com.infosys.svpms.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RfqItemRequest {
    private Long id;
    @NotBlank @Size(max=200) public String itemName;
    @Size(max=500) public String description;
    @NotNull @Min(1) public Integer quantity;
    @NotBlank @Size(max=50) public String unit;
    public String specifications;
}
