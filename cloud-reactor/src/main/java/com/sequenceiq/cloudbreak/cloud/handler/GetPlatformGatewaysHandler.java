package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudGatewaysResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformGatewaysHandler implements CloudPlatformEventHandler<GetPlatformCloudGatewaysRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformGatewaysHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformCloudGatewaysRequest> type() {
        return GetPlatformCloudGatewaysRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformCloudGatewaysRequest> getPlatformCloudGatewaysRequest) {
        LOGGER.debug("Received event: {}", getPlatformCloudGatewaysRequest);
        GetPlatformCloudGatewaysRequest request = getPlatformCloudGatewaysRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudGateWays cloudGateWays = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().gateways(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformCloudGatewaysResult getPlatformCloudGatewaysResult = new GetPlatformCloudGatewaysResult(request.getResourceId(), cloudGateWays);
            request.getResult().onNext(getPlatformCloudGatewaysResult);
            LOGGER.debug("Query platform gateway types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformCloudGatewaysResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
