package com.sequenceiq.cloudbreak.common.mappable;

import java.util.Set;

public enum CloudPlatform {

    AWS,
    GCP,
    AZURE,
    YARN,
    MOCK,
    // DEPRECATED Platforms
    OPENSTACK;

    public boolean equalsIgnoreCase(String platfrom) {
        return name().equalsIgnoreCase(platfrom);
    }

    public static Set<CloudPlatform> deprecated() {
        return Set.of(OPENSTACK);
    }
}
