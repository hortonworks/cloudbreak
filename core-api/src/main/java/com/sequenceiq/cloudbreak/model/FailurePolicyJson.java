package com.sequenceiq.cloudbreak.model;

import com.sequenceiq.cloudbreak.common.type.AdjustmentType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FailurePolicyModelDescription;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FailurePolicy")
public class FailurePolicyJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(FailurePolicyModelDescription.THRESHOLD)
    private Long threshold;
    @ApiModelProperty(required = true)
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
