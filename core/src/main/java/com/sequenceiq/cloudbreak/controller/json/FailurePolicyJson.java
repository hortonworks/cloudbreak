package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.FailurePolicyModelDescription;
import com.sequenceiq.cloudbreak.domain.AdjustmentType;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

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
