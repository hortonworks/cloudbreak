package com.sequenceiq.environment.credential.reactor.handler;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class InteractiveCredentialCreationStatusHandler implements EventHandler<InteractiveCredentialCreationStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveCredentialCreationStatusHandler.class);

//    @Inject
//    private NotificationSender notificationSender;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InteractiveCredentialCreationStatus.class);
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationStatus> interactiveCredentialCreationStatusEvent) {
        InteractiveCredentialCreationStatus interactiveCredentialCreationStatus = interactiveCredentialCreationStatusEvent.getData();
        String message = interactiveCredentialCreationStatus.getMessage();
        InteractiveCredentialNotification notification = new InteractiveCredentialNotification()
                .withEventTimestamp(new Date().getTime())
                .withUserId(interactiveCredentialCreationStatus.getCloudContext().getUserId())
                .withCloud(interactiveCredentialCreationStatus.getExtendedCloudCredential().getCloudPlatform())
                .withEventMessage(message);

        if (interactiveCredentialCreationStatus.isError()) {
            notification = notification.withEventType("CREDENTIAL_CREATE_FAILED");
            LOGGER.info("Interactive credential creation failed status: {}", new Json(notification).getValue());
        } else {
            notification = notification.withEventType("INTERACTIVE_CREDENTIAL_STATUS");
            LOGGER.info("Interactive credential creation success status: {}", new Json(notification).getValue());
        }
//        notificationSender.send(new Notification<>(notification));
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
