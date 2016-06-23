package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
class TemplateResponse : TemplateBase() {
    @ApiModelProperty(ModelDescriptions.ID)
    var id: Long? = null
}
