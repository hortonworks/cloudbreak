package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.PreProvisionCheckRequest;
import com.sequenceiq.cloudbreak.cloud.event.PreProvisionCheckResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
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
        try {
            String platform = request.getStackContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(request.getStackContext(), request.getCloudCredential());
            String message = connector.provision().preCheck(authenticatedContext, request.getCloudStack());
            PreProvisionCheckResult result = new PreProvisionCheckResult(message);
            request.getResult().onNext(result);
        } catch (Exception e) {
            LOGGER.error("Failed to handle PreProvisionCheckRequest.", e);
            request.getResult().onNext(new PreProvisionCheckResult(e.getMessage()));
        }
        LOGGER.info("PreProvisionCheckHandler finished");
    }
}
