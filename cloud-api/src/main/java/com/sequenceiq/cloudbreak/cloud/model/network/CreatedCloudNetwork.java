package com.sequenceiq.cloudbreak.cloud.model.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreatedCloudNetwork {

    private String networkId;

    private Set<CreatedSubnet> subnets = new HashSet<>();

    private Map<String, Object> properties = new HashMap<>();

    public CreatedCloudNetwork() {
    }

    public CreatedCloudNetwork(String networkId, Set<CreatedSubnet> subnets, Map<String, Object> properties) {
        this.networkId = networkId;
        this.subnets = subnets;
        this.properties = properties;
    }

    public CreatedCloudNetwork(String networkId, Set<CreatedSubnet> subnets) {
        this.networkId = networkId;
        this.subnets = subnets;
    }

    public String getNetworkId() {
        return networkId;
    }

    public Set<CreatedSubnet> getSubnets() {
        return subnets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
