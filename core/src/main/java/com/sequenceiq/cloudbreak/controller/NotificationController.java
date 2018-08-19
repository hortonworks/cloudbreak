package com.sequenceiq.cloudbreak.controller;

import java.util.Date;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public abstract class NotificationController {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private UserService userService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    protected final void executeAndNotify(Consumer<IdentityUser> consumer, ResourceEvent resourceEvent) {
        IdentityUser user = authenticatedUserService.getCbUser();
        consumer.accept(user);
        notify(resourceEvent);
    }

    protected final void notify(ResourceEvent resourceEvent) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        Long orgId = restRequestThreadLocalService.getRequestedOrgId();
        if (orgId == null) {
            orgId = organizationService.getDefaultOrganizationForCurrentUser().getId();
        }
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventTimestamp(new Date().getTime());
        notification.setUserIdV3(userService.getCurrentUser().getUserId());
        notification.setOrganizationId(orgId);
        notification.setOwner(identityUser.getUserId());
        notification.setAccount(identityUser.getAccount());
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notificationSender.send(new Notification<>(notification));
    }
}
