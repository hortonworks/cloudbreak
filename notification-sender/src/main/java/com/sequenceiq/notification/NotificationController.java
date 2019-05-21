package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;

public abstract class NotificationController {

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Value("${notification.url:http://localhost:3000/notifications}")
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
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage(), messageArgs));
        notification.setTenantName(cloudbreakUser.getTenant());
        notification.setUserId(cloudbreakUser.getUserId());
        notificationSender.send(new Notification<>(notification), Collections.singletonList(notificationUrl), RestClientUtil.get());
    }
}
