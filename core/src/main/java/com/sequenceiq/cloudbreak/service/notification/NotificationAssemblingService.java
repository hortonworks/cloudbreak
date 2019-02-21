package com.sequenceiq.cloudbreak.service.notification;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.notification.Notification;

@Component
public class NotificationAssemblingService<T> {
    public Notification<T> createNotification(T notification) {
        return new Notification(notification);
    }
}
