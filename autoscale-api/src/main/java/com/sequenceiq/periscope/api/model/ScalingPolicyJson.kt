package com.sequenceiq.periscope.api.model

import javax.validation.constraints.Pattern

import com.sequenceiq.periscope.doc.ApiDescription.ScalingPolicyJsonProperties

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ScalingPolicyJson")
class ScalingPolicyJson : Json {

    @ApiModelProperty(ScalingPolicyJsonProperties.ID)
    var id: Long? = null
    @ApiModelProperty(ScalingPolicyJsonProperties.NAME)
    @Pattern(regexp = "([a-zA-Z][-a-zA-Z0-9]*)", message = "The name can only contain alphanumeric characters and hyphens and has start with an alphanumeric character")
    var name: String? = null
    @ApiModelProperty(ScalingPolicyJsonProperties.ADJUSTMENTTYPE)
    var adjustmentType: AdjustmentType? = null
    @ApiModelProperty(ScalingPolicyJsonProperties.SCALINGADJUSTMENT)
    var scalingAdjustment: Int = 0
    @ApiModelProperty(ScalingPolicyJsonProperties.ALERTID)
    var alertId: Long = 0
    @ApiModelProperty(ScalingPolicyJsonProperties.HOSTGROUP)
    var hostGroup: String? = null
}
