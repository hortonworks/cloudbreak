package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public interface PricingCache {

    double getPriceForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential);

    int getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential);

    int getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential);

    double getStoragePricePerGBHour(String region, String storageType, int volumeSize);

    CloudPlatform getCloudPlatform();
}
