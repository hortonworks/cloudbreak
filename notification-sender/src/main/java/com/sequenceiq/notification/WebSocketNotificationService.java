package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;

@Component
public class WebSocketNotificationService {

    @Inject
    private WebSocketNotificationSender webSocketNotificationSender;

    @Inject
    private NotificationAssemblingService notificationAssemblingService;

    @Value("${notification.urls:}")
    private String notificationUrls;

    public void send(Notification<CloudbreakNotification> notification) {
        webSocketNotificationSender.send(notification, getNotificationUrls(), RestClientUtil.get());
    }

    public void send(ResourceEvent resourceEvent) {
        send(resourceEvent, Collections.emptySet(), null, null, null);
    }

    public void send(ResourceEvent resourceEvent, String userId) {
        send(resourceEvent, Collections.emptySet(), null, userId, null);
    }

    public void send(ResourceEvent resourceEvent, Object payload, String userId) {
        send(resourceEvent, Collections.emptySet(), payload, userId, null);
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload) {
        webSocketNotificationSender.send(
                notificationAssemblingService.createNotification(resourceEvent, messageArgs, payload, null),
                getNotificationUrls(),
                RestClientUtil.get());
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload, String userId, String notificationType) {
        webSocketNotificationSender.send(
                notificationAssemblingService.createNotification(resourceEvent, messageArgs, payload, userId, notificationType),
                getNotificationUrls(),
                RestClientUtil.get());
    }

    private List<String> getNotificationUrls() {
        return Lists.newArrayList(notificationUrls.trim().split(","))
                .stream()
                .filter(Objects::nonNull)
                .filter(e -> !e.isEmpty())
                .map(url -> {
                    if (isUrlMissProtocol(url)) {
                        return "http://" + url;
                    }
                    return url;
                })
                .collect(Collectors.toList());
    }

    private boolean isUrlMissProtocol(String url) {
        return !(url.startsWith("http://") ^ url.startsWith("https://"));
    }

}
