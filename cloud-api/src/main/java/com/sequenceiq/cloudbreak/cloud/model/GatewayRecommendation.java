package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;
import java.util.Set;

public class GatewayRecommendation {

    private final Set<String> hostGroups;

    public GatewayRecommendation(Set<String> hostGroups) {
        this.hostGroups = Set.copyOf(hostGroups);
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        GatewayRecommendation other = (GatewayRecommendation) obj;

        return Objects.equals(hostGroups, other.hostGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostGroups);
    }

    @Override
    public String toString() {
        return "GatewayRecommendation" + hostGroups;
    }
}
