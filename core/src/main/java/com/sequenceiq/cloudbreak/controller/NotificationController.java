package com.sequenceiq.cloudbreak.controller;

import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

public abstract class NotificationController {

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private NotificationSender notificationSender;

    protected final void executeAndNotify(Consumer<IdentityUser> consumer, ResourceEvent resourceEvent) {
        IdentityUser user = authenticatedUserService.getCbUser();
        consumer.accept(user);
        notify(user, resourceEvent);
    }

    protected final void notify(IdentityUser user, ResourceEvent resourceEvent) {
        Notification notification = new Notification();
        notification.setEventTimestamp(new Date());
        notification.setOwner(user.getUserId());
        notification.setAccount(user.getAccount());
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notificationSender.send(notification);
    }
}
