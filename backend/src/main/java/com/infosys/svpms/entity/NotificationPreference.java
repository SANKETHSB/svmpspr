package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * US 13 AC #10: Notification Preferences Entity
 * 
 * Stores user preferences for notification delivery channels.
 * Each user can configure whether they want to receive each notification type
 * via in-app notifications and/or email.
 */
@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(
           name = "unique_user_type_notif",
           columnNames = {"user_id", "user_type", "notification_type"}
       ))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType; // USER or VENDOR

    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Notification.NotificationType notificationType;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
