package com.sequenceiq.cloudbreak.api.model

import javax.validation.Valid

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("UpdateCluster")
@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateClusterJson : JsonEntity {

    @ApiModelProperty(required = true)
    var hostGroupAdjustment: HostGroupAdjustmentJson? = null
    @ApiModelProperty(required = true)
    var status: StatusRequest? = null
    @ApiModelProperty(required = true)
    var userNamePasswordJson: UserNamePasswordJson? = null
    @ApiModelProperty(StackModelDescription.BLUEPRINT_ID)
    var blueprintId: Long? = null
    private var validateBlueprint: Boolean? = true
    var hostgroups: Set<HostGroupJson>? = null
    @Valid
    var ambariStackDetails: AmbariStackDetailsJson? = null

    val validateBlueprint: Boolean
        get() = if (validateBlueprint == null) false else validateBlueprint

    fun setValidateBlueprint(validateBlueprint: Boolean?) {
        this.validateBlueprint = validateBlueprint
    }
}
