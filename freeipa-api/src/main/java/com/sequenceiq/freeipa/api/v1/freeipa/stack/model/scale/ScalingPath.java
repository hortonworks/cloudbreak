package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import java.util.Objects;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;

public class ScalingPath {

    private final AvailabilityType originalAvailabilityType;

    private final AvailabilityType targetAvailabilityType;

    public ScalingPath(AvailabilityType originalAvailabilityType, AvailabilityType targetAvailabilityType) {
        this.originalAvailabilityType = originalAvailabilityType;
        this.targetAvailabilityType = targetAvailabilityType;
    }

    public AvailabilityType getOriginalAvailabilityType() {
        return originalAvailabilityType;
    }

    public AvailabilityType getTargetAvailabilityType() {
        return targetAvailabilityType;
    }

    @Override
    public String toString() {
        return "ScalingPath{" +
                "originalAvailabilityType=" + originalAvailabilityType +
                ", targetAvailabilityType=" + targetAvailabilityType +
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
        ScalingPath that = (ScalingPath) o;
        return originalAvailabilityType == that.originalAvailabilityType && targetAvailabilityType == that.targetAvailabilityType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalAvailabilityType, targetAvailabilityType);
    }
}
