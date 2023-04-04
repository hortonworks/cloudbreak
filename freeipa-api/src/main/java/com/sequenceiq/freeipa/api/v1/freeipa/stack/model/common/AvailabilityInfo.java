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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AvailabilityInfo that = (AvailabilityInfo) o;

        if (getActualNodeCount() != that.getActualNodeCount()) {
            return false;
        }
        return getAvailabilityType() == that.getAvailabilityType();
    }

    @Override
    public int hashCode() {
        int result = getAvailabilityType().hashCode();
        result = 31 * result + getActualNodeCount();
        return result;
    }

}