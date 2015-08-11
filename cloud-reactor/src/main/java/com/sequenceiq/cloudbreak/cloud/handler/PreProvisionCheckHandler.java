package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.PreProvisionCheckRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PreProvisionCheckResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class PreProvisionCheckHandler implements CloudPlatformEventHandler<PreProvisionCheckRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreProvisionCheckHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<PreProvisionCheckRequest> type() {
        return PreProvisionCheckRequest.class;
    }

    @Override
    public void accept(Event<PreProvisionCheckRequest> event) {
        LOGGER.info("Received event: {}", event);
        PreProvisionCheckRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            String platform = cloudContext.getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(cloudContext, request.getCloudCredential());
            String message = connector.setup().preCheck(authenticatedContext, request.getCloudStack());
            request.getResult().onNext(new PreProvisionCheckResult(message, null, request));
            LOGGER.info("Pre-provision check finished for {}", cloudContext);
        } catch (Exception e) {
            request.getResult().onNext(new PreProvisionCheckResult(e.getMessage(), e, request));
        }
    }
}
