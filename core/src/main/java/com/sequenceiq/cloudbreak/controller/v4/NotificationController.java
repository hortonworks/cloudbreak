package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
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
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setEventTimestamp(new Date().getTime());
        notification.setUserId(userService.getOrCreate(cloudbreakUser).getUserId());
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage(), messageArgs));
        notification.setTenantName(cloudbreakUser.getTenant());
        if (workspaceMessage) {
            Long workspaceId = restRequestThreadLocalService.getRequestedWorkspaceId();
            notification.setWorkspaceId(workspaceId);
        }
        notificationSender.send(new Notification<>(notification));
    }
}
