package com.sequenceiq.environment.credential.reactor.handler;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CREDENTIAL_AZURE_INTERACTIVE_CREATED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CREDENTIAL_AZURE_INTERACTIVE_FAILED;

import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.reactor.handler.InteractiveCredentialCreationStatusHandler.InteractiveCredentialNotification;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.notification.NotificationService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

import reactor.bus.Event;

@Component
public class InteractiveCredentialCreationHandler implements EventHandler<InteractiveCredentialCreationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveCredentialCreationHandler.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private CredentialToCredentialV1ResponseConverter extendedCloudCredentialToCredentialConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InteractiveCredentialCreationRequest.class);
    }

    @Override
    public void accept(Event<InteractiveCredentialCreationRequest> interactiveCredentialCreationRequestEvent) {
        InteractiveCredentialCreationRequest interactiveCredentialCreationRequest = interactiveCredentialCreationRequestEvent.getData();

        ExtendedCloudCredential extendedCloudCredential = interactiveCredentialCreationRequest.getExtendedCloudCredential();
        Credential credential = extendedCloudCredentialToCredentialConverter.convert(extendedCloudCredential);
        try {
            credentialService.initCodeGrantFlow(credential.getAccountId(), credential, credential.getCreator());
            sendNotification(credential, extendedCloudCredential.getName(), CREDENTIAL_AZURE_INTERACTIVE_CREATED);
        } catch (BadRequestException e) {
            sendNotification(credential, e.getMessage(), CREDENTIAL_AZURE_INTERACTIVE_FAILED);
        }
    }

    private void sendNotification(Credential credential, String message, ResourceEvent eventType) {
        InteractiveCredentialNotification notification = new InteractiveCredentialNotification()
                .withEventTimestamp(new Date().getTime())
                .withUserId(credential.getCreator())
                .withCloud(credential.getCloudPlatform())
                .withEventMessage(message)
                .withEventType(eventType.name());
        notificationService.send(eventType, notification, credential.getCreator());
        LOGGER.info("Interactive credential creation init code grant flow notification: {}", new Json(notification).getValue());
    }
}
