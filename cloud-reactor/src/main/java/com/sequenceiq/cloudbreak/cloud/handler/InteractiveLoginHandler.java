package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class InteractiveLoginHandler implements CloudPlatformEventHandler<InteractiveLoginRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveLoginHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialNotifier credentialNotifier;

    @Override
    public Class<InteractiveLoginRequest> type() {
        return InteractiveLoginRequest.class;
    }

    @Override
    public void accept(Event<InteractiveLoginRequest> interactiveLoginRequestEvent) {
        LOGGER.debug("Received event: {}", interactiveLoginRequestEvent);
        InteractiveLoginRequest request = interactiveLoginRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.getDefault(cloudContext.getPlatform());
            Map<String, String> parameters = connector.credentials().interactiveLogin(cloudContext, request.getExtendedCloudCredential(), credentialNotifier);
            InteractiveLoginResult interactiveLoginResult = new InteractiveLoginResult(request, parameters);
            request.getResult().onNext(interactiveLoginResult);
            LOGGER.debug("Interactive login request successfully processed");
        } catch (RuntimeException e) {
            request.getResult().onNext(new InteractiveLoginResult(e.getMessage(), e, request));
        }
    }

}
