package com.sequenceiq.cloudbreak.reactor;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.controller.mapper.DuplicatedKeyValueExceptionMapper;
import com.sequenceiq.cloudbreak.converter.spi.ExtendedCloudCredentialToCredentialConverter;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;

@Component
public class InteractiveCredentialCreationHandler implements EventHandler<InteractiveCredentialCreationRequest> {

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private ExtendedCloudCredentialToCredentialConverter extendedCloudCredentialToCredentialConverter;

    @Inject
    private UserService userService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InteractiveCredentialCreationRequest.class);
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationRequest> interactiveCredentialCreationRequestEvent) {
        InteractiveCredentialCreationRequest interactiveCredentialCreationRequest = interactiveCredentialCreationRequestEvent.getData();

        ExtendedCloudCredential extendedCloudCredential = interactiveCredentialCreationRequest.getExtendedCloudCredential();
//        Credential credential = extendedCloudCredentialToCredentialConverter.convert(extendedCloudCredential);
//        User user = userService.getOrCreate(extendedCloudCredential.getCloudbreakUser());
        try {
            //TODO this logic needs to be in the Environment MS
//            credentialService.initCodeGrantFlow(extendedCloudCredential.getWorkspaceId(), credential, user);
            sendNotification(extendedCloudCredential, extendedCloudCredential.getName(), "CREDENTIAL_APP_CREATED");
        } catch (DuplicateKeyValueException e) {
            sendNotification(extendedCloudCredential, DuplicatedKeyValueExceptionMapper.errorMessage(e), "CREDENTIAL_CREATE_FAILED");
        } catch (BadRequestException e) {
            sendNotification(extendedCloudCredential, e.getMessage(), "CREDENTIAL_CREATE_FAILED");
        }
    }

    private void sendNotification(ExtendedCloudCredential extendedCloudCredential, String message, String eventType) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setEventType(eventType);
        notification.setEventTimestamp(new Date().getTime());
        notification.setUserId(extendedCloudCredential.getUserId());
        notification.setEventMessage(message);
        notification.setCloud(extendedCloudCredential.getCloudPlatform());
        notificationSender.send(new Notification<>(notification));
    }
}
