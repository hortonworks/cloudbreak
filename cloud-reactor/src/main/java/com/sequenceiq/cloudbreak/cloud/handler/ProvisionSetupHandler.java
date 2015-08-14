package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class ProvisionSetupHandler implements CloudPlatformEventHandler<SetupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionSetupHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<SetupRequest> type() {
        return SetupRequest.class;
    }

    @Override
    public void accept(Event<SetupRequest> event) {
        LOGGER.info("Received event: {}", event);
        SetupRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            String platform = cloudContext.getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(cloudContext, request.getCloudCredential());
            Map<String, Object> provisionSetupResult = connector.setup().execute(authenticatedContext, request.getCloudStack());
            request.getResult().onNext(new SetupResult(request, provisionSetupResult));
            LOGGER.info("Provision setup finished for {}", cloudContext);
        } catch (Exception e) {
            request.getResult().onNext(new SetupResult(e, request));
        }
    }
}
