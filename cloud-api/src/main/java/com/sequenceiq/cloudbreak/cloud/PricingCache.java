package com.sequenceiq.cloudbreak.cloud;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public interface PricingCache {

    Optional<Double> getPriceForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential);

    Optional<Integer> getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential);

    Optional<Integer> getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential);

    Optional<Double> getStoragePricePerGBHour(String region, String storageType, int volumeSize);

    CloudPlatform getCloudPlatform();
}
