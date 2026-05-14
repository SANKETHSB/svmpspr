package com.infosys.svpms.dto.response;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RfqItemResponse {
    private Long id;
    private String itemName;
    private String description;
    private Integer quantity;
    private String unit;
    private String specifications;
}
