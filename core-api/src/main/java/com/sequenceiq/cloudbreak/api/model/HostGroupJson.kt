package com.sequenceiq.cloudbreak.api.model

import java.util.HashSet

import javax.validation.Valid
import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("HostGroup")
@JsonIgnoreProperties(ignoreUnknown = true)
class HostGroupJson {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null

    @Valid
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.CONSTRAINT, required = true)
    var constraint: ConstraintJson? = null

    @ApiModelProperty(value = HostGroupModelDescription.RECIPE_IDS)
    var recipeIds: Set<Long>? = null

    var metadata: Set<HostMetadataJson> = HashSet()
}
