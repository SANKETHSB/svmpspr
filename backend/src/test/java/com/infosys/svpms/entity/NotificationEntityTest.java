package com.infosys.svpms.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityTest {

    @Test
    void testNotificationBuilder() {
        Notification notification = Notification.builder()
                .title("Test Notification")
                .message("Test Message")
                .type(Notification.NotificationType.GENERAL)
                .read(false)
                .build();

        assertEquals("Test Notification", notification.getTitle());
        assertEquals("Test Message", notification.getMessage());
        assertEquals(Notification.NotificationType.GENERAL, notification.getType());
        assertFalse(notification.isRead());
    }

    @Test
    void testNotificationGettersAndSetters() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setType(Notification.NotificationType.RFQ_ASSIGNED);
        notification.setRead(true);

        assertEquals(1L, notification.getId());
        assertEquals("Title", notification.getTitle());
        assertEquals(Notification.NotificationType.RFQ_ASSIGNED, notification.getType());
        assertTrue(notification.isRead());
    }

    @Test
    void testNotificationTypeEnum() {
        assertTrue(Notification.NotificationType.values().length >= 5);
        assertEquals(Notification.NotificationType.GENERAL, Notification.NotificationType.valueOf("GENERAL"));
        assertEquals(Notification.NotificationType.RFQ_ASSIGNED, Notification.NotificationType.valueOf("RFQ_ASSIGNED"));
    }

    @Test
    void testNotificationDefaults() {
        Notification notification = Notification.builder().build();
        assertFalse(notification.isRead());
    }
}
