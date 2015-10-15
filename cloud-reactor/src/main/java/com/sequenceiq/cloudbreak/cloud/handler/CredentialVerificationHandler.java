package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

import reactor.bus.Event;

@Component
public class CredentialVerificationHandler implements CloudPlatformEventHandler<CredentialVerificationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialVerificationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CredentialVerificationRequest> type() {
        return CredentialVerificationRequest.class;
    }

    @Override
    public void accept(Event<CredentialVerificationRequest> createCredentialRequestEvent) {
        LOGGER.info("Received event: {}", createCredentialRequestEvent);
        CredentialVerificationRequest request = createCredentialRequestEvent.getData();
        try {
            CloudConnector connector = cloudPlatformConnectors.getDefault(request.getCloudContext().getPlatform());
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            CloudCredentialStatus cloudCredentialStatus = connector.credentials().verify(ac);
            CredentialVerificationResult credentialVerificationResult = new CredentialVerificationResult(request, cloudCredentialStatus);
            request.getResult().onNext(credentialVerificationResult);
            LOGGER.info("Credential verification successfully finished");
        } catch (Exception e) {
            request.getResult().onNext(new CredentialVerificationResult(e.getMessage(), e, request));
        }
    }

}
