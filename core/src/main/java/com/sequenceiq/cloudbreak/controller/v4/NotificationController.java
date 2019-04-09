package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakV4Event;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.notification.NotificationAssemblingService;
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

    protected final void executeAndNotify(Consumer<CloudbreakUser> consumer, Object payload, NotificationEventType eventType,
            WorkspaceResource resource, Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        consumer.accept(cloudbreakUser);
        notify(payload, eventType, resource, workspaceId);
    }

    protected final void notify(Object payload, NotificationEventType eventType, WorkspaceResource resource, Long workspaceId) {
        notify(payload, eventType, resource, workspaceId, Collections.emptySet());
    }

    protected final void notify(Object payload, NotificationEventType eventType, WorkspaceResource resource, Long workspaceId, Collection<?> messageArgs) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        CloudbreakV4Event notification = NotificationAssemblingService.cloudbreakEvent(payload, eventType, resource);
        notification.setUser(userService.getOrCreate(cloudbreakUser).getUserId());
        notification.setWorkspaceId(workspaceId);
        notification.setMessage(messagesService.getMessage(resource, eventType, messageArgs));
        notificationSender.send(new Notification<>(notification));
    }
}
