package com.infosys.svpms.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoleAssignmentHistoryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String previousRole;
    private String newRole;
    private Long actorId;
    private String actorName;
    private String reason;
    private LocalDateTime changedAt;
}
