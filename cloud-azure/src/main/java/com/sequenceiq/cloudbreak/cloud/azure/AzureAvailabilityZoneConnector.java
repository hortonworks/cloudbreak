package com.sequenceiq.cloudbreak.cloud.azure;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class AzureAvailabilityZoneConnector implements AvailabilityZoneConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAvailabilityZoneConnector.class);

    @Inject
    private AzurePlatformResources azurePlatformResources;

    public Set<String> getAvailabilityZones(ExtendedCloudCredential cloudCredential, Set<String> environmentZones,
            String instanceType, Region region) {
        CloudVmTypes vmTypesV2 = azurePlatformResources.virtualMachines(cloudCredential, region, null);
        if (vmTypesV2.getCloudVmResponses() != null && vmTypesV2.getCloudVmResponses().get(region.getValue()) != null) {
            List<String> availabilityZonesForInstanceType = vmTypesV2.getCloudVmResponses().get(region.getValue()).stream()
                    .filter(vmType -> vmType.value().equals(instanceType) && vmType.getMetaData() != null
                            && !isEmpty(vmType.getMetaData().getAvailabilityZones()))
                    .map(vmType -> vmType.getMetaData().getAvailabilityZones())
                    .findFirst().orElse(List.of());
            LOGGER.debug("Availability Zones for Instance Type {} are {} ", instanceType,
                    availabilityZonesForInstanceType);
            return emptyIfNull(environmentZones).stream().filter(availabilityZonesForInstanceType::contains).collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }
}
