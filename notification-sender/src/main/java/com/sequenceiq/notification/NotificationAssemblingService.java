package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;

@Component
public class NotificationAssemblingService {
    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private EntitlementService entitlementsService;

    public Notification<CloudbreakNotification> createNotification(CloudbreakNotification notification) {
        return new Notification<>(notification);
    }

    public Notification<CloudbreakNotification> createNotification(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload) {
        CloudbreakNotification notification = new CloudbreakNotification();
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventType(resourceEvent.name());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage(), messageArgs));
        String accountId = getAccountId();
        if (accountId != null) {
            notification.setSubscriptionRequired(
                entitlementsService.isEntitledFor(accountId, Entitlement.PERSONAL_VIEW_CB_BY_RIGHT)
            );
        }
        notification.setTenantName(getAccountId());
        notification.setUserId(ThreadBasedUserCrnProvider.getUserCrn());
        if (payload != null) {
            notification.setPayload(JsonUtil.convertToTree(payload));
            notification.setPayloadType(payload.getClass().getSimpleName());
        }
        return new Notification<>(notification);
    }

    public Notification<CloudbreakNotification> createNotification(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload, String userId) {
        Notification<CloudbreakNotification> n = createNotification(resourceEvent, messageArgs, payload);
        n.getNotification().setTenantName(getTenantName(userId));
        n.getNotification().setUserId(userId);
        String accountId = getTenantName(userId);
        if (accountId != null) {
            n.getNotification().setSubscriptionRequired(
                    entitlementsService.isEntitledFor(accountId, Entitlement.PERSONAL_VIEW_CB_BY_RIGHT)
            );
        }
        return n;
    }

    private String getTenantName(String userId) {
        return Optional.ofNullable(Crn.fromString(userId))
                .map(Crn::getAccountId)
                .orElse(null);
    }

    private String getAccountId() {
        try {
            return ThreadBasedUserCrnProvider.getAccountId();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
