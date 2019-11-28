package com.sequenceiq.cloudbreak.common.mappable;

public enum CloudPlatform {
    AWS, GCP, AZURE, OPENSTACK, YARN, MOCK;

    public boolean equalsIgnoreCase(String platfrom) {
        return name().equalsIgnoreCase(platfrom);
    }
}
