package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import javax.validation.constraints.NotNull

import com.sequenceiq.cloudbreak.doc.ModelDescriptions

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
open class OrchestratorBase : JsonEntity {
    @ApiModelProperty(value = ModelDescriptions.OrchestratorModelDescription.PARAMETERS, required = true)
    var parameters: Map<String, Any> = HashMap()
    @ApiModelProperty(value = ModelDescriptions.OrchestratorModelDescription.ENDPOINT, required = true)
    var apiEndpoint: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.OrchestratorModelDescription.TYPE, required = true)
    var type: String? = null
}
