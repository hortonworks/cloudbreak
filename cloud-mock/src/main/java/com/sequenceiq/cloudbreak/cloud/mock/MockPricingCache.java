package com.sequenceiq.cloudbreak.cloud.mock;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service("mockPricingCache")
public class MockPricingCache implements PricingCache {
    @Override
    public Optional<Double> getPriceForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        return Optional.of(1.0);
    }

    @Override
    public Optional<Integer> getCpuCountForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        return Optional.of(64);
    }

    @Override
    public Optional<Integer> getMemoryForInstanceType(String region, String instanceType, ExtendedCloudCredential extendedCloudCredential) {
        return Optional.of(256);
    }

    @Override
    public Optional<Double> getStoragePricePerGBHour(String region, String storageType, int volumeSize) {
        return Optional.of(0.1);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }
}
