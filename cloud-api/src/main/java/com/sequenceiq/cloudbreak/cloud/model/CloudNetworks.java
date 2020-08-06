package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudNetworks {

    private Map<String, Set<CloudNetwork>> cloudNetworkResponses = new HashMap<>();

    public CloudNetworks(Map<String, Set<CloudNetwork>> cloudNetworkResponses) {
        this.cloudNetworkResponses = cloudNetworkResponses;
    }

    public CloudNetworks() {
    }

    public Map<String, Set<CloudNetwork>> getCloudNetworkResponses() {
        return cloudNetworkResponses;
    }

    public void setCloudNetworkResponses(Map<String, Set<CloudNetwork>> cloudNetworkResponses) {
        this.cloudNetworkResponses = cloudNetworkResponses;
    }

    @Override
    public String toString() {
        return "CloudNetworks{" +
                "cloudNetworkResponses=" + cloudNetworkResponses +
                '}';
    }
}
