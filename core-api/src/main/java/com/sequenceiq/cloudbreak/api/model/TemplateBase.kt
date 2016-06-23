package com.sequenceiq.cloudbreak.api.model

import java.util.HashMap

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class TemplateBase : JsonEntity {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    var cloudPlatform: String? = null
    @Size(max = 100, min = 5, message = "The length of the template's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The name of the template can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.TemplateModelDescription.PARAMETERS, required = true)
    var parameters: Map<String, Any> = HashMap()
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.TemplateModelDescription.VOLUME_COUNT, required = true)
    var volumeCount: Int? = null
    @ApiModelProperty(ModelDescriptions.TemplateModelDescription.VOLUME_SIZE)
    var volumeSize: Int? = null
    @ApiModelProperty(ModelDescriptions.TemplateModelDescription.VOLUME_TYPE)
    var volumeType: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.TemplateModelDescription.INSTANCE_TYPE, required = true)
    var instanceType: String? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, readOnly = true)
    var isPublicInAccount: Boolean = false

    @ApiModelProperty(value = ModelDescriptions.TOPOLOGY_ID)
    var topologyId: Long? = null
}
