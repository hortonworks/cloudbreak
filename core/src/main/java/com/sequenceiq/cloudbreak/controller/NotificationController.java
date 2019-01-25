package com.sequenceiq.cloudbreak.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.model.event.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.user.UserService;

public abstract class NotificationController {

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    protected final void executeAndNotify(Consumer<CloudbreakUser> consumer, ResourceEvent resourceEvent) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        consumer.accept(cloudbreakUser);
        notify(resourceEvent);
    }

    protected final void notify(ResourceEvent resourceEvent) {
        notify(resourceEvent, true);
    }

    protected final void notify(ResourceEvent resourceEvent, boolean workspaceMessage) {
        notify(resourceEvent, workspaceMessage, Collections.emptySet());
    }

    protected final void notify(ResourceEvent resourceEvent, boolean workspaceMessage, Collection<?> messageArgs) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventTimestamp(new Date().getTime());
        notification.setUserId(userService.getOrCreate(cloudbreakUser).getUserId());
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage(), messageArgs));
        if (workspaceMessage) {
            Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
            notification.setWorkspaceId(workspaceId);
        }
        notificationSender.send(new Notification<>(notification));
    }
}
