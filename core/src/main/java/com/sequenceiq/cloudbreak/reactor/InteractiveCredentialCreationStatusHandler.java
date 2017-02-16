package com.sequenceiq.cloudbreak.reactor;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationStatus;
import com.sequenceiq.cloudbreak.converter.spi.ExtendedCloudCredentialToCredentialConverter;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

import reactor.bus.Event;

/**
 * Created by perdos on 9/23/16.
 */
@Component
public class InteractiveCredentialCreationStatusHandler implements ClusterEventHandler<InteractiveCredentialCreationStatus> {

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private ExtendedCloudCredentialToCredentialConverter extendedCloudCredentialToCredentialConverter;

    @Override
    public Class<InteractiveCredentialCreationStatus> type() {
        return InteractiveCredentialCreationStatus.class;
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationStatus> interactiveCredentialCreationFailedEvent) {
        InteractiveCredentialCreationStatus interactiveCredentialCreationStatus = interactiveCredentialCreationFailedEvent.getData();
        String message = interactiveCredentialCreationStatus.getMessage();
        Notification notification = new Notification();
        if (interactiveCredentialCreationStatus.isError()) {
            notification.setEventType("CREDENTIAL_CREATE_FAILED");
        } else {
            notification.setEventType("INTERACTIVE_CREDENTIAL_STATUS");
        }
        notification.setEventTimestamp(new Date());
        notification.setEventMessage(message);
        notification.setOwner(interactiveCredentialCreationStatus.getCloudContext().getOwner());
        notification.setAccount(interactiveCredentialCreationStatus.getExtendedCloudCredential().getAccount());
        notification.setCloud(interactiveCredentialCreationStatus.getExtendedCloudCredential().getCloudPlatform());
        notificationSender.send(notification);
    }
}
