package com.sequenceiq.cloudbreak.api.model


import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Constraint")
@JsonIgnoreProperties(ignoreUnknown = true)
class ConstraintJson {

    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.INSTANCE_GROUP, required = true)
    var instanceGroupName: String? = null

    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.CONSTRAINT_NAME, required = true)
    var constraintTemplateName: String? = null

    @NotNull
    var hostCount: Int? = null
}
