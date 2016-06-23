package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FailurePolicyModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("FailurePolicy")
class FailurePolicyJson : JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    var id: Long? = null
    @ApiModelProperty(FailurePolicyModelDescription.THRESHOLD)
    var threshold: Long? = null
        private set
    @ApiModelProperty(required = true)
    var adjustmentType: AdjustmentType? = null

    fun setThreshold(threshold: Long) {
        this.threshold = threshold
    }
}
