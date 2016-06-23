package com.sequenceiq.cloudbreak.api.model

import java.util.ArrayList

import javax.validation.Valid

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
class StackResponse : StackBase() {
    @ApiModelProperty(StackModelDescription.STACK_ID)
    var id: Long? = null
    @ApiModelProperty(ModelDescriptions.OWNER)
    var owner: String? = null
    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    var account: String? = null
    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    var isPublicInAccount: Boolean = false
    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    var status: Status? = null
    var cluster: ClusterResponse? = null
    @ApiModelProperty(StackModelDescription.STATUS_REASON)
    var statusReason: String? = null
    @Valid
    @ApiModelProperty
    override var instanceGroups: List<InstanceGroupJson> = ArrayList()
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    var orchestrator: OrchestratorResponse? = null
    @ApiModelProperty(StackModelDescription.CREATED)
    var created: Long? = null
    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    var gatewayPort: Int? = null
    @ApiModelProperty(StackModelDescription.IMAGE)
    var image: ImageJson? = null
}
