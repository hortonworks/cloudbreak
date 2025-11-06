package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakNotification;

@Component
public class WebSocketNotificationAssemblingService {
    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private EntitlementService entitlementsService;

    public WebSocketNotification<CloudbreakNotification> createWebSocketNotification(CloudbreakNotification notification) {
        return new WebSocketNotification<>(notification);
    }

    public WebSocketNotification<CloudbreakNotification> createWebSocketNotification(
            ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload, String notificationType) {
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
        notification.setNotificationType(notificationType);
        if (payload != null) {
            notification.setPayload(JsonUtil.convertToTree(payload));
            notification.setPayloadType(payload.getClass().getSimpleName());
        }
        return new WebSocketNotification<>(notification);
    }

    public WebSocketNotification<CloudbreakNotification> createWebSocketNotification(
            ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload, String userId, String notificationType) {
        WebSocketNotification<CloudbreakNotification> n = createWebSocketNotification(resourceEvent, messageArgs, payload, notificationType);
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
