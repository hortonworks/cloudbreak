package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.AdjustmentType;

public class FailurePolicyJson implements JsonEntity {

    private Long id;
    private Long threshold;
    private AdjustmentType adjustmentType;

    public Long getThreshold() {
        return threshold;
    }

    public void setThreshold(Long threshold) {
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
