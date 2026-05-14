package com.infosys.svpms.dto.response;

import com.infosys.svpms.entity.RFQ;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RfqRevisionHistoryResponse {
    private Long id;
    private Long rfqId;
    private String rfqNumber;
    private int revisionNumber;
    private String title;
    private String description;
    private String terms;
    private LocalDateTime deadline;
    private RFQ.RFQStatus status;
    private List<RfqItemResponse> items;
    private String modifiedByName;
    private String changeDescription;
    private LocalDateTime createdAt;
}
