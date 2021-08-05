package com.sequenceiq.environment.network.service.domain;

import java.util.HashSet;
import java.util.Set;

public class ProvidedSubnetIds {
    private String subnetId;

    private Set<String> subnetIds = new HashSet<>();

    public ProvidedSubnetIds(String subnetId, Set<String> subnetIds) {
        this.subnetId = subnetId;
        this.subnetIds = subnetIds;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }
}
