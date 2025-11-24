package com.sequenceiq.environment.credential.service;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.notification.WebSocketNotification;
import com.sequenceiq.notification.WebSocketNotificationService;

@Service
public class CredentialNotificationService {

    protected static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    private final WebSocketNotificationService webSocketNotificationService;

    private final CloudbreakMessagesService messagesService;

    public CredentialNotificationService(WebSocketNotificationService webSocketNotificationService, CloudbreakMessagesService messagesService) {
        this.webSocketNotificationService = webSocketNotificationService;
        this.messagesService = messagesService;
    }

    public void send(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakNotification notification = new CloudbreakNotification();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.getCloudPlatform());
        webSocketNotificationService.send(new WebSocketNotification<>(notification));
    }
}
