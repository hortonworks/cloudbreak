package com.sequenceiq.environment.credential.reactor.handler;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CREDENTIAL_AZURE_INTERACTIVE_CREATED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CREDENTIAL_AZURE_INTERACTIVE_FAILED;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.notification.NotificationService;

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
            LOGGER.debug("Azure init code grant flow for account id {} creator {} credential name {}",
                    credential.getAccountId(), credential.getCreator(), credential.getName());
            credentialService.initCodeGrantFlow(credential.getAccountId(), credential, credential.getCreator());
            CredentialResponse payload = extendedCloudCredentialToCredentialConverter.convert(credential);
            LOGGER.debug("Sending notification that the interactive credential successfully created account id {} creator {} credential name {}",
                    credential.getAccountId(), credential.getCreator(), credential.getName());
            notificationService.send(CREDENTIAL_AZURE_INTERACTIVE_CREATED, payload, credential.getCreator());
            LOGGER.info("Azure interactive credential ({}) succesfully created", credential.getName());
        } catch (BadRequestException e) {
            LOGGER.debug("Sending notification that the interactive credential failed to create account id {} creator {} credential name {}",
                    credential.getAccountId(), credential.getCreator(), credential.getName());
            notificationService.send(CREDENTIAL_AZURE_INTERACTIVE_FAILED, credential.getCreator());
            LOGGER.info("Failed to create Azure interactive credential with name \"{}\"", credential.getName());
        }
    }

}
