package com.sequenceiq.cloudbreak.api.model

import javax.validation.Valid

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
class StackRequest : StackBase() {
    @Valid
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    var orchestrator: OrchestratorRequest? = null
    @ApiModelProperty(value = StackModelDescription.AMBARI_VERSION)
    var ambariVersion: String? = null
    @ApiModelProperty(value = StackModelDescription.HDP_VERSION)
    var hdpVersion: String? = null
}
