package com.infosys.svpms.service;

import com.infosys.svpms.entity.Notification;
import com.infosys.svpms.dto.response.NotificationPreferenceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    void send(Long recipientId, String recipientType, String title,
              String message, Notification.NotificationType type,
              Long refId, String refType);
    Page<Notification> getMyNotifications(Long recipientId, String recipientType, Pageable pageable);
    long getUnreadCount(Long recipientId, String recipientType);
    void markRead(Long notificationId, Long recipientId);
    void markAllRead(Long recipientId, String recipientType);
    
    /**
     * US 13 AC #7: Mark notification as unread
     */
    void markUnread(Long notificationId, Long recipientId);
    
    /**
     * US 13 AC #10: Get notification preferences for a user
     */
    List<NotificationPreferenceResponse> getPreferences(Long userId, String userType);
    
    /**
     * US 13 AC #10: Update notification preferences
     */
    void updatePreferences(Long userId, String userType, 
                          Map<Notification.NotificationType, Map<String, Boolean>> preferences);
    
    /**
     * US 13 AC #10: Reset preferences to defaults
     */
    void resetPreferences(Long userId, String userType);
}
