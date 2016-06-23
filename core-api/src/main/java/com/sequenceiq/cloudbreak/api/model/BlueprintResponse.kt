package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
class BlueprintResponse : BlueprintBase() {
    @ApiModelProperty(value = ModelDescriptions.ID)
    var id: String? = null
    @ApiModelProperty(value = BlueprintModelDescription.BLUEPRINT_NAME)
    var blueprintName: String? = null
    @ApiModelProperty(value = BlueprintModelDescription.HOST_GROUP_COUNT)
    var hostGroupCount: Int? = null
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT)
    var isPublicInAccount: Boolean = false
}
