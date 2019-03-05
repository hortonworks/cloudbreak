package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CreateCredentialHandler implements CloudPlatformEventHandler<CreateCredentialRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCredentialHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<CreateCredentialRequest> type() {
        return CreateCredentialRequest.class;
    }

    @Override
    public void accept(Event<CreateCredentialRequest> credentialRequestEvent) {
        LOGGER.debug("Received event: {}", credentialRequestEvent);
        CreateCredentialRequest request = credentialRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            CloudCredentialStatus credentialStatus = connector.credentials().create(ac);
            if (CredentialStatus.FAILED == credentialStatus.getStatus()) {
                if (credentialStatus.getException() != null) {
                    throw new CloudConnectorException(credentialStatus.getException());
                }
                throw new CloudConnectorException(credentialStatus.getStatusReason());
            }
            CreateCredentialResult result = new CreateCredentialResult(request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(credentialRequestEvent.getHeaders(), result));
            LOGGER.debug("Creating credential successfully finished for {}", cloudContext);
        } catch (RuntimeException e) {
            CreateCredentialResult failure = new CreateCredentialResult(e, request);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(credentialRequestEvent.getHeaders(), failure));
        }
    }
}
