package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * US-RBAC AC #10: Immutable record of every role assignment/removal for a user.
 */
@Entity
@Table(name = "role_assignment_history",
       indexes = {
           @Index(name = "idx_rah_user",  columnList = "userId"),
           @Index(name = "idx_rah_actor", columnList = "actorId")
       })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleAssignmentHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user whose role changed */
    @Column(nullable = false)
    private Long userId;

    @Column(length = 200)
    private String userName;

    /** Previous role value */
    @Column(length = 100)
    private String previousRole;

    /** New role value */
    @Column(nullable = false, length = 100)
    private String newRole;

    /** Who made the change */
    @Column(nullable = false)
    private Long actorId;

    @Column(length = 200)
    private String actorName;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    private LocalDateTime changedAt;
}
