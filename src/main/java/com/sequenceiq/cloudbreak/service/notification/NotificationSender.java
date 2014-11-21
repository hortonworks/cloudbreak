package com.sequenceiq.cloudbreak.service.notification;

public interface NotificationSender {

    void send(Notification notification);
}
