package com.sequenceiq.common.api.adjustment;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.AdjustmentType;

public class AdjustmentTypeWithThreshold {

    private AdjustmentType adjustmentType;

    private Long threshold;

    @JsonCreator
    public AdjustmentTypeWithThreshold(
            @JsonProperty("adjustmentType") AdjustmentType adjustmentType,
            @JsonProperty("threshold") Long threshold) {

        this.adjustmentType = adjustmentType;
        this.threshold = threshold;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Long getThreshold() {
        return threshold;
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AdjustmentTypeWithThreshold.class.getSimpleName() + "[", "]")
                .add("adjustmentType=" + adjustmentType)
                .add("threshold=" + threshold)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            AdjustmentTypeWithThreshold that = (AdjustmentTypeWithThreshold) o;
            return adjustmentType == that.adjustmentType && Objects.equals(threshold, that.threshold);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(adjustmentType, threshold);
    }
}
