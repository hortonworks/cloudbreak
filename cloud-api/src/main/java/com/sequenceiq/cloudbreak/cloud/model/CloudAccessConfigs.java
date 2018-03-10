package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Set;

public class CloudAccessConfigs {

    private final Set<CloudAccessConfig> cloudAccessConfigs;

    public CloudAccessConfigs(Set<CloudAccessConfig> cloudAccessConfigs) {
        this.cloudAccessConfigs = cloudAccessConfigs;
    }

    public Collection<CloudAccessConfig> getCloudAccessConfigs() {
        return cloudAccessConfigs;
    }

    @Override
    public String toString() {
        return "CloudAccessConfigs{"
                + "cloudAccessConfigs=" + cloudAccessConfigs
                + '}';
    }
}
