package com.sequenceiq.cloudbreak.cloud;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public interface AvailabilityZoneConnector extends CloudPlatformAware {

    Set<String> getAvailabilityZones(ExtendedCloudCredential cloudCredential, Set<String> environmentZones,
            String instanceType, Region region);

}
