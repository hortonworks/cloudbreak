package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformNetworksResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformNetworksHandler implements CloudPlatformEventHandler<GetPlatformNetworksRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformNetworksHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformNetworksRequest> type() {
        return GetPlatformNetworksRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformNetworksRequest> getPlatformNetworksRequest) {
        LOGGER.debug("Received event: {}", getPlatformNetworksRequest);
        GetPlatformNetworksRequest request = getPlatformNetworksRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudNetworks networks = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .networks(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformNetworksResult getPlatformNetworksResult = new GetPlatformNetworksResult(request.getResourceId(), networks);
            request.getResult().onNext(getPlatformNetworksResult);
            LOGGER.debug("Platform networks result : {}", getPlatformNetworksResult);
            LOGGER.debug("Query platform networks types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformNetworksResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
