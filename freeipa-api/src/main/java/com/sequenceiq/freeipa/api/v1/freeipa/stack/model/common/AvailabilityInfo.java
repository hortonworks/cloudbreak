package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

public class AvailabilityInfo {

    private final AvailabilityType availabilityType;

    private final int actualNodeCount;

    public AvailabilityInfo(int nodeCount) {
        this.availabilityType = AvailabilityType.getByInstanceCount(nodeCount);
        this.actualNodeCount = nodeCount;
    }

    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public int getActualNodeCount() {
        return actualNodeCount;
    }

    @Override
    public String toString() {
        return "AvailabilityInfo{" +
                "availabilityType=" + availabilityType +
                ", actualNodeCount=" + actualNodeCount +
                '}';
    }

}