package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CloudNetwork {

    private String name;

    private Set<String> subnetIds = new HashSet<>();

    private Map<String, Object> properties = new HashMap<>();

    public CloudNetwork(String name, Set<String> subnetIds, Map<String, Object> properties) {
        this.name = name;
        this.subnetIds = subnetIds;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
