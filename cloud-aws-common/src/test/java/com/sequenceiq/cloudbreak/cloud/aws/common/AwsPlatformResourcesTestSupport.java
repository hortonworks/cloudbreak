package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.Region;

public class AwsPlatformResourcesTestSupport {

    private final AwsPlatformResources awsPlatformResources;

    public AwsPlatformResourcesTestSupport(AwsPlatformResources awsPlatformResources) {
        this.awsPlatformResources = awsPlatformResources;
    }

    public Set<Region> getEnabledRegions() {
        return awsPlatformResources.getEnabledRegions();
    }

}
