package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupSubnet {

    private final String subnetId;

    @JsonCreator
    public GroupSubnet(@JsonProperty("subnetId") String subnetId) {
        this.subnetId = subnetId;
    }

    public String getSubnetId() {
        return subnetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupSubnet)) {
            return false;
        }
        GroupSubnet subnet = (GroupSubnet) o;
        return Objects.equals(subnetId, subnet.subnetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subnetId);
    }

    @Override
    public String toString() {
        return "GroupSubnet{" +
                "subnetId='" + subnetId + '\'' +
                '}';
    }
}
