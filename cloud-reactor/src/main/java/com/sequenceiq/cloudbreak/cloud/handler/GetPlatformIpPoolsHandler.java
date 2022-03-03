package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudIpPoolsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformIpPoolsHandler implements CloudPlatformEventHandler<GetPlatformCloudIpPoolsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformIpPoolsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformCloudIpPoolsRequest> type() {
        return GetPlatformCloudIpPoolsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformCloudIpPoolsRequest> getPlatformIpPoolsRequest) {
        LOGGER.debug("Received event: {}", getPlatformIpPoolsRequest);
        GetPlatformCloudIpPoolsRequest request = getPlatformIpPoolsRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudIpPools cloudIpPools = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().publicIpPool(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformCloudIpPoolsResult getPlatformIpPoolsResult = new GetPlatformCloudIpPoolsResult(request.getResourceId(), cloudIpPools);
            request.getResult().onNext(getPlatformIpPoolsResult);
            LOGGER.debug("Query platform ip pool types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformCloudIpPoolsResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
