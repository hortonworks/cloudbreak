package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetCdpPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetCdpPlatformRegionsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetCdpSupportedRegionsHandler implements CloudPlatformEventHandler<GetCdpPlatformRegionsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetCdpSupportedRegionsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetCdpPlatformRegionsRequest> type() {
        return GetCdpPlatformRegionsRequest.class;
    }

    @Override
    public void accept(Event<GetCdpPlatformRegionsRequest> getRegionsRequestEvent) {
        LOGGER.debug("Received event: {}", getRegionsRequestEvent);
        GetCdpPlatformRegionsRequest request = getRegionsRequestEvent.getData();
        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    request.getCloudContext().getPlatform().getValue(),
                    request.getCloudContext().getVariant().getValue());
            CloudRegions cloudRegions = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .cdpEnabledRegions();
            GetCdpPlatformRegionsResult getPlatformRegionsResultV2 = new GetCdpPlatformRegionsResult(cloudRegions);
            request.getResult().onNext(getPlatformRegionsResultV2);
            LOGGER.debug("Query cdp platform regions types finished.");
        } catch (Exception e) {
            LOGGER.warn("Could not get cdp regions from the cloud provider due to:", e);
            request.getResult().onNext(new GetCdpPlatformRegionsResult(e.getMessage(), e));
        }
    }
}
