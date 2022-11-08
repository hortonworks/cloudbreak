package com.sequenceiq.environment.network.service.domain;

import java.util.Set;

public class ProvidedSubnetIds {
    private final String subnetId;

    private final Set<String> subnetIds;

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
