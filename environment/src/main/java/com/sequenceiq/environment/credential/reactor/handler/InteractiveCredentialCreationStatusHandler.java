package com.sequenceiq.environment.credential.reactor.handler;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CREDENTIAL_AZURE_INTERACTIVE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CREDENTIAL_AZURE_INTERACTIVE_STATUS;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.notification.NotificationService;

import reactor.bus.Event;

@Component
public class InteractiveCredentialCreationStatusHandler implements EventHandler<InteractiveCredentialCreationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveCredentialCreationStatusHandler.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private NotificationService notificationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InteractiveCredentialCreationStatus.class);
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationStatus> interactiveCredentialCreationStatusEvent) {
        InteractiveCredentialCreationStatus interactiveCredentialCreationStatus = interactiveCredentialCreationStatusEvent.getData();
        String userCrn = interactiveCredentialCreationStatus.getCloudContext().getUserId();
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> {
            String message = interactiveCredentialCreationStatus.getMessage();
            InteractiveCredentialNotification notification = new InteractiveCredentialNotification()
                    .withEventTimestamp(new Date().getTime())
                    .withUserId(userCrn)
                    .withCloud(interactiveCredentialCreationStatus.getExtendedCloudCredential().getCloudPlatform())
                    .withEventMessage(message);
            ResourceEvent event;
            if (interactiveCredentialCreationStatus.isError()) {
                event = CREDENTIAL_AZURE_INTERACTIVE_FAILED;
                notification.withEventType(event.name());
                LOGGER.info("Interactive credential creation failed status: {}", new Json(notification).getValue());
            } else {
                event = CREDENTIAL_AZURE_INTERACTIVE_STATUS;
                notification.withEventType(event.name());
                LOGGER.info("Interactive credential creation success status: {}", new Json(notification).getValue());
            }
            notificationService.send(event, notification, ThreadBasedUserCrnProvider.getAccountId());
        });
    }

    static class InteractiveCredentialNotification {
        private String eventType;

        private long eventTimestamp;

        private String userId;

        private String cloud;

        private String eventMessage;

        InteractiveCredentialNotification() {
        }

        public String getEventType() {
            return eventType;
        }

        public long getEventTimestamp() {
            return eventTimestamp;
        }

        public String getUserId() {
            return userId;
        }

        public String getCloud() {
            return cloud;
        }

        public String getEventMessage() {
            return eventMessage;
        }

        InteractiveCredentialNotification withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        InteractiveCredentialNotification withEventTimestamp(long eventTimestamp) {
            this.eventTimestamp = eventTimestamp;
            return this;
        }

        InteractiveCredentialNotification withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        InteractiveCredentialNotification withCloud(String cloud) {
            this.cloud = cloud;
            return this;
        }

        InteractiveCredentialNotification withEventMessage(String eventMessage) {
            this.eventMessage = eventMessage;
            return this;
        }
    }
}
