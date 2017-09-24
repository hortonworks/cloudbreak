package com.sequenceiq.cloudbreak.service.notification;

public interface NotificationSender {
    <T> void send(Notification<T> notification);
}
