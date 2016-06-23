package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupAdjustmentModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("InstanceGroupAdjustment")
class InstanceGroupAdjustmentJson {

    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    var instanceGroup: String? = null
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    var scalingAdjustment: Int? = null
    @ApiModelProperty(value = InstanceGroupAdjustmentModelDescription.WITH_CLUSTER_EVENT)
    var withClusterEvent: Boolean? = false
}
