package com.sequenceiq.cloudbreak.reactor;

import java.util.Date;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.mapper.DuplicatedKeyValueExceptionMapper;
import com.sequenceiq.cloudbreak.converter.spi.ExtendedCloudCredentialToCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.user.UserService;

import reactor.bus.Event;

@Component
public class InteractiveCredentialCreationHandler implements ReactorEventHandler<InteractiveCredentialCreationRequest> {

    @Inject
    private CredentialService credentialService;

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
        Credential credential = extendedCloudCredentialToCredentialConverter.convert(extendedCloudCredential);
        User user = userService.getOrCreate(extendedCloudCredential.getCloudbreakUser());
        try {
            credentialService.createWithRetry(credential, extendedCloudCredential.getWorkspaceId(), user);
        } catch (DuplicateKeyValueException e) {
            sendErrorNotification(extendedCloudCredential, DuplicatedKeyValueExceptionMapper.errorMessage(e));
        } catch (BadRequestException e) {
            sendErrorNotification(extendedCloudCredential, e.getMessage());
        }
    }

    public void sendErrorNotification(ExtendedCloudCredential extendedCloudCredential, String errorMessage) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType("CREDENTIAL_CREATE_FAILED");
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(errorMessage);
        notification.setUserId(extendedCloudCredential.getUserId());
        notification.setCloud(extendedCloudCredential.getCloudPlatform());
        notificationSender.send(new Notification<>(notification));
    }
}
