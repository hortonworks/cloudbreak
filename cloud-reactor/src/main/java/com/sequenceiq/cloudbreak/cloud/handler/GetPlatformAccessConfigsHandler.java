package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudAccessConfigsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformCloudAccessConfigsResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudUnauthorizedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformAccessConfigsHandler implements CloudPlatformEventHandler<GetPlatformCloudAccessConfigsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformAccessConfigsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformCloudAccessConfigsRequest> type() {
        return GetPlatformCloudAccessConfigsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformCloudAccessConfigsRequest> getPlatformCloudAccessConfigsRequest) {
        LOGGER.debug("Received event: {}", getPlatformCloudAccessConfigsRequest);
        GetPlatformCloudAccessConfigsRequest request = getPlatformCloudAccessConfigsRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudAccessConfigs cloudAccessConfigs = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().accessConfigs(request.getCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformCloudAccessConfigsResult getPlatformCloudAccessConfigsResult =
                    new GetPlatformCloudAccessConfigsResult(request.getResourceId(), cloudAccessConfigs);
            request.getResult().onNext(getPlatformCloudAccessConfigsResult);
            LOGGER.debug("Query platform access configs finished. {} access config(s) has returned.",
                    getResultAccessConfigQuantityIfAvailable(getPlatformCloudAccessConfigsResult));
        } catch (CloudUnauthorizedException e) {
            request.getResult().onNext(new GetPlatformCloudAccessConfigsResult(EventStatus.PERMANENTLY_FAILED, e.getMessage(), e, request.getResourceId()));
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformCloudAccessConfigsResult(e.getMessage(), e, request.getResourceId()));
        }
    }

    private int getResultAccessConfigQuantityIfAvailable(GetPlatformCloudAccessConfigsResult result) {
        CloudAccessConfigs cloudAccessConfigs = result.getCloudAccessConfigs();
        return cloudAccessConfigs != null && cloudAccessConfigs.getCloudAccessConfigs() != null ? cloudAccessConfigs.getCloudAccessConfigs().size() : 0;
    }

}
