package com.sequenceiq.cloudbreak.api.model

import com.sequenceiq.cloudbreak.doc.ModelDescriptions

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

import javax.validation.constraints.NotNull

@ApiModel
class ConstraintTemplateResponse : ConstraintTemplateBase() {
    @ApiModelProperty(ModelDescriptions.ID)
    var id: Long? = null

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, readOnly = true)
    var isPublicInAccount: Boolean = false
}
