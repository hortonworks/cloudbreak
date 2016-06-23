package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModelProperty

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class ConstraintTemplateBase : JsonEntity {

    @Size(max = 100, min = 5, message = "The length of the constraint template's name has to be in range of 5 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])", message = "The name of the constraint template can only contain lowercase characters and hyphens")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    var name: String? = null

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    var description: String? = null

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.CPU, readOnly = true)
    var cpu: Double? = null

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.MEMORY, readOnly = true)
    var memory: Double? = null

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.DISK, readOnly = true)
    var disk: Double? = null

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ConstraintTemplateModelDescription.ORCHESTRATOR_TYPE, readOnly = true)
    var orchestratorType: String? = null
}
