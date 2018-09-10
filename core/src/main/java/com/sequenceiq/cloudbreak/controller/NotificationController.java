package com.sequenceiq.cloudbreak.controller;

import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public abstract class NotificationController {

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    protected final void executeAndNotify(Consumer<IdentityUser> consumer, ResourceEvent resourceEvent) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        consumer.accept(identityUser);
        notify(resourceEvent);
    }

    protected final void notify(ResourceEvent resourceEvent) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        Long orgId = restRequestThreadLocalService.getRequestedWorkspaceId();
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventTimestamp(new Date().getTime());
        notification.setUserIdV3(userService.getOrCreate(identityUser).getUserId());
        notification.setWorkspaceId(orgId);
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notificationSender.send(new Notification<>(notification));
    }
}
