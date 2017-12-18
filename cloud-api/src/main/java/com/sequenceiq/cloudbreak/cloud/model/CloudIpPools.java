package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudIpPools {

    private Map<String, Set<CloudIpPool>> cloudIpPools = new HashMap<>();

    public CloudIpPools() {
    }

    public CloudIpPools(Map<String, Set<CloudIpPool>> cloudIpPools) {
        this.cloudIpPools = cloudIpPools;
    }

    public Map<String, Set<CloudIpPool>> getCloudIpPools() {
        return cloudIpPools;
    }

    @Override
    public String toString() {
        return "CloudIpPools{"
                + "cloudIpPools=" + cloudIpPools
                + '}';
    }
}
