package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.restclient.RestClientUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;
import com.sequenceiq.cloudbreak.user.UserService;

public abstract class NotificationController {

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private UserService userService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Value("${notification.url:http://localhost:8089}")
    private String notificationUrl;

    protected final void executeAndNotify(Consumer<CloudbreakUser> consumer, ResourceEvent resourceEvent) {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        consumer.accept(cloudbreakUser);
        notify(resourceEvent);
    }

    protected final void notify(ResourceEvent resourceEvent) {
        notify(resourceEvent, Collections.emptySet());
    }

    protected final void notify(ResourceEvent resourceEvent, Collection<?> messageArgs) {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        CloudbreakNotification notification = new CloudbreakNotification();
        notification.setEventTimestamp(new Date().getTime());
        notification.setUserId(userService.getOrCreate(cloudbreakUser).getUserId());
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage(), messageArgs));
        notification.setTenantName(cloudbreakUser.getTenant());
        notificationSender.send(new Notification<>(notification), Collections.singletonList(notificationUrl), RestClientUtil.get());
    }
}
