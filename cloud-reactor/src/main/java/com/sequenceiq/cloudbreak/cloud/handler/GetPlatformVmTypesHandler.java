package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVmTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.common.api.type.CdpResourceType;

import reactor.bus.Event;

@Component
public class GetPlatformVmTypesHandler implements CloudPlatformEventHandler<GetPlatformVmTypesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformVmTypesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public Class<GetPlatformVmTypesRequest> type() {
        return GetPlatformVmTypesRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformVmTypesRequest> getPlatformVmTypesRequest) {
        LOGGER.debug("Received event: {}", getPlatformVmTypesRequest);
        GetPlatformVmTypesRequest request = getPlatformVmTypesRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            PlatformResources platformResources = cloudPlatformConnectors.get(cloudPlatformVariant).platformResources();
            CloudVmTypes platformVirtualMachinesJson;
            if (CdpResourceType.DATAHUB.equals(request.getCdpResourceType())) {
                if (request.hasEnableDistroxInstanceTypesEntitlement()) {
                    platformVirtualMachinesJson = platformResources
                            .virtualMachines(
                                    request.getCloudCredential(),
                                    Region.region(request.getRegion()),
                                    request.getFilters(),
                                    entitlementService.getEntitlements(request.getCloudCredential().getAccountId()));
                } else {
                    platformVirtualMachinesJson = platformResources
                            .virtualMachinesForDistroX(request.getCloudCredential(), Region.region(request.getRegion()), request.getFilters(),
                                    entitlementService.getEntitlements(request.getCloudCredential().getAccountId()));
                }
            } else {
                platformVirtualMachinesJson = platformResources
                        .virtualMachines(request.getCloudCredential(), Region.region(request.getRegion()), request.getFilters(),
                                entitlementService.getEntitlements(request.getCloudCredential().getAccountId()));
            }
            GetPlatformVmTypesResult getPlatformSecurityGroupsResult = new GetPlatformVmTypesResult(request.getResourceId(), platformVirtualMachinesJson);
            request.getResult().onNext(getPlatformSecurityGroupsResult);
            LOGGER.debug("Query platform vmtypes types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformVmTypesResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
