package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.ProvisionSetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.ProvisionSetupResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class ProvisionSetupHandler implements CloudPlatformEventHandler<ProvisionSetupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionSetupHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<ProvisionSetupRequest> type() {
        return ProvisionSetupRequest.class;
    }

    @Override
    public void accept(Event<ProvisionSetupRequest> event) {
        LOGGER.info("Received event: {}", event);
        ProvisionSetupRequest request = event.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(request.getCloudContext(), request.getCloudCredential());
            Map<String, Object> provisionSetupResult = connector.provision().setup(authenticatedContext, request.getCloudStack());
            request.getResult().onNext(new ProvisionSetupResult(provisionSetupResult));
        } catch (Exception e) {
            LOGGER.error("Failed to handle ProvisionSetupRequest.", e);
            request.getResult().onNext(new ProvisionSetupResult(e));
        }
        LOGGER.info("ProvisionSetupHandler finished");
    }
}
