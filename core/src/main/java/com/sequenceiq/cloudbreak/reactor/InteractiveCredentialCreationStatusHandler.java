package com.sequenceiq.cloudbreak.reactor;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationStatus;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;

/**
 * Created by perdos on 9/23/16.
 */
@Component
public class InteractiveCredentialCreationStatusHandler implements ReactorEventHandler<InteractiveCredentialCreationStatus> {

    @Inject
    private NotificationSender notificationSender;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InteractiveCredentialCreationStatus.class);
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationStatus> interactiveCredentialCreationStatusEvent) {
        InteractiveCredentialCreationStatus interactiveCredentialCreationStatus = interactiveCredentialCreationStatusEvent.getData();
        String message = interactiveCredentialCreationStatus.getMessage();
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        if (interactiveCredentialCreationStatus.isError()) {
            notification.setEventType("CREDENTIAL_CREATE_FAILED");
        } else {
            notification.setEventType("INTERACTIVE_CREDENTIAL_STATUS");
        }
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(message);
        notification.setUserId(interactiveCredentialCreationStatus.getCloudContext().getUserId());
        notification.setCloud(interactiveCredentialCreationStatus.getExtendedCloudCredential().getCloudPlatform());
        notificationSender.send(new Notification<>(notification));
    }
}
