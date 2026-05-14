package com.infosys.svpms.scheduler;

import com.infosys.svpms.entity.Notification;
import com.infosys.svpms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * US 13 AC #8: Notification Retention Scheduler
 * 
 * Automatically purges old notifications based on configured retention period.
 * Runs daily at 4 AM (configurable via app.notification.retention-cron).
 * 
 * Configuration:
 * - app.notification.retention-days: Number of days to retain notifications (default: 365)
 * - app.notification.retention-cron: Cron expression for purge schedule (default: 0 0 4 * * *)
 * 
 * Set retention-days to 0 to disable automatic purging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetentionScheduler {

    private final NotificationRepository notificationRepository;

    @Value("${app.notification.retention-days:365}")
    private int retentionDays;

    /**
     * US 13 AC #8: Purge old notifications based on retention policy
     * 
     * Runs daily at 4 AM by default (configurable via cron expression)
     */
    @Scheduled(cron = "${app.notification.retention-cron:0 0 4 * * *}")
    @Transactional
    public void purgeOldNotifications() {
        if (retentionDays <= 0) {
            log.debug("[US 13 AC #8] Notification retention disabled (retention-days = {})", retentionDays);
            return;
        }

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            
            log.info("[US 13 AC #8] Starting notification retention purge. Cutoff date: {}", cutoffDate);
            
            List<Notification> oldNotifications = notificationRepository.findOlderThan(cutoffDate);
            int count = oldNotifications.size();
            
            if (count == 0) {
                log.info("[US 13 AC #8] No notifications to purge");
                return;
            }
            
            notificationRepository.deleteAll(oldNotifications);
            
            log.info("[US 13 AC #8] Notification retention purge completed. Deleted {} notifications older than {} days",
                count, retentionDays);
            
        } catch (Exception e) {
            log.error("[US 13 AC #8] Failed to purge old notifications: {}", e.getMessage(), e);
        }
    }
}
