package com.sequenceiq.cloudbreak.cloud.handler;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseVmTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformDatabaseVmTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetPlatformDatabaseVmTypesHandler implements CloudPlatformEventHandler<GetPlatformDatabaseVmTypesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDefaultPlatformDatabaseCapabilitiesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformDatabaseVmTypesRequest> type() {
        return GetPlatformDatabaseVmTypesRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformDatabaseVmTypesRequest> getDefaultPlatformDatabaseCapabilityRequestEvent) {
        LOGGER.debug("Received event: {}", getDefaultPlatformDatabaseCapabilityRequestEvent);
        GetPlatformDatabaseVmTypesRequest request = getDefaultPlatformDatabaseCapabilityRequestEvent.getData();
        try {
            CloudDatabaseVmTypes cloudDatabaseVmTypes = new CloudDatabaseVmTypes();
            CloudConnector cloudConnector = cloudPlatformConnectors.getDefault(platform(request.getExtendedCloudCredential().getCloudPlatform()));
            if (cloudConnector != null) {
                cloudDatabaseVmTypes = cloudConnector
                        .platformResources()
                        .databaseVirtualMachines(request.getExtendedCloudCredential(), region(request.getRegion()), request.getFilters());
            }
            request.getResult().onNext(new GetPlatformDatabaseVmTypesResult(request.getResourceId(), cloudDatabaseVmTypes));
            LOGGER.debug("Query default platform database capabilities finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformDatabaseVmTypesResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
