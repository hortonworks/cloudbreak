package com.sequenceiq.cloudbreak.common.mappable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum CloudPlatform {

    AWS,
    GCP,
    AZURE,
    YARN,
    MOCK,
    // DEPRECATED Platforms
    OPENSTACK;

    private static final Map<CloudPlatform, String> DISPLAY_NAME = new HashMap<>() {
        {
            put(AWS, "Amazon Web Services");
            put(AZURE, "Microsoft Azure");
        }
    };

    public boolean equalsIgnoreCase(String platfrom) {
        return name().equalsIgnoreCase(platfrom);
    }

    public static Set<CloudPlatform> deprecated() {
        return Set.of(OPENSTACK);
    }

    public String getDislayName() {
        return DISPLAY_NAME.getOrDefault(this, name());
    }

    public static boolean azureOrAws(String cloudPlatform) {
        return AWS.equalsIgnoreCase(cloudPlatform) || AZURE.equalsIgnoreCase(cloudPlatform);
    }
}
