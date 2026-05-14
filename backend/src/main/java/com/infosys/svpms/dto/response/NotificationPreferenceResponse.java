package com.infosys.svpms.dto.response;

import com.infosys.svpms.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * US 13 AC #10: Response DTO for notification preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private Notification.NotificationType notificationType;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
}
