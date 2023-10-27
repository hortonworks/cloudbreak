package com.sequenceiq.cloudbreak.core.flow2.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkScaleDetails {

    private final List<String> preferredSubnetIds;

    private final Set<String> preferredAvailabilityZones;

    public NetworkScaleDetails() {
        this.preferredSubnetIds = new ArrayList<>();
        this.preferredAvailabilityZones = new HashSet<>();
    }

    public NetworkScaleDetails(List<String> preferredSubnetIds, Set<String> preferredAvailabilityZones) {
        this.preferredSubnetIds = preferredSubnetIds == null ? new ArrayList<>() : preferredSubnetIds;
        this.preferredAvailabilityZones = preferredAvailabilityZones == null ? new HashSet<>() : preferredAvailabilityZones;
    }

    public List<String> getPreferredSubnetIds() {
        return new ArrayList<>(preferredSubnetIds);
    }

    public Set<String> getPreferredAvailabilityZones() {
        return preferredAvailabilityZones;
    }

    public static NetworkScaleDetails getEmpty() {
        return new NetworkScaleDetails();
    }
}
