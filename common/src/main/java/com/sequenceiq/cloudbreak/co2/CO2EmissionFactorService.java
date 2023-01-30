package com.sequenceiq.cloudbreak.co2;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public interface CO2EmissionFactorService {

    double getAverageMinimumWatts();

    double getAverageMaximumWatts();

    double getPowerUsageEffectiveness();

    double getEmissionFactorByRegion(String region);

    CloudPlatform getCloudPlatform();
}
