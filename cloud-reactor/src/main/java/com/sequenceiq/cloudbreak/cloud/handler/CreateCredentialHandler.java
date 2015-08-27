package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CreateCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

import reactor.bus.Event;

@Component
public class CreateCredentialHandler implements CloudPlatformEventHandler<CreateCredentialRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCredentialHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CreateCredentialRequest> type() {
        return CreateCredentialRequest.class;
    }


    @Override
    public void accept(Event<CreateCredentialRequest> createCredentialRequestEvent) {
        LOGGER.info("Received event: {}", createCredentialRequestEvent);
        CreateCredentialRequest createCredentialRequest = createCredentialRequestEvent.getData();
        try {
            String platform = createCredentialRequest.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authenticate(createCredentialRequest.getCloudContext(), createCredentialRequest.getCloudCredential());
            CloudCredentialStatus cloudCredentialStatus = connector.credentials().create(ac);
            CreateCredentialResult createCredentialResult = new CreateCredentialResult(createCredentialRequest, cloudCredentialStatus);
            createCredentialRequest.getResult().onNext(createCredentialResult);
            LOGGER.info("Create credential successfully finished");
        } catch (Exception e) {
            createCredentialRequest.getResult().onNext(new CreateCredentialResult(e.getMessage(), e, createCredentialRequest));
        }
    }

}
