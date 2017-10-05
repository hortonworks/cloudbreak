package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformVmTypesHandler implements CloudPlatformEventHandler<GetPlatformVmTypesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformVmTypesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformVmTypesRequest> type() {
        return GetPlatformVmTypesRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformVmTypesRequest> getPlatformVmTypesRequest) {
        LOGGER.info("Received event: {}", getPlatformVmTypesRequest);
        GetPlatformVmTypesRequest request = getPlatformVmTypesRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudVmTypes platformVirtualMachinesJson = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .virtualMachines(request.getCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformVmTypesResult getPlatformSecurityGroupsResult = new GetPlatformVmTypesResult(request, platformVirtualMachinesJson);
            request.getResult().onNext(getPlatformSecurityGroupsResult);
            LOGGER.info("Query platform vmtypes types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformVmTypesResult(e.getMessage(), e, request));
        }
    }
}
