package com.sequenceiq.cloudbreak.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.sequenceiq.cloudbreak.doc.ModelDescriptions
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel
class RecipeResponse : RecipeBase() {

    @ApiModelProperty(ModelDescriptions.ID)
    var id: Long? = null
    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    var isPublicInAccount: Boolean = false
}
