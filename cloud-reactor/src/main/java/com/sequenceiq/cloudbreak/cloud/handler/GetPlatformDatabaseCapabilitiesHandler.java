package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseCapabilityRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseCapabilityResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetPlatformDatabaseCapabilitiesHandler implements CloudPlatformEventHandler<GetPlatformDatabaseCapabilityRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformDatabaseCapabilitiesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformDatabaseCapabilityRequest> type() {
        return GetPlatformDatabaseCapabilityRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformDatabaseCapabilityRequest> getDatabaseCapabilityRequest) {
        LOGGER.debug("Received event: {}", getDatabaseCapabilityRequest);
        GetPlatformDatabaseCapabilityRequest request = getDatabaseCapabilityRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            PlatformDatabaseCapabilities databaseCapabilities = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .databaseCapabilities(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformDatabaseCapabilityResult getPlatformDatabaseCapabilityResult =
                    new GetPlatformDatabaseCapabilityResult(request.getResourceId(), databaseCapabilities);
            request.getResult().onNext(getPlatformDatabaseCapabilityResult);
            LOGGER.debug("Query platform database capabilities finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformDatabaseCapabilityResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
