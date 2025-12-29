package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Component
public class GcpAvailabilityZoneConnector implements AvailabilityZoneConnector, CloudConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpAvailabilityZoneConnector.class);

    @Inject
    private GcpPlatformResources gcpPlatformResources;

    @Override
    public Set<String> getAvailabilityZones(ExtendedCloudCredential cloudCredential, Set<String> environmentZones,
            String instanceType, Region region) {
        Map<String, Set<String>> availabilityZonesForVmTypes = gcpPlatformResources.getAvailabilityZonesForVmTypes(cloudCredential, region);
        Set<String> availabilityZonesForInstanceType = availabilityZonesForVmTypes.getOrDefault(instanceType, new HashSet<>());
        LOGGER.debug("Availability Zones for Instance Type {} are {} ", instanceType,
                availabilityZonesForInstanceType);
        return emptyIfNull(environmentZones).stream().filter(availabilityZonesForInstanceType::contains).collect(Collectors.toSet());
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }

    @Override
    public String[] variants() {
        return GcpConstants.VARIANTS;
    }
}
