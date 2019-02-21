package com.sequenceiq.cloudbreak.notification;

public interface NotificationSender {
    <T> void send(Notification<T> notification);

    void sendTestNotification(String userId);
}
