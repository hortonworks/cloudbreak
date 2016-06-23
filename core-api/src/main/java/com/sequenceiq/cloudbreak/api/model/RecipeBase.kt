package com.sequenceiq.cloudbreak.api.model

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RecipeModelDescription
import com.sequenceiq.cloudbreak.validation.ValidPlugin

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
internal abstract class RecipeBase : JsonEntity {
    @Size(max = 100, min = 1, message = "The length of the recipe's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null

    @JsonPropertyDescription("Recipe timeout in minutes.")
    @ApiModelProperty(RecipeModelDescription.TIMEOUT)
    var timeout: Int? = null

    @ValidPlugin
    @ApiModelProperty(value = RecipeModelDescription.PLUGINS, required = true)
    var plugins: Map<String, ExecutionType>? = null

    @JsonProperty("properties")
    @ApiModelProperty(value = RecipeModelDescription.PROPERTIES)
    var properties: Map<String, String>? = null
}
