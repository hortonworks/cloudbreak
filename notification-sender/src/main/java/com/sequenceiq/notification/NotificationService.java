package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;

@Component
public class NotificationService {

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private NotificationAssemblingService notificationAssemblingService;

    @Value("${notification.urls:}")
    private String notificationUrls;

    public void send(Notification<CloudbreakNotification> notification) {
        notificationSender.send(notification, getNotificationUrls(), RestClientUtil.get());
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
        notificationSender.send(
                notificationAssemblingService.createNotification(resourceEvent, messageArgs, payload),
                getNotificationUrls(),
                RestClientUtil.get());
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload, String userId) {
        notificationSender.send(
                notificationAssemblingService.createNotification(resourceEvent, messageArgs, payload, userId),
                getNotificationUrls(),
                RestClientUtil.get());
    }

    private List<String> getNotificationUrls() {
        return Lists.newArrayList(notificationUrls.trim().split(","))
                .stream()
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
    }

}
