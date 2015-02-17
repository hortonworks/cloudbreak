package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.AdjustmentType;

public class FailurePolicyJson implements JsonEntity {

    private Long id;
    private long threshold;
    private AdjustmentType adjustmentType;

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
