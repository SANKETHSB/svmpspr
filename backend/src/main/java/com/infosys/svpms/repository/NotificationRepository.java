package com.infosys.svpms.repository;

import com.infosys.svpms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(Long recipientId, String recipientType, Pageable pageable);
    long countByRecipientIdAndRecipientTypeAndReadFalse(Long recipientId, String recipientType);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :id AND n.recipientType = :type")
    void markAllAsRead(@Param("id") Long id, @Param("type") String type);

    /**
     * US 13 AC #8: Find notifications older than specified date for retention policy
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoffDate")
    List<Notification> findOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
