package com.sequenceiq.cloudbreak.api.model

import java.util.HashSet

import javax.validation.constraints.NotNull

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
class StackValidationRequest : JsonEntity {
    @ApiModelProperty(required = true)
    var hostGroups: Set<HostGroupJson> = HashSet()
    @ApiModelProperty(required = true)
    var instanceGroups: Set<InstanceGroupJson> = HashSet()
    @NotNull
    @ApiModelProperty(value = StackModelDescription.BLUEPRINT_ID, required = true)
    var blueprintId: Long? = null
    @NotNull
    @ApiModelProperty(value = StackModelDescription.NETWORK_ID, required = true)
    var networkId: Long? = null
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    var platform: String? = null
    var fileSystem: FileSystemRequest? = null
}
