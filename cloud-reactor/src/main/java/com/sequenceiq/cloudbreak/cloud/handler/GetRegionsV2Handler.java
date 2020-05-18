package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequestV2;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResultV2;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetRegionsV2Handler implements CloudPlatformEventHandler<GetPlatformRegionsRequestV2> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetRegionsV2Handler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformRegionsRequestV2> type() {
        return GetPlatformRegionsRequestV2.class;
    }

    @Override
    public void accept(Event<GetPlatformRegionsRequestV2> getRegionsRequestEvent) {
        LOGGER.debug("Received event: {}", getRegionsRequestEvent);
        GetPlatformRegionsRequestV2 request = getRegionsRequestEvent.getData();
        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            Region region = Region.region(request.getRegion());
            CloudRegions cloudRegions = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .regions(request.getCloudCredential(), region, request.getFilters(), request.isAvailabilityZonesNeeded());
            GetPlatformRegionsResultV2 getPlatformRegionsResultV2 = new GetPlatformRegionsResultV2(request.getResourceId(), cloudRegions);
            request.getResult().onNext(getPlatformRegionsResultV2);
            LOGGER.debug("Query platform regions types finished.");
        } catch (Exception e) {
            LOGGER.warn("Could not get regions from the cloud provider due to:", e);
            request.getResult().onNext(new GetPlatformRegionsResultV2(e.getMessage(), e, request.getResourceId()));
        }
    }
}
