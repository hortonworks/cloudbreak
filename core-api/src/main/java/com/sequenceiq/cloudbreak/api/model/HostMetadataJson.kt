package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("HostMetadata")
@JsonIgnoreProperties(ignoreUnknown = true)
class HostMetadataJson {

    @ApiModelProperty(value = ModelDescriptions.ID)
    var id: Long? = null

    @ApiModelProperty(value = ModelDescriptions.NAME)
    var name: String? = null

    @ApiModelProperty(value = ModelDescriptions.HostGroupModelDescription.HOST_GROUP_NAME)
    var groupName: String? = null

    @ApiModelProperty(value = ModelDescriptions.HostMetadataModelDescription.STATE)
    var state: String? = null
}
