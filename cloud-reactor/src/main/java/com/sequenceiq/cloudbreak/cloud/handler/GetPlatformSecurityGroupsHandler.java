package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSecurityGroupsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSecurityGroupsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformSecurityGroupsHandler implements CloudPlatformEventHandler<GetPlatformSecurityGroupsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformSecurityGroupsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformSecurityGroupsRequest> type() {
        return GetPlatformSecurityGroupsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformSecurityGroupsRequest> getPlatformSecurityGroupsRequest) {
        LOGGER.info("Received event: {}", getPlatformSecurityGroupsRequest);
        GetPlatformSecurityGroupsRequest request = getPlatformSecurityGroupsRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudSecurityGroups securityGroups = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .securityGroups(request.getCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformSecurityGroupsResult getPlatformSecurityGroupsResult = new GetPlatformSecurityGroupsResult(request, securityGroups);
            request.getResult().onNext(getPlatformSecurityGroupsResult);
            LOGGER.info("Query platform networks types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformSecurityGroupsResult(e.getMessage(), e, request));
        }
    }
}
