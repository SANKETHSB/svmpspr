package com.infosys.svpms.dto.request;

import com.infosys.svpms.entity.Notification;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * US 13 AC #10: Request DTO for updating notification preferences
 */
@Data
public class NotificationPreferenceRequest {

    @NotNull(message = "Notification type is required")
    private Notification.NotificationType notificationType;

    @NotNull(message = "In-app enabled flag is required")
    private Boolean inAppEnabled;

    @NotNull(message = "Email enabled flag is required")
    private Boolean emailEnabled;
}
