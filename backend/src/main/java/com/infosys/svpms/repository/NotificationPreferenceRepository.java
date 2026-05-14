package com.infosys.svpms.repository;

import com.infosys.svpms.entity.Notification;
import com.infosys.svpms.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * US 13 AC #10: Notification Preferences Repository
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Find all preferences for a specific user
     */
    List<NotificationPreference> findByUserIdAndUserType(Long userId, String userType);

    /**
     * Find specific preference for a user and notification type
     */
    Optional<NotificationPreference> findByUserIdAndUserTypeAndNotificationType(
        Long userId, String userType, Notification.NotificationType notificationType);

    /**
     * Delete all preferences for a user (for reset functionality)
     */
    void deleteByUserIdAndUserType(Long userId, String userType);
}
