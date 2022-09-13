package com.sequenceiq.cloudbreak.cost.model;

public class PricingCacheKey {

    private final String region;

    private final String instanceType;

    public PricingCacheKey(String region, String instanceType) {
        this.region = region;
        this.instanceType = instanceType;
    }

    public String getRegion() {
        return region;
    }

    public String getInstanceType() {
        return instanceType;
    }
}
