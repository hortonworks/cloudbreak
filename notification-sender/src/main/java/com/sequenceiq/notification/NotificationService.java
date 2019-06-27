package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;

@Component
public class NotificationService {

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private NotificationAssemblingService notificationAssemblingService;

    @Value("${notification.url:http://localhost:3000/notifications}")
    private String notificationUrl;

    public void send(Notification<CloudbreakNotification> notification) {
        notificationSender.send(notification, Collections.singletonList(notificationUrl), RestClientUtil.get());
    }

    public void send(ResourceEvent resourceEvent) {
        send(resourceEvent, Collections.emptySet(), null, null);
    }

    public void send(ResourceEvent resourceEvent, String userId) {
        send(resourceEvent, Collections.emptySet(), null, userId);
    }

    public void send(ResourceEvent resourceEvent, Object payload, String userId) {
        send(resourceEvent, Collections.emptySet(), payload, userId);
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload) {
        notificationSender.send(notificationAssemblingService.createNotification(resourceEvent, messageArgs, payload),
                Collections.singletonList(notificationUrl), RestClientUtil.get());
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload, String userId) {
        notificationSender.send(notificationAssemblingService.createNotification(resourceEvent, messageArgs, payload, userId),
                Collections.singletonList(notificationUrl), RestClientUtil.get());
    }

}
