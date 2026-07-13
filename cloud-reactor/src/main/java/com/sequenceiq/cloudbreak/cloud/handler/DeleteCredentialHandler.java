package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialDeletionResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class DeleteCredentialHandler implements CloudPlatformEventHandler<CredentialDeletionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCredentialHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CredentialDeletionRequest> type() {
        return CredentialDeletionRequest.class;
    }

    @Override
    public void accept(Event<CredentialDeletionRequest> event) {
        LOGGER.debug("Received event: {}", event);
        CredentialDeletionRequest request = event.getData();
        try {
            CloudConnector connector = cloudPlatformConnectors.getDefault(request.getCloudContext().getPlatform());
            AuthenticatedContext ac = new AuthenticatedContext(request.getCloudContext(), request.getCloudCredential());
            CloudCredentialStatus credentialStatus = connector.credentials().delete(ac);
            CredentialDeletionResult result;
            if (CredentialStatus.FAILED == credentialStatus.getStatus()) {
                Exception cause = credentialStatus.getException();
                String reason = cause != null ? cause.getMessage() : credentialStatus.getStatusReason();
                LOGGER.warn("Credential deletion failed: {}", reason);
                result = new CredentialDeletionResult(reason, cause, request.getResourceId());
            } else {
                result = new CredentialDeletionResult(request.getResourceId(), credentialStatus);
            }
            request.getResult().onNext(result);
            LOGGER.debug("Credential deletion finished for {}", request.getCloudContext());
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to delete credential", e);
            request.getResult().onNext(new CredentialDeletionResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
