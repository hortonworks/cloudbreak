package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupAdjustmentModelDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("HostGroupAdjustment")
class HostGroupAdjustmentJson {

    @ApiModelProperty(value = HostGroupModelDescription.HOST_GROUP_NAME, required = true)
    var hostGroup: String? = null
    @ApiModelProperty(value = HostGroupAdjustmentModelDescription.SCALING_ADJUSTMENT, required = true)
    var scalingAdjustment: Int? = null
    @ApiModelProperty(HostGroupAdjustmentModelDescription.WITH_STACK_UPDATE)
    var withStackUpdate: Boolean? = false

}
