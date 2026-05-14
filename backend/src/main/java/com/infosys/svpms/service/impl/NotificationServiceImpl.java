package com.infosys.svpms.service.impl;

import com.infosys.svpms.entity.Notification;
import com.infosys.svpms.entity.NotificationPreference;
import com.infosys.svpms.dto.response.NotificationPreferenceResponse;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.NotificationRepository;
import com.infosys.svpms.repository.NotificationPreferenceRepository;
import com.infosys.svpms.service.EmailService;
import com.infosys.svpms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * US 13: Notification Service Implementation
 * 
 * This service handles both in-app notifications and email notifications.
 * 
 * US 13 Acceptance Criteria Implementation:
 * 
 * AC #1: Email notification on vendor approval
 *   - Integrated with EmailService.sendVendorApprovalEmail()
 * 
 * AC #2: Email notification on RFQ assignment
 *   - Integrated with EmailService.sendRfqAssignmentEmail()
 * 
 * AC #3: Email notification on RFQ award
 *   - Integrated with EmailService.sendRfqAwardEmail()
 * 
 * AC #4: Email notification on PO issuance
 *   - Integrated with EmailService.sendPoIssuanceEmail()
 * 
 * AC #5: In-app notification panel displays unread notifications
 *   - getMyNotifications() returns notifications with isRead flag
 *   - getUnreadCount() provides badge count
 * 
 * AC #6: Notification timestamps visible
 *   - createdAt field populated automatically
 *   - Included in NotificationResponse
 * 
 * AC #7: Notifications marked as read/unread
 *   - markRead() marks single notification as read
 *   - markUnread() marks single notification as unread
 *   - markAllRead() marks all for user
 * 
 * AC #8: Notification history retained
 *   - Notifications stored in database
 *   - Retention policy configured via app.notification.retention-days
 * 
 * AC #9: Failed email delivery logged
 *   - EmailService wrapped in try-catch
 *   - Failures logged at ERROR level
 * 
 * AC #10: Users configure notification preferences
 *   - Notification preferences stored in database
 *   - Checked before sending notifications
 *   - Separate toggles for in-app and email
 * 
 * AC #11: No sensitive data in notifications
 *   - Messages contain only reference IDs
 *   - No passwords or tokens in notification content
 * 
 * AC #12: Notification dispatch non-blocking
 *   - @Async annotation on send() method
 *   - Email sending also @Async
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;
    private final NotificationPreferenceRepository preferenceRepo;
    private final EmailService emailService;

    /**
     * US 13 AC #12: Async notification dispatch to prevent blocking
     * US 13 AC #10: Check preferences before sending
     * 
     * This method sends both in-app notification and email notification
     * based on user preferences.
     */
    @Async
    @Override
    public void send(Long recipientId, String recipientType, String title,
                     String message, Notification.NotificationType type,
                     Long refId, String refType) {
        try {
            // US 13 AC #10: Check notification preferences
            NotificationPreference pref = getOrCreatePreference(recipientId, recipientType, type);
            
            boolean inAppEnabled = pref.getInAppEnabled();
            boolean emailEnabled = pref.getEmailEnabled();
            
            if (!inAppEnabled && !emailEnabled) {
                log.debug("[US 13 AC #10] Notification type {} disabled for {} {}",
                    type, recipientType, recipientId);
                return;
            }

            // US 13 AC #10: Send in-app notification if enabled
            if (inAppEnabled) {
                Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .recipientType(recipientType)
                    .title(title)
                    .message(message)
                    .type(type)
                    .referenceId(refId)
                    .referenceType(refType)
                    .build();
                
                Notification saved = repo.save(notification);
                
                log.info("[US 13 AC #5] In-app notification created: {} (ID: {}, Type: {})",
                    title, saved.getId(), type);
            }

            // US 13 AC #10: Send email notification if enabled
            if (emailEnabled) {
                String recipientLabel = recipientType + "#" + recipientId;
                emailService.sendGenericNotificationEmail(null, recipientLabel, title, message);
            }
            
        } catch (Exception e) {
            log.error("[US 13 AC #9] Failed to create notification: {}", e.getMessage());
            // Don't rethrow - AC #12: Don't block core workflow
        }
    }

    /**
     * US 13 AC #5: Get notifications with unread status
     */
    @Override
    public Page<Notification> getMyNotifications(Long recipientId, String recipientType, Pageable pageable) {
        log.debug("[US 13 AC #5] Fetching notifications for {} {}", recipientType, recipientId);
        return repo.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, recipientType, pageable);
    }

    /**
     * US 13 AC #5: Get unread notification count for badge
     */
    @Override
    public long getUnreadCount(Long recipientId, String recipientType) {
        long count = repo.countByRecipientIdAndRecipientTypeAndReadFalse(recipientId, recipientType);
        log.debug("[US 13 AC #5] Unread count for {} {}: {}", recipientType, recipientId, count);
        return count;
    }

    /**
     * US 13 AC #7: Mark single notification as read
     */
    @Override
    @Transactional
    public void markRead(Long id, Long recipientId) {
        try {
            Notification n = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
            
            if (!n.getRecipientId().equals(recipientId)) {
                throw new ResourceNotFoundException("Notification", "id", id);
            }
            
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            repo.save(n);
            
            log.info("[US 13 AC #7] Notification {} marked as read by recipient {}", id, recipientId);
        } catch (ResourceNotFoundException e) {
            log.warn("[US 13] Notification not found or access denied: ID {}, Recipient {}", id, recipientId);
            throw e;
        }
    }

    /**
     * US 13 AC #7: Mark single notification as unread
     */
    @Override
    @Transactional
    public void markUnread(Long id, Long recipientId) {
        try {
            Notification n = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
            
            if (!n.getRecipientId().equals(recipientId)) {
                throw new ResourceNotFoundException("Notification", "id", id);
            }
            
            n.setRead(false);
            n.setReadAt(null);
            repo.save(n);
            
            log.info("[US 13 AC #7] Notification {} marked as unread by recipient {}", id, recipientId);
        } catch (ResourceNotFoundException e) {
            log.warn("[US 13] Notification not found or access denied: ID {}, Recipient {}", id, recipientId);
            throw e;
        }
    }

    /**
     * US 13 AC #7: Mark all notifications as read
     */
    @Override
    @Transactional
    public void markAllRead(Long recipientId, String recipientType) {
        repo.markAllAsRead(recipientId, recipientType);
        log.info("[US 13 AC #7] Marked all notifications as read for {} {}",
            recipientType, recipientId);
    }

    /**
     * US 13 AC #10: Get notification preferences for a user
     */
    @Override
    public List<NotificationPreferenceResponse> getPreferences(Long userId, String userType) {
        List<NotificationPreferenceResponse> responses = new ArrayList<>();
        
        // Get or create preferences for all notification types
        for (Notification.NotificationType type : Notification.NotificationType.values()) {
            NotificationPreference pref = getOrCreatePreference(userId, userType, type);
            responses.add(NotificationPreferenceResponse.builder()
                .notificationType(type)
                .inAppEnabled(pref.getInAppEnabled())
                .emailEnabled(pref.getEmailEnabled())
                .build());
        }
        
        log.debug("[US 13 AC #10] Retrieved preferences for {} {}", userType, userId);
        return responses;
    }

    /**
     * US 13 AC #10: Update notification preferences
     */
    @Override
    @Transactional
    public void updatePreferences(Long userId, String userType,
                                   Map<Notification.NotificationType, Map<String, Boolean>> preferences) {
        for (Map.Entry<Notification.NotificationType, Map<String, Boolean>> entry : preferences.entrySet()) {
            Notification.NotificationType type = entry.getKey();
            Map<String, Boolean> settings = entry.getValue();
            
            NotificationPreference pref = getOrCreatePreference(userId, userType, type);
            pref.setInAppEnabled(settings.getOrDefault("inApp", true));
            pref.setEmailEnabled(settings.getOrDefault("email", true));
            preferenceRepo.save(pref);
        }
        
        log.info("[US 13 AC #10] Updated notification preferences for {} {}", userType, userId);
    }

    /**
     * US 13 AC #10: Reset preferences to defaults (all enabled)
     */
    @Override
    @Transactional
    public void resetPreferences(Long userId, String userType) {
        preferenceRepo.deleteByUserIdAndUserType(userId, userType);
        log.info("[US 13 AC #10] Reset notification preferences for {} {}", userType, userId);
    }

    /**
     * US 13 AC #10: Get or create preference with defaults
     */
    private NotificationPreference getOrCreatePreference(Long userId, String userType,
                                                          Notification.NotificationType type) {
        Optional<NotificationPreference> existing = preferenceRepo
            .findByUserIdAndUserTypeAndNotificationType(userId, userType, type);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create default preference (all enabled)
        NotificationPreference newPref = NotificationPreference.builder()
            .userId(userId)
            .userType(userType)
            .notificationType(type)
            .inAppEnabled(true)
            .emailEnabled(true)
            .build();
        
        return preferenceRepo.save(newPref);
    }
}
