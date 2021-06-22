package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

public class GroupSubnet {

    private final String subnetId;

    public GroupSubnet(String subnetId) {
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
