package com.sequenceiq.cloudbreak.service.events;

import java.util.Collection;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.notification.WebSocketNotificationService;

@Service
public class CloudbreakEventHandler implements Consumer<Event<CloudbreakCompositeEvent>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakEventHandler.class);

    @Inject
    private LegacyDefaultStructuredEventClient legacyStructuredEventAsyncNotifier;

    @Inject
    private WebSocketNotificationService webSocketNotificationService;

    @Override
    public void accept(Event<CloudbreakCompositeEvent> cloudbreakEvent) {
        CloudbreakCompositeEvent cloudbreakCompositeEvent = cloudbreakEvent.getData();
        StructuredNotificationEvent structuredNotificationEvent = cloudbreakCompositeEvent.getStructuredNotificationEvent();
        legacyStructuredEventAsyncNotifier.sendStructuredEvent(structuredNotificationEvent);
        sendNotification(cloudbreakCompositeEvent);
    }

    private void sendNotification(CloudbreakCompositeEvent cloudbreakCompositeEvent) {
        String owner = cloudbreakCompositeEvent.getOwner();
        StackV4Response stackResponse = cloudbreakCompositeEvent.getStackResponse();
        if (stackResponse != null) {
            try {
                String notificationType = getNotificationType(cloudbreakCompositeEvent.getStructuredNotificationEvent());
                Collection<String> resourceEventMessageArgs = cloudbreakCompositeEvent.getResourceEventMessageArgs();
                webSocketNotificationService.send(cloudbreakCompositeEvent.getResourceEvent(), resourceEventMessageArgs, stackResponse, owner, notificationType);
            } catch (Exception e) {
                String msg = String.format("Failed to send notification from structured event, stack('%s')", stackResponse.getId());
                LOGGER.warn(msg, e);
            }
        }
    }

    private String getNotificationType(StructuredNotificationEvent structuredNotificationEvent) {
        if (structuredNotificationEvent != null && structuredNotificationEvent.getNotificationDetails() != null) {
            return structuredNotificationEvent.getNotificationDetails().getNotificationType();
        }
        return null;
    }
}
