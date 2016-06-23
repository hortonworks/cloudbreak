package com.sequenceiq.cloudbreak.api.model

import java.util.HashSet

import javax.validation.constraints.Digits
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("InstanceGroup")
@JsonIgnoreProperties(ignoreUnknown = true)
class InstanceGroupJson : JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    var id: Long? = null
    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.TEMPLATE_ID, required = true)
    var templateId: Long? = null
    @Min(value = 1, message = "The node count has to be greater than 0")
    @Max(value = 100000, message = "The node count has to be less than 100000")
    @Digits(fraction = 0, integer = 10, message = "The node count has to be a number")
    @ApiModelProperty(value = InstanceGroupModelDescription.NODE_COUNT, required = true)
    var nodeCount: Int = 0
    @NotNull
    @ApiModelProperty(value = InstanceGroupModelDescription.INSTANCE_GROUP_NAME, required = true)
    var group: String? = null
    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_TYPE)
    var type = InstanceGroupType.CORE
    var metadata: Set<InstanceMetaDataJson> = HashSet()
}
