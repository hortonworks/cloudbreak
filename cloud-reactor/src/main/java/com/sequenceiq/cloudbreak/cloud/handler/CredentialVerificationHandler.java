package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

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
        LOGGER.debug("Received event: {}", createCredentialRequestEvent);
        CredentialVerificationRequest request = createCredentialRequestEvent.getData();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.getDefault(request.getCloudContext().getPlatform());
            AuthenticatedContext ac;
            CloudCredentialStatus cloudCredentialStatus;
            try {
                ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
                cloudCredentialStatus = connector.credentials().verify(ac);
            } catch (CredentialVerificationException e) {
                String errorMessage = e.getMessage();
                LOGGER.info(errorMessage, e);
                cloudCredentialStatus = new CloudCredentialStatus(request.getCloudCredential(), CredentialStatus.FAILED, e, errorMessage);
            } catch (RuntimeException e) {
                String errorMessage = String.format("Could not verify credential [credential: '%s'], detailed message: %s",
                        request.getCloudContext().getName(), e.getMessage());
                LOGGER.warn(errorMessage, e);
                cloudCredentialStatus = new CloudCredentialStatus(request.getCloudCredential(), CredentialStatus.FAILED, e, errorMessage);
            }
            CredentialVerificationResult credentialVerificationResult = new CredentialVerificationResult(request, cloudCredentialStatus);
            request.getResult().onNext(credentialVerificationResult);
            LOGGER.debug("Credential verification successfully finished");
        } catch (RuntimeException e) {
            request.getResult().onNext(new CredentialVerificationResult(e.getMessage(), e, request));
        }
    }

}
