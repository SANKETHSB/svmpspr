package com.infosys.svpms.dto.response;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long actorId;
    private String actorType;
    private String actorName;
    private String action;
    private String entityType;
    private Long entityId;
    private String oldValue;
    private String newValue;
    private String description;
    private String ipAddress;
    private LocalDateTime timestamp;
}
