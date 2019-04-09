package com.sequenceiq.cloudbreak.reactor;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.InteractiveCreationStatusV4Event;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationStatus;
import com.sequenceiq.cloudbreak.converter.events.ExtendedCloudCredentialToCredentialViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.notification.NotificationAssemblingService;

import reactor.bus.Event;

/**
 * Created by perdos on 9/23/16.
 */
@Component
public class InteractiveCredentialCreationStatusHandler implements ReactorEventHandler<InteractiveCredentialCreationStatus> {

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private ExtendedCloudCredentialToCredentialViewV4ResponseConverter converter;

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
        //TODO: remove notifiaction backward compatible
        notificationSender.send(new Notification<>(notification));
        notificationSender.send(getNotification(interactiveCredentialCreationStatus));
    }

    private Notification<?> getNotification(InteractiveCredentialCreationStatus status) {
        NotificationEventType eventType = status.isError()
                ? NotificationEventType.CREATE_FAILED
                : NotificationEventType.CREATE_IN_PROGRESS;

        InteractiveCreationStatusV4Event payload = new InteractiveCreationStatusV4Event();
        payload.setCredential(converter.convert(status.getExtendedCloudCredential()));
        payload.setMessage(status.getMessage());
        return new Notification<>(NotificationAssemblingService.cloudbreakEvent(payload, eventType, WorkspaceResource.CREDENTIAL));
    }
}
