package com.sequenceiq.cloudbreak.service.multiaz;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GroupPlacement {
    private final String name;

    private final String availabilityZone;

    private final Set<String> components;

    private final Map<String, Integer> subnetUsage;

    public GroupPlacement(String name, String availabilityZone) {
        this.name = name;
        this.availabilityZone = availabilityZone;
        this.components = new HashSet<>();
        this.subnetUsage = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public boolean containsComponent(String componentName) {
        return components.contains(componentName);
    }

    public void addComponent(String componentName) {
        components.add(componentName);
    }

    public void addSubnet(String subnet) {
        subnetUsage.put(subnet, 0);
    }

    public void increaseSubnetUsage(String subnet) {
        subnetUsage.put(subnet, subnetUsage.getOrDefault(subnet, 0) + 1);
    }

    public Map<String, Integer> getSubnetUsage() {
        return subnetUsage;
    }
}