package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashSet;
import java.util.Set;

public class CloudAccessConfigs {

    private Set<CloudAccessConfig> cloudAccessConfigs = new HashSet<>();

    public CloudAccessConfigs(Set<CloudAccessConfig> cloudAccessConfigs) {
        this.cloudAccessConfigs = cloudAccessConfigs;
    }

    public Set<CloudAccessConfig> getCloudAccessConfigs() {
        return cloudAccessConfigs;
    }

    @Override
    public String toString() {
        return "CloudAccessConfigs{"
                + "cloudAccessConfigs=" + cloudAccessConfigs
                + '}';
    }
}
