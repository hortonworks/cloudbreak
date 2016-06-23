package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
class FileSystemResponse : FileSystemBase() {

    @ApiModelProperty(ModelDescriptions.ID)
    var id: String? = null
}
