package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreatedCloudNetwork {

    private String networkId;

    private Set<CloudSubnet> subnets = new HashSet<>();

    private Map<String, Object> properties = new HashMap<>();

    public CreatedCloudNetwork() {
    }

    public CreatedCloudNetwork(String networkId, Set<CloudSubnet> subnets, Map<String, Object> properties) {
        this.networkId = networkId;
        this.subnets = subnets;
        this.properties = properties;
    }

    public CreatedCloudNetwork(String networkId, Set<CloudSubnet> subnets) {
        this.networkId = networkId;
        this.subnets = subnets;
    }

    public String getNetworkId() {
        return networkId;
    }

    public Set<CloudSubnet> getSubnets() {
        return subnets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
