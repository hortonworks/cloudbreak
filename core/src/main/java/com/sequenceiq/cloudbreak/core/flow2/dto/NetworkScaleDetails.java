package com.sequenceiq.cloudbreak.core.flow2.dto;

import java.util.ArrayList;
import java.util.List;

public class NetworkScaleDetails {

    private final List<String> preferredSubnetIds;

    public NetworkScaleDetails() {
        this.preferredSubnetIds = new ArrayList<>();
    }

    public NetworkScaleDetails(List<String> preferredSubnetIds) {
        this.preferredSubnetIds = preferredSubnetIds == null ? new ArrayList<>() : preferredSubnetIds;
    }

    public List<String> getPreferredSubnetIds() {
        return new ArrayList<>(preferredSubnetIds);
    }

    public static NetworkScaleDetails getEmpty() {
        return new NetworkScaleDetails();
    }
}
