package com.sequenceiq.common.api.adjustment;

import java.util.StringJoiner;

import com.sequenceiq.common.api.type.AdjustmentType;

public class AdjustmentTypeWithThreshold {

    private AdjustmentType adjustmentType;

    private Long threshold;

    public AdjustmentTypeWithThreshold(AdjustmentType adjustmentType, Long threshold) {
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

}
