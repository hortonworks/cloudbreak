package com.sequenceiq.environment.credential.service;

import java.util.Collections;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.notification.Notification;
import com.sequenceiq.notification.NotificationSender;

@Service
public class CredentialNotificationService {

    protected static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    private final NotificationSender notificationSender;

    private final CloudbreakMessagesService messagesService;

    public CredentialNotificationService(NotificationSender notificationSender, CloudbreakMessagesService messagesService) {
        this.notificationSender = notificationSender;
        this.messagesService = messagesService;
    }

    public void send(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakNotification notification = new CloudbreakNotification();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.getCloudPlatform());
        notificationSender.send(new Notification<>(notification), Collections.emptyList(), RestClientUtil.get());
    }
}
