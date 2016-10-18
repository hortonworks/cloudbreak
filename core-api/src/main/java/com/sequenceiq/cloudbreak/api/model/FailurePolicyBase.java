package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FailurePolicyModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class FailurePolicyBase implements JsonEntity {

    @ApiModelProperty(FailurePolicyModelDescription.THRESHOLD)
    private Long threshold;
    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.ADJUSTMENT_TYPE, required = true)
    private AdjustmentType adjustmentType;

    public Long getThreshold() {
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

}
