package com.sequenceiq.cloudbreak.cloud.handler;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDefaultPlatformDatabaseCapabilityRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDefaultPlatformDatabaseCapabilityResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.DefaultPlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetDefaultPlatformDatabaseCapabilitiesHandler implements CloudPlatformEventHandler<GetDefaultPlatformDatabaseCapabilityRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDefaultPlatformDatabaseCapabilitiesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetDefaultPlatformDatabaseCapabilityRequest> type() {
        return GetDefaultPlatformDatabaseCapabilityRequest.class;
    }

    @Override
    public void accept(Event<GetDefaultPlatformDatabaseCapabilityRequest> getDefaultPlatformDatabaseCapabilityRequestEvent) {
        LOGGER.debug("Received event: {}", getDefaultPlatformDatabaseCapabilityRequestEvent);
        GetDefaultPlatformDatabaseCapabilityRequest request = getDefaultPlatformDatabaseCapabilityRequestEvent.getData();
        try {
            DefaultPlatformDatabaseCapabilities defaultPlatformDatabaseCapabilities = new DefaultPlatformDatabaseCapabilities();
            CloudConnector cloudConnector = cloudPlatformConnectors.getDefault(platform(request.getPlatform()));
            if (cloudConnector != null) {
                defaultPlatformDatabaseCapabilities = cloudConnector
                        .platformResources()
                        .defaultDatabaseCapabilities();
            }
            request.getResult().onNext(new GetDefaultPlatformDatabaseCapabilityResult(defaultPlatformDatabaseCapabilities));
            LOGGER.debug("Query default platform database capabilities finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetDefaultPlatformDatabaseCapabilityResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
