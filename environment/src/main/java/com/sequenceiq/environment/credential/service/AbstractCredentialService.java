package com.sequenceiq.environment.credential.service;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.notification.Notification;
import com.sequenceiq.notification.NotificationSender;
import com.sequenceiq.notification.ResourceEvent;

public abstract class AbstractCredentialService {

    protected static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    private final Set<String> enabledPlatforms;

    private final NotificationSender notificationSender;

    private final CloudbreakMessagesService messagesService;

    protected AbstractCredentialService(NotificationSender notificationSender, CloudbreakMessagesService messagesService, Set<String> enabledPlatforms) {
        this.notificationSender = notificationSender;
        this.messagesService = messagesService;
        this.enabledPlatforms = enabledPlatforms;
    }

    protected void sendCredentialNotification(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakNotification notification = new CloudbreakNotification();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.getCloudPlatform());
        notificationSender.send(new Notification<>(notification), Collections.emptyList(), RestClientUtil.get());
    }

    protected Set<String> getEnabledPlatforms() {
        return enabledPlatforms;
    }
}
