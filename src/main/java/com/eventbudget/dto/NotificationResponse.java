package com.eventbudget.dto;

import com.eventbudget.model.approval.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String subject,
        String message,
        boolean read,
        LocalDateTime sentAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getSubject(),
                notification.getMessage(),
                notification.isRead(),
                notification.getSentAt());
    }
}
