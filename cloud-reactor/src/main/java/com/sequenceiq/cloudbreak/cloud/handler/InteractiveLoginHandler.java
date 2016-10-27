package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InteractiveLoginResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class InteractiveLoginHandler implements CloudPlatformEventHandler<InteractiveLoginRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveLoginHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<InteractiveLoginRequest> type() {
        return InteractiveLoginRequest.class;
    }

    @Override
    public void accept(Event<InteractiveLoginRequest> interactiveLoginRequestEvent) {
        LOGGER.info("Received event: {}", interactiveLoginRequestEvent);
        InteractiveLoginRequest request = interactiveLoginRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.getDefault(cloudContext.getPlatform());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            Map<String, String> parameters = connector.credentials().interactiveLogin(auth, request.getExtendedCloudCredential());
            InteractiveLoginResult interactiveLoginResult = new InteractiveLoginResult(request, parameters);
            request.getResult().onNext(interactiveLoginResult);
            LOGGER.info("Interactive login request successfully processed");
        } catch (Exception e) {
            request.getResult().onNext(new InteractiveLoginResult(e.getMessage(), e, request));
        }
    }

}
