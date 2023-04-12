package com.sequenceiq.cloudbreak.service.multiaz;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GroupPlacement {
    private final String name;

    private final String availabilityZone;

    private final Set<String> instanceGroupNames;

    private final Map<String, Integer> subnetUsage;

    public GroupPlacement(String name, String availabilityZone) {
        this.name = name;
        this.availabilityZone = availabilityZone;
        this.instanceGroupNames = new HashSet<>();
        this.subnetUsage = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public boolean containsInstanceGroupName(String instanceGroupName) {
        return instanceGroupNames.contains(instanceGroupName);
    }

    public void addInstanceGroupName(String componentName) {
        instanceGroupNames.add(componentName);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupPlacement that = (GroupPlacement) o;
        return Objects.equals(name, that.name) && Objects.equals(availabilityZone, that.availabilityZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, availabilityZone);
    }

    @Override
    public String toString() {
        return "GroupPlacement{" +
                "name='" + name + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                ", instanceGroupNames=" + instanceGroupNames +
                ", subnetUsage=" + subnetUsage +
                '}';
    }
}