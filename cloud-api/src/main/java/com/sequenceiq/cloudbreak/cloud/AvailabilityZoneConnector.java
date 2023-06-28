package com.sequenceiq.cloudbreak.cloud;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public interface AvailabilityZoneConnector extends CloudPlatformAware {
    int MIN_ZONES_FOR_FREE_IPA = 2;

    int MIN_ZONES_FOR_DATALAKE = 2;

    int MIN_ZONES_FOR_DATAHUB = 1;

    Set<String> getAvailabilityZones(ExtendedCloudCredential cloudCredential, Set<String> environmentZones,
            String instanceType, Region region);

    default Integer getMinZonesForFreeIpa() {
        return MIN_ZONES_FOR_FREE_IPA;
    }

    default Integer getMinZonesForDataLake() {
        return MIN_ZONES_FOR_DATALAKE;
    }

    default Integer getMinZonesForDataHub() {
        return MIN_ZONES_FOR_DATAHUB;
    }

}
