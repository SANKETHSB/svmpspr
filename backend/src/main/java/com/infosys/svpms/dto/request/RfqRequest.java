package com.infosys.svpms.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RfqRequest {
    @NotBlank @Size(max=200) public String title;
    @Size(max=2000) public String description;
    @NotBlank public String terms;
    @NotNull @Future public LocalDateTime deadline;
    @NotEmpty @Valid public List<RfqItemRequest> items;
    @NotEmpty public List<Long> invitedVendorIds;
}
