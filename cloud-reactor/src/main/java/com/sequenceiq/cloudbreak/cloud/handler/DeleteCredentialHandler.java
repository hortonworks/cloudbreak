package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.DeleteCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.DeleteCredentialResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

import reactor.bus.Event;

@Component
public class DeleteCredentialHandler implements CloudPlatformEventHandler<DeleteCredentialRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCredentialHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<DeleteCredentialRequest> type() {
        return DeleteCredentialRequest.class;
    }

    @Override
    public void accept(Event<DeleteCredentialRequest> deleteCredentialRequestEvent) {
        LOGGER.info("Received event: {}", deleteCredentialRequestEvent);
        DeleteCredentialRequest request = deleteCredentialRequestEvent.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            CloudCredentialStatus cloudCredentialStatus = connector.credentials().delete(ac);
            DeleteCredentialResult deleteCredentialResult = new DeleteCredentialResult(request, cloudCredentialStatus);
            request.getResult().onNext(deleteCredentialResult);
            LOGGER.info("Delete credential finished");
        } catch (Exception e) {
            request.getResult().onNext(new DeleteCredentialResult(e.getMessage(), e, request));
        }
    }

}
