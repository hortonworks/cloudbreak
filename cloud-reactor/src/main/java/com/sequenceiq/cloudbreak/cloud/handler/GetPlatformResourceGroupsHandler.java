package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformResourceGroupsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformResourceGroupsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;

import reactor.bus.Event;

@Component
public class GetPlatformResourceGroupsHandler implements CloudPlatformEventHandler<GetPlatformResourceGroupsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformResourceGroupsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformResourceGroupsRequest> type() {
        return GetPlatformResourceGroupsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformResourceGroupsRequest> event) {
        LOGGER.debug("Received event: {}", event);
        GetPlatformResourceGroupsRequest request = event.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudResourceGroups resourceGroups = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().resourceGroups(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformResourceGroupsResult result = new GetPlatformResourceGroupsResult(request.getResourceId(), resourceGroups);
            request.getResult().onNext(result);
            LOGGER.debug("Query platform Resource groups finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetPlatformResourceGroupsResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
