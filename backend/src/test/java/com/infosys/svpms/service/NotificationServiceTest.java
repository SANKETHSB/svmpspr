package com.infosys.svpms.service;

import com.infosys.svpms.entity.Notification;
import com.infosys.svpms.entity.NotificationPreference;
import com.infosys.svpms.exception.ResourceNotFoundException;
import com.infosys.svpms.repository.NotificationPreferenceRepository;
import com.infosys.svpms.repository.NotificationRepository;
import com.infosys.svpms.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepo;

    @Mock
    private NotificationPreferenceRepository preferenceRepo;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;
    private NotificationPreference testPreference;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .recipientId(1L)
                .recipientType("USER")
                .title("Test Notification")
                .message("Test Message")
                .type(Notification.NotificationType.GENERAL)
                .referenceId(1L)
                .referenceType("RFQ")
                .read(false)
                .build();

        testPreference = NotificationPreference.builder()
                .id(1L)
                .userId(1L)
                .userType("USER")
                .notificationType(Notification.NotificationType.GENERAL)
                .inAppEnabled(true)
                .emailEnabled(true)
                .build();
    }

    @Test
    void testSendNotification_Success() {
        // Arrange
        when(preferenceRepo.findByUserIdAndUserTypeAndNotificationType(anyLong(), anyString(), any()))
                .thenReturn(Optional.of(testPreference));
        when(notificationRepo.save(any(Notification.class))).thenReturn(testNotification);
        doNothing().when(emailService).sendGenericNotificationEmail(nullable(String.class), anyString(), anyString(), anyString());

        // Act
        notificationService.send(1L, "USER", "Test Title", "Test Message", 
                Notification.NotificationType.GENERAL, 1L, "RFQ");

        // Assert
        verify(notificationRepo, times(1)).save(any(Notification.class));
        verify(emailService, times(1)).sendGenericNotificationEmail(nullable(String.class), anyString(), anyString(), anyString());
    }

    @Test
    void testSendNotification_InAppDisabled_OnlyEmailSent() {
        // Arrange
        testPreference.setInAppEnabled(false);
        testPreference.setEmailEnabled(true);
        when(preferenceRepo.findByUserIdAndUserTypeAndNotificationType(anyLong(), anyString(), any()))
                .thenReturn(Optional.of(testPreference));
        doNothing().when(emailService).sendGenericNotificationEmail(nullable(String.class), anyString(), anyString(), anyString());

        // Act
        notificationService.send(1L, "USER", "Test Title", "Test Message", 
                Notification.NotificationType.GENERAL, 1L, "RFQ");

        // Assert
        verify(notificationRepo, never()).save(any(Notification.class));
        verify(emailService, times(1)).sendGenericNotificationEmail(nullable(String.class), anyString(), anyString(), anyString());
    }

    @Test
    void testGetMyNotifications_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(testNotification));
        when(notificationRepo.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(anyLong(), anyString(), any(Pageable.class)))
                .thenReturn(notificationPage);

        // Act
        Page<Notification> result = notificationService.getMyNotifications(1L, "USER", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetUnreadCount_Success() {
        // Arrange
        when(notificationRepo.countByRecipientIdAndRecipientTypeAndReadFalse(anyLong(), anyString()))
                .thenReturn(5L);

        // Act
        long count = notificationService.getUnreadCount(1L, "USER");

        // Assert
        assertEquals(5L, count);
    }

    @Test
    void testMarkRead_Success() {
        // Arrange
        when(notificationRepo.findById(anyLong())).thenReturn(Optional.of(testNotification));
        when(notificationRepo.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.markRead(1L, 1L);

        // Assert
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void testMarkRead_NotFound_ThrowsException() {
        // Arrange
        when(notificationRepo.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.markRead(1L, 1L);
        });
    }

    @Test
    void testMarkUnread_Success() {
        // Arrange
        testNotification.setRead(true);
        when(notificationRepo.findById(anyLong())).thenReturn(Optional.of(testNotification));
        when(notificationRepo.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        notificationService.markUnread(1L, 1L);

        // Assert
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void testMarkAllRead_Success() {
        // Arrange
        doNothing().when(notificationRepo).markAllAsRead(anyLong(), anyString());

        // Act
        notificationService.markAllRead(1L, "USER");

        // Assert
        verify(notificationRepo, times(1)).markAllAsRead(1L, "USER");
    }
}
