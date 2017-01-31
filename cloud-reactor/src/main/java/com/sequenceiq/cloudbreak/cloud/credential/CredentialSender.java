package com.sequenceiq.cloudbreak.cloud.credential;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveCredentialCreationStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * Created by perdos on 11/17/16.
 */
@Service
public class CredentialSender implements CredentialNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialSender.class);

    @Inject
    private EventBus eventBus;

    @Override
    public void createCredential(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential) {
        InteractiveCredentialCreationRequest credentialCreationRequest =
                new InteractiveCredentialCreationRequest(cloudContext, extendedCloudCredential);
        LOGGER.info("Triggering event: {}", credentialCreationRequest);
        eventBus.notify(credentialCreationRequest.selector(), Event.wrap(credentialCreationRequest));
    }

    @Override
    public void sendStatusMessage(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential, boolean error, String statusMessage) {
        InteractiveCredentialCreationStatus interactiveCredentialCreationStatus =
                new InteractiveCredentialCreationStatus(error, statusMessage, cloudContext, extendedCloudCredential);
        eventBus.notify(interactiveCredentialCreationStatus.selector(), Event.wrap(interactiveCredentialCreationStatus));
    }

}
