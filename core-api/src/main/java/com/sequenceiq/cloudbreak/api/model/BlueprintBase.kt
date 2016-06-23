package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
abstract class BlueprintBase : JsonEntity {

    @Size(max = 100, min = 1, message = "The length of the blueprint's name has to be in range of 1 to 100")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null

    @ApiModelProperty(value = BlueprintModelDescription.AMBARI_BLUEPRINT, required = true)
    var ambariBlueprint: String? = null
        private set

    @Size(max = 1000)
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    var description: String? = null

    fun setAmbariBlueprint(ambariBlueprint: JsonNode) {
        this.ambariBlueprint = ambariBlueprint.toString()
    }
}
