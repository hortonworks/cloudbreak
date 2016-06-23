package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.validation.ValidUpdateStackRequest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("UpdateStack")
@ValidUpdateStackRequest
@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateStackJson : JsonEntity {

    @ApiModelProperty(required = true)
    var status: StatusRequest? = null

    var instanceGroupAdjustment: InstanceGroupAdjustmentJson? = null
}